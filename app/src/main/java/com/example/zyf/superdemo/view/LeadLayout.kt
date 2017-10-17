package com.example.zyf.superdemo.view

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import com.example.zyf.superdemo.R

/**
 * 操作引导View
 *
 *
 */
@Suppress("CAST_NEVER_SUCCEEDS")
open class LeadLayout : ViewGroup, ViewTreeObserver.OnGlobalLayoutListener, View.OnClickListener {

    companion object {

        var mBackgroundColor = Color.argb(80, 0, 0, 0)

        /**
         * 第一象限
         */
        val FIRST_QUADRANT = 0x000000FF


        /**
         * 第二象限
         */
        val SECOND_QUADRANT = 0x0000FF00

        /**
         * 第三象限
         */
        val THIRD_QUADRANT = 0x00FF0000

        /**
         * 第四象限
         */
        val FOURTH_QUADRANT = 0x0FF00000

        val CENTER = 0x0FFFFFFF

        /**
         * 椭圆
         */
        val OVAL = 0x00000F00

        /**
         * 圆
         */
        val CIRCLE = 0x000000F0

        /**
         * 正方形
         */
        val SQUARE = 0x0000000F

        /**
         * 传入x,y获取象限
         */
        fun getQuanrant(x: Int, y: Int): Int {
            if (x > 0 && y > 0) {
                return FIRST_QUADRANT
            } else if (x < 0 && y > 0) {
                return SECOND_QUADRANT
            } else if (x > 0 && y < 0) {
                return THIRD_QUADRANT
            } else if (x < 0 && y < 0) {
                return FOURTH_QUADRANT
            }

            return CENTER
        }
    }

    /**
     * 屏幕宽度
     */
    private val SCREEN_WIDTH: Int

    /**
     * 屏幕高度
     */
    private val SCREEN_HEIGHT: Int

    /**
     * 像素密度
     */
    private val DENSITY: Float

    /**
     * 需要被提示view的屏幕所在位置
     */
    private val mHintViewLocation = IntArray(2)

    /**
     * 引导view的所在屏幕位置
     */
    private val mLeadViewLocation = IntArray(2)

    /**
     * 是否在当前引导view范围内
     */
    private var mIsInLeadView: Boolean = false

    /**
     * 是否完成加载布局
     */
    private var mIsFinishLayout: Boolean = false

    /**
     * 需要被提示的view
     */
    var mHintView: View? = null
        set(value) {
            field = value
            if (mIsFinishLayout) {
                if (initInfo()) {
                    requestLayout()
                }
            }
        }

    /**
     * 需要被提示view的id，用于自定义属性
     */
    private var mHintViewID: Int = -1

    /**
     * 提示文本
     */
    var mHintText: View? = null

    /**
     * 提示箭头
     */
    var mArrowImage: View? = null

    /**
     * 背景画笔
     */
    private val mBackgroundPaint = Paint()

    /**
     * 设置绘制的形状
     */
    var mShape = OVAL
        set(value) {
            field = value
            if (mIsFinishLayout) {
                invalidate()
            }
        }

    /**
     * 视图布尔运算对象
     */
    private var mXcode_Clear: PorterDuffXfermode? = null

    /**
     * 提示框倍率 比hintView 大多少倍
     */
    var mHintMultiplyPower: Float = 1f
        set(value) {
            if (0 < value)
                field = value

            if (mIsFinishLayout && mHintView != null) {
                if (mHintMultiplyPower >= 1) {
                    //放大或者不变
                    mHintMoreWidth = mHintView?.width!! * (mHintMultiplyPower - 1) / 2
                    mHintMoreHeight = mHintView?.height!! * (mHintMultiplyPower - 1) / 2
                } else {
                    //缩小
                    mHintMoreWidth = -mHintView?.width!! * (1 - mHintMultiplyPower) / 2f
                    mHintMoreHeight = -mHintView?.height!! * (1 - mHintMultiplyPower) / 2
                }
            }

        }

    //设置虚线
    private var mDashPath = DashPathEffect(floatArrayOf(15f, 15f), 0f)
    //是否设置虚线
    var mIsDashPath = true

    /**
     * 虚线边框偏移量
     */
    private var mDashLineOffset: Float
        set(value) {
            field = value * DENSITY
        }

    /**
     * 是否使用  LeadLayoutParams 中的象限进行自动布局
     */
    var mIsSelfMotionLayout = true

    /**
     * 通过倍数 mHintMultiplyPower 获取 比原mHintView多或少出来的宽、高值
     */
    var mHintMoreWidth = 0f
    var mHintMoreHeight = 0f

    /**
     * 当前所在象限
     */
    private var mQuadrant = FIRST_QUADRANT

    /**
     * 象限原点 ，进行计算的时候可将 x,y 当成0,0
     */
    private var mZeroX: Int = 0
    private var mZeroY: Int = 0

    /**
     * 初始化数据
     */
    init {
        val wm = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val metric = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metric)
        SCREEN_WIDTH = metric.widthPixels     // 屏幕宽度（像素）
        SCREEN_HEIGHT = metric.heightPixels   // 屏幕高度（像素）
        DENSITY = metric.density //像素密度
        mDashLineOffset = 3f //虚线边框偏移量
    }

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        //设置view加载完成监听
        viewTreeObserver.addOnGlobalLayoutListener(this)
        //关闭硬件加速
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        //使其viewGroup会调用onDraw方法
        setWillNotDraw(false)

        //设置可点击，不让事件穿透
        if (!isClickable) {
            isClickable = true
        }

        val typeArray = context?.obtainStyledAttributes(attrs, R.styleable.LeadLayout)!!

        mIsSelfMotionLayout = typeArray.getBoolean(R.styleable.LeadLayout_selfMotionLayout, true)

        mHintViewID = typeArray.getResourceId(R.styleable.LeadLayout_hintViewId, -1)

        mHintMultiplyPower = typeArray.getFloat(R.styleable.LeadLayout_multiply, 1.0f)

        mShape = typeArray.getInt(R.styleable.LeadLayout_shape, OVAL)

        mIsDashPath = typeArray.getBoolean(R.styleable.LeadLayout_isDashLine,true)
        typeArray.recycle()
    }

    /**
     * 初始化数据
     */
    private fun initInfo(): Boolean {
        if (mHintView == null) return false

        //获取提示view所在屏幕的所在废纸
        mHintView?.getLocationOnScreen(mHintViewLocation)


        //判断leadView 是否包含hintView ，包含指的是屏幕
        mIsInLeadView = isInMyScreen(mHintView)

        //如果不包含 将hintView设置成空 结束方法体
        if (!mIsInLeadView) {
            mHintView = null
            return false
        }

        //通过倍数 mHintMultiplyPower 获取 比原mHintView多出来的宽、高值
        if (mHintMultiplyPower >= 1) {
            //放大或者不变
            mHintMoreWidth = mHintView?.width!! * (mHintMultiplyPower - 1) / 2
            mHintMoreHeight = mHintView?.height!! * (mHintMultiplyPower - 1) / 2
        } else {
            //缩小
            mHintMoreWidth = -mHintView?.width!! * (1 - mHintMultiplyPower) / 2f
            mHintMoreHeight = -mHintView?.height!! * (1 - mHintMultiplyPower) / 2
        }

        val x = mZeroX - mHintViewLocation[0]
        val y = mZeroY - mHintViewLocation[1]

        mQuadrant = getQuanrant(x, y)

        return true
    }

    /**
     * 初始化有关画图的对象
     */
    private fun initDraw() {

        //设置抗锯齿
        mBackgroundPaint.isAntiAlias = true
        mXcode_Clear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private fun findHintView(mHintViewID: Int): View? {
        if (context is Activity) {
            val c = context as Activity
            val tree = c.findViewById(android.R.id.content)

            if (tree is FrameLayout) {
                return findViewByTree(tree.getChildAt(0), mHintViewID)
            }
        }

        return null
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    private fun findViewByTree(view: View, mHintViewID: Int): View? {
        var v = view.findViewById(mHintViewID)

        println(view.toString())
        if (v == null && view is ViewGroup) {
            view.eachChild {
                if (it is ViewGroup) {
                    v = findViewByTree(it, mHintViewID)
                    if (v != null) return@eachChild
                }
            }
        }

        return v
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)


        eachChild {
            if (it.visibility == View.GONE) {
                return@eachChild
            }
            //设置view的大小
            val param = it.layoutParams as LeadLayoutParams

            //设置view宽度
            val childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    paddingLeft + paddingRight + param.leftMargin + param.rightMargin, param.width)
            //设置view高度
            val childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    paddingTop + paddingBottom + param.topMargin + param.bottomMargin, param.height)

            when (param.mViewType) {
            //设置箭头对象
                LeadLayoutParams.ARROW_IMG -> {
                    mArrowImage = it
                }

            //设置提示文本的位置
                LeadLayoutParams.HINT_TEXT -> {
                    mHintText = it
                }

            }


            it.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        eachChild {
            if (it.visibility == View.GONE) {
                return@eachChild
            }

            val param = it.layoutParams as LeadLayoutParams

            //判断是否需要自动布局 mIsSelfMotionLayout 和 当前是否设置 mGravity ，如果设置 mGravity 表示不需要自动化布局
            param.mViewType = if (mIsSelfMotionLayout) param.mViewType else LeadLayoutParams.NONE

            when (param.mViewType) {
            //设置箭头位置
                LeadLayoutParams.ARROW_IMG -> {
                    layoutArrowImg(param, it)
                }

            //设置提示文本的位置
                LeadLayoutParams.HINT_TEXT -> {
                    layoutHintText(param, it)
                }

            //设置默认类型位置
                LeadLayoutParams.NONE -> {
                    layoutChildren(it, param, false, 0, 0)
                }
            }
        }
    }

    /**
     * 布局提示图片
     */
    private fun layoutArrowImg(param: LeadLayoutParams, it: View) {
        var cTop = 0
        val cLeft = 0

        if (mHintView != null) {
            when (mQuadrant) {
                FIRST_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!!
                    param.mGravity = Gravity.LEFT or Gravity.TOP
                }

                SECOND_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!!
                    param.mGravity = Gravity.RIGHT or Gravity.TOP
                }

                THIRD_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1]- it.measuredHeight
                    param.mGravity = Gravity.RIGHT or Gravity.TOP
                }

                FOURTH_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1] - it.measuredHeight
                    param.mGravity = Gravity.LEFT or Gravity.TOP

                }
            }
        }

        layoutChildren(it, param, false, cLeft, cTop)
    }

    /**
     * 布局提示文字
     */
    private fun layoutHintText(param: LeadLayoutParams, it: View) {
        var cTop = 0
        val cLeft = 0

        if (mArrowImage != null && mHintView != null) {

            val params: LeadLayoutParams = mArrowImage?.layoutParams!! as LeadLayoutParams
            val arrowHeightCount = mArrowImage?.measuredHeight!! + params.topMargin + params.bottomMargin
            when (mQuadrant) {
                FIRST_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! + arrowHeightCount

                    param.mGravity = Gravity.LEFT or Gravity.TOP
                }

                SECOND_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! + arrowHeightCount


                    param.mGravity = Gravity.RIGHT or Gravity.TOP
                }

                THIRD_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1]  - arrowHeightCount - it.measuredHeight

                    param.mGravity = Gravity.RIGHT or Gravity.TOP
                }

                FOURTH_QUADRANT -> {
                    cTop = mHintViewLocation[1] - mLeadViewLocation[1] - arrowHeightCount - it.measuredHeight

                    param.mGravity = Gravity.LEFT or Gravity.TOP
                }
            }
        }

        layoutChildren(it, param, false, cLeft, cTop)
    }

    private fun layoutSpecialChildren(cView: View, param: LeadLayoutParams, cLeft: Int, cTop: Int) {
        val cWidth = cView.measuredWidth
        val cHeight = cView.measuredHeight


        cView.layout(cLeft, cTop, cLeft + cWidth, cTop + cHeight)
    }

    /**
     * 拷贝FrameLayout的代码 注释是自己写代码
     *
     * 主要是通过 gravity 和 margin值 对子View 进行摆放
     */
    private fun layoutChildren(view: View, param: LeadLayoutParams, forceLeftGravity: Boolean, cL: Int, cT: Int) {
        val cWidth = view.measuredWidth
        val cHeight = view.measuredHeight

        var cLeft = cL
        var cTop = cT


        var gravity = param.mGravity
        if (gravity == -1) {
            gravity = Gravity.TOP or Gravity.START
        }

        val layoutDirection = layoutDirection
        val absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
        val verticalGravity = gravity and Gravity.VERTICAL_GRAVITY_MASK

        when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> cLeft += left + (right - left - cWidth) / 2 +
                    param.leftMargin - param.rightMargin

            Gravity.RIGHT -> {
                if (!forceLeftGravity) {
                    cLeft += right - cWidth - param.rightMargin
                } else {
                    cLeft += left + param.leftMargin
                }
            }

            Gravity.LEFT -> cLeft += left + param.leftMargin

            else -> cLeft += left + param.leftMargin
        }

        when (verticalGravity) {
            Gravity.TOP -> cTop += top + param.topMargin

            Gravity.CENTER_VERTICAL -> cTop += top + (bottom - top - cHeight) / 2 +
                    param.topMargin - param.bottomMargin

            Gravity.BOTTOM -> cTop += bottom - cHeight - param.bottomMargin

            else -> cTop += top + param.topMargin
        }

        view.layout(cLeft, cTop, cLeft + cWidth, cTop + cHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        //绘制背景色
        canvas?.drawColor(mBackgroundColor)

//        //不再范围内不继续绘制
        if (mHintView == null || !mIsInLeadView) return

        //计算出当前hintView 在的leadLayout的起始点
        val x = mHintViewLocation[0].toFloat() - mLeadViewLocation[0].toFloat()
        val y = mHintViewLocation[1].toFloat() - mLeadViewLocation[1].toFloat()

        val startX = x - mHintMoreWidth
        val startY = y - mHintMoreHeight
        val endX = x + mHintView?.width?.toFloat()!! + mHintMoreWidth
        val endY = y + mHintView?.height?.toFloat()!! + mHintMoreHeight

        //掏空起始点
        drawHint(canvas, startX, startY, endX, endY)

        if(mIsDashPath)
            //画虚线
            drawHintDashPath(canvas, startX, startY, endX, endY)
    }

    /**
     * 绘制需要被掏空的形状
     */
    private fun drawHint(canvas: Canvas?, startX: Float, startY: Float, endX: Float, endY: Float) {
        //设置视图布尔运算
        mBackgroundPaint.xfermode = mXcode_Clear

        darwShape(canvas,startX,startY,endX,endY)

/*        canvas?.drawOval(x - mHintMoreWidth, //椭圆起始点x
                y - mHintMoreHeight, //椭圆起始点y
                x + mHintView?.width?.toFloat()!! + mHintMoreWidth, //椭圆结束点x
                y + mHintView?.height?.toFloat()!! + mHintMoreHeight //椭圆结束点y
                , mBackgroundPaint)*/

    }

    /**
     * 绘制需要被掏空形状的虚线
     */
    private fun drawHintDashPath(canvas: Canvas?, startX: Float, startY: Float, endX: Float, endY: Float) {
        //取消布尔运算
        mBackgroundPaint.xfermode = null
        //设置画直线格式
        mBackgroundPaint.style = Paint.Style.STROKE
        //设置虚线效果
        mBackgroundPaint.pathEffect = mDashPath
        mBackgroundPaint.color = Color.WHITE
        mBackgroundPaint.strokeWidth = 5f

        darwShape(canvas,startX - mDashLineOffset,startY - mDashLineOffset ,endX + mDashLineOffset,endY + mDashLineOffset)

        //取消虚线效果
        mBackgroundPaint.style = Paint.Style.FILL
        mBackgroundPaint.pathEffect = null
        mBackgroundPaint.color = Color.BLACK
        mBackgroundPaint.strokeWidth = 0f
    }

    /**
     * 绘制形状
     */
    private fun darwShape(canvas: Canvas?, startX: Float, startY: Float, endX: Float, endY: Float){
        when (mShape) {
            SQUARE -> canvas?.drawRect(startX,
                    startY,
                    endX,
                    endY,
                    mBackgroundPaint)

            CIRCLE -> {
                val cx = endX - startX
                val cy = endY - startY

                val radius = Math.max(cx, cy) / 2

                canvas?.drawCircle(startX + cx/2,
                        startY + cy/2,
                        radius,
                        mBackgroundPaint)
            }

            OVAL -> canvas?.drawOval(startX, //椭圆起始点x
                    startY, //椭圆起始点y
                    endX, //椭圆结束点x
                    endY //椭圆结束点y
                    , mBackgroundPaint)

        }
    }

    override fun onGlobalLayout() {
        //去除记载完成监听
        viewTreeObserver.removeOnGlobalLayoutListener(this)

        //设置加载完成
        mIsFinishLayout = true


        //获取屏幕所在位置
        getLocationOnScreen(mLeadViewLocation)

        //计算象限圆心点
        mZeroX = mLeadViewLocation[0] + width / 2
        mZeroY = mLeadViewLocation[1] + height / 2

        //根据ID获取需要提示的view
        if (mHintViewID != -1) {
            val v = findHintView(mHintViewID)
            mHintView = v ?: mHintView
        }

        //初始化在onDraw方法中用到的数据
        initDraw()
    }

    override fun onClick(v: View?) {

    }


    override fun generateDefaultLayoutParams(): LayoutParams {
        return LeadLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LeadLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return LeadLayoutParams(p)
    }


    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is LeadLayoutParams
    }

    /**
     * leadLayout的参数
     */
    class LeadLayoutParams : MarginLayoutParams {
        companion object {
            /**
             * 是箭头图片标识
             */
            val ARROW_IMG = 0x000000FF


            /**
             * 提示文本标识
             */
            val HINT_TEXT = 0x00FF0000

            val NONE = 0x0000000

        }

        var mViewType: Int = NONE

        /**
         * @see android.view.Gravity
         */
        var mGravity: Int = Gravity.LEFT or Gravity.TOP

        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs) {
            val attrEmtity = c?.obtainStyledAttributes(attrs, R.styleable.LeadLayout_Layout)!!

            mViewType = attrEmtity.getInt(R.styleable.LeadLayout_Layout_layout_viewType, NONE)
            mGravity = attrEmtity.getInt(R.styleable.LeadLayout_Layout_layout_gravity, Gravity.NO_GRAVITY)
            attrEmtity.recycle()
        }

        constructor(source: LayoutParams?) : super(source)
        constructor(width: Int, height: Int) : super(width, height)

        fun setViewType(type: Int): LeadLayoutParams {
            mViewType = type

            return this
        }
    }
}

/**
 * 判断是否view是否被包含，这里的被包含指的是屏幕
 * @return true 代表包含，flase 代表不包含
 */
fun View.isInMyScreen(view: View?): Boolean {
    if (view == null) return false

    val myLocation = IntArray(2)
    val viewLocation = IntArray(2)

    getLocationOnScreen(myLocation)
    view.getLocationOnScreen(viewLocation)

    return myLocation[0] <= viewLocation[0] &&
            myLocation[1] <= viewLocation[1] &&
            width > view.width &&
            height > view.height
}

fun ViewGroup.eachChild(c: ((view: View) -> Unit)) {
    (0 until childCount).map { getChildAt(it) }
            .forEach {
                c.invoke(it)
            }
}
