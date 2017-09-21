package com.example.zyf.superdemo.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.zyf.superdemo.R

/**
 * 操作引导View
 *
 *
 */
@Suppress("CAST_NEVER_SUCCEEDS")
open class LeadLayout : ViewGroup, ViewTreeObserver.OnGlobalLayoutListener {

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
         * 传入x,y获取象限
         */
        fun getQuanrant(x: Int, y: Int): Int {
            if (x > 0 && y > 0) {
                return FIRST_QUADRANT
            } else if (x < 0 && y > 0) {
                return SECOND_QUADRANT
            } else if (x < 0 && y < 0) {
                return THIRD_QUADRANT
            } else if (x > 0 && y < 0) {
                return FOURTH_QUADRANT
            }

            return CENTER
        }
    }

    /**
     * 屏幕宽度
     */
    val SCREEN_WIDTH: Int

    /**
     * 屏幕高度
     */
    val SCREEN_HEIGHT: Int

    /**
     * 像素密度
     */
    val DENSITY: Float

    /**
     * 需要被提示view的屏幕所在位置
     */
    val mHintViewLocation = IntArray(2)

    /**
     * 引导view的所在屏幕位置
     */
    val mLeadViewLocation = IntArray(2)

    /**
     * 是否在当前引导view范围内
     */
    var mIsInLeadView: Boolean = false

    /**
     * 是否完成加载布局
     */
    var mIsFinishLayout: Boolean = false

    /**
     * 需要被提示的view
     */
    var mHintView: View? = null
        set(value) {
            field = value
            if (mIsFinishLayout) {
                if (initInfo()) {
                    invalidate()
                }
            }
        }

    /**
     * 提示文本
     */
    var mHintText: View? = null

    /**
     * 提示箭头
     */
    var mArrowImage: View? =  null

    /**
     * 我知道了按钮
     */
    var mUnderstandButton: View? = null

    /**
     * 背景画笔
     */
    val mBackgroundPaint = Paint()

    /**
     * 视图布尔运算对象
     */
    var mXcode_Clear: PorterDuffXfermode? = null

    /**
     * 提示框倍率 比hintView 大多少倍
     */
    var mHintMultiplyingPower: Float = 1.2f

    //设置虚线
    var mDashPath = DashPathEffect(floatArrayOf(15f, 15f), 0f)

    /**
     * 虚线边框偏移量
     */
    var mDashLineOffset: Float
        set(value) {
            field = value * DENSITY
        }

    /**
     * 是否使用  LeadLayoutParams 中的象限进行自动布局
     */
    var mIsSelfMotionLayout = true

    /**
     * 通过倍数 mHintMultiplyingPower 获取 比原mHintView多或少出来的宽、高值
     */
    var mHintMoreWidth = 0f
    var mHintMoreHeight = 0f

    /**
     * 当前所在象限
     */
    var mQuanrant = FIRST_QUADRANT

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
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        //使其viewGroup会调用onDraw方法
        setWillNotDraw(false)

        //设置可点击，不让事件穿透
        if (!isClickable) {
            isClickable = true
        }

        val typeArray = context?.obtainStyledAttributes(attrs, R.styleable.LeadLayout)

        mIsSelfMotionLayout = typeArray?.getBoolean(R.styleable.LeadLayout_self_motion_layout, true)!!


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

        //通过倍数 mHintMultiplyingPower 获取 比原mHintView多出来的宽、高值
        if (mHintMultiplyingPower >= 1) {
            //放大或者不变
            mHintMoreWidth = mHintView?.width!! * (mHintMultiplyingPower - 1) / 2
            mHintMoreHeight = mHintView?.height!! * (mHintMultiplyingPower - 1) / 2
        } else {
            //缩小
            mHintMoreWidth = -mHintView?.width!! * (1 - mHintMultiplyingPower) / 2f
            mHintMoreHeight = -mHintView?.height!! * (1 - mHintMultiplyingPower) / 2
        }

        val x = mZeroX - mHintViewLocation[0]
        val y = mZeroY - mHintViewLocation[1]

        mQuanrant = getQuanrant(x, y)

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        eachChild {
            if (it.visibility == View.GONE) {
                return@eachChild
            }
            //设置view的大小
            val param = it.layoutParams as LeadLayoutParams

            val childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    paddingLeft + paddingRight + param.leftMargin + param.rightMargin, param.width)
            val childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    paddingTop + paddingBottom + param.topMargin + param.bottomMargin, param.height)

            when (param.mViewType) {
            //设置箭头对象
                LeadLayoutParams.ARROW_IMG -> mArrowImage = it

            //设置提示文本的位置
                LeadLayoutParams.HINT_TEXT -> mHintText = it

            //设置我知道、我了解按钮位置
                LeadLayoutParams.UNDERSTAND_BTN -> mUnderstandButton = it
            }

            it.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        val parentLeft = paddingLeft
        val parentRight = right - left

        val parentTop = paddingTop
        val parentBottom = bottom - top

        eachChild {
            if (it.visibility == View.GONE) {
                return@eachChild
            }

            val param = it.layoutParams as LeadLayoutParams

            //判断是否需要自动布局 mIsSelfMotionLayout 和 当前是否设置 mGravity ，如果设置 mGravity 表示不需要自动化布局
            param.mViewType = if (mIsSelfMotionLayout && param.mGravity == Gravity.NO_GRAVITY) param.mViewType else LeadLayoutParams.NONE

            when (param.mViewType) {
            //设置箭头位置
                LeadLayoutParams.ARROW_IMG -> {
                    var cTop : Int = 0

                    if(mHintView != null){
                        when(mQuanrant){
                            FIRST_QUADRANT ->{
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt()

                                param.mGravity = Gravity.RIGHT or Gravity.TOP
                            }

                            SECOND_QUADRANT -> {
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt()

                                param.mGravity = Gravity.LEFT or Gravity.TOP
                            }

                            THIRD_QUADRANT -> {
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt() - param.height
                                param.mGravity = Gravity.LEFT or Gravity.BOTTOM
                            }

                            FOURTH_QUADRANT -> {
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt() - param.height

                                param.mGravity = Gravity.RIGHT or Gravity.BOTTOM
                            }
                        }
                    }

                    layoutChildren(it,param,false,0,cTop)
                }

            //设置提示文本的位置
                LeadLayoutParams.HINT_TEXT -> {
                    var cTop : Int = 0
                    var cLeft : Int = 0

                    if(mArrowImage != null){
                        when(mQuanrant){
                            FIRST_QUADRANT ->{
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt()
                                + (mArrowImage?.layoutParams?.height ?:0)



                                param.mGravity = Gravity.RIGHT or Gravity.TOP
                            }

                            SECOND_QUADRANT -> {
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt()
                                + (mArrowImage?.layoutParams?.height ?:0)

                                param.mGravity = Gravity.LEFT or Gravity.TOP
                            }

                            THIRD_QUADRANT -> {
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt() - param.width
                                param.mGravity = Gravity.LEFT or Gravity.BOTTOM
                            }

                            FOURTH_QUADRANT -> {
                                cTop = (mHintViewLocation[1] - mLeadViewLocation[1] + mHintView?.height!! * mHintMultiplyingPower).toInt() - param.width

                                param.mGravity = Gravity.RIGHT or Gravity.BOTTOM
                            }
                        }
                    }

                    layoutChildren(it,param,false,cLeft,cTop)
                }

            //设置我知道、我了解按钮位置
                LeadLayoutParams.UNDERSTAND_BTN -> {

                    layoutSpecialChildren(it,param,0,0)
                }

            //设置默认类型位置
                LeadLayoutParams.NONE -> {
                    layoutChildren(it, param, false,0,0)
                }
            }
        }
    }

    protected fun layoutSpecialChildren(cView :View, param: LeadLayoutParams, cLeft : Int,cTop:Int){
        val cWidth = cView.measuredWidth
        val cHeight = cView.measuredHeight


        cView.layout(cLeft,cTop,cLeft+cWidth, cTop + cHeight)
    }

     /**
     * 拷贝FrameLayout的代码 注释是自己写代码
     *
     * 主要是通过 gravity 和 margin值 对子View 进行摆放
     */
    protected fun layoutChildren(view: View, param: LeadLayoutParams, forceLeftGravity: Boolean, cL : Int,cT:Int) {
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

        /* when (param.mGravity) {

             Gravity.BOTTOM -> {
                 cBottom = bottom - top - param.bottomMargin
                 cTop = bottom - top - cHeight - param.bottomMargin
                 cLeft += param.leftMargin + paddingLeft
                 cRight += param.leftMargin + paddingLeft
             }

             Gravity.RIGHT -> {
                 cLeft = right - left - cWidth - param.rightMargin
                 cRight = right - left - param.rightMargin
             }

             Gravity.CENTER_VERTICAL -> {
                 cTop = (bottom - top) / 2 - cHeight / 2
                 cBottom = cTop + cHeight

                 cLeft += param.leftMargin + paddingLeft
                 cRight += param.leftMargin + paddingLeft
             }

             Gravity.CENTER_HORIZONTAL -> {
                 cLeft = (right - left) / 2 - cWidth / 2
                 cRight = cLeft + cWidth

                 cTop += param.topMargin + paddingTop
                 cBottom += param.topMargin + paddingTop
             }

             Gravity.CENTER -> {
                 cTop = bottom / 2 - cHeight / 2 +(paddingTop - paddingBottom + param.topMargin - param.bottomMargin)
                 cBottom = cTop + cHeight
                 cLeft = right / 2 - cWidth / 2 + (paddingLeft - paddingRight + param.leftMargin - param.rightMargin)
                 cRight = cLeft + cWidth

             }

             Gravity.CENTER_VERTICAL or Gravity.RIGHT -> {
                 cTop = bottom / 2 - cHeight / 2 +(paddingTop - paddingBottom + param.topMargin - param.bottomMargin)
                 cBottom = cTop + cHeight

                 cRight = right - left - param.rightMargin
                 cLeft = (right - left) - cWidth - param.rightMargin
             }

             Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM -> {
                 cLeft = right / 2 - cWidth / 2 + (paddingLeft - paddingRight + param.leftMargin - param.rightMargin)
                 cRight = cLeft + cWidth

                 cBottom = bottom - top - param.bottomMargin
                 cTop = bottom - top - cHeight - param.bottomMargin
             }

             Gravity.BOTTOM or Gravity.RIGHT -> {
                 cBottom = bottom - top - param.bottomMargin
                 cTop = bottom - top - cHeight - param.bottomMargin
                 cRight = right - left - param.rightMargin
                 cLeft = (right - left) - cWidth - param.rightMargin
             }

             else ->{
                 cLeft += param.leftMargin + paddingLeft
                 cRight += param.leftMargin + paddingLeft
                 cTop += param.topMargin + paddingTop
                 cBottom += param.topMargin + paddingTop
             }
         }*/

/*        //设置magin
        cTop += param.topMargin + paddingTop
        cBottom += param.topMargin + paddingTop
        cLeft += param.leftMargin + paddingLeft
        cRight += param.leftMargin + paddingLeft*/

        //如果right 值大于 left 把left值复制给right值 bottom同理
/*        cRight = Math.min((right - left), cRight)
        cBottom = Math.min((bottom - top), cBottom)*/

        view.layout(cLeft, cTop, cLeft + cWidth, cTop + cHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        //绘制背景色
        canvas?.drawColor(mBackgroundColor)

//        //不再范围内不继续绘制
//        if(mHintView == null || !mIsInLeadView) return

        drawHint(canvas)
        //画虚线
        drawHintDashPath(canvas)
    }

    /**
     * 绘制需要被掏空的形状
     */
    private fun drawHint(canvas: Canvas?) {
        //设置视图布尔运算
        mBackgroundPaint.xfermode = mXcode_Clear

//        canvas?.drawOval(mHintViewLocation[0].toFloat() - mHintMoreWidth, //椭圆起始点x
//                mHintViewLocation [1].toFloat() - mHintMoreHeight, //椭圆起始点y
//                mHintView?.width?.toFloat()!! + mHintMoreWidth, //椭圆结束点x
//                mHintView?.height?.toFloat()!! + mHintMoreWidth //椭圆结束点y
//                ,mBackgroundPaint)

        canvas?.drawOval(mHintViewLocation[0].toFloat(), mHintViewLocation [1].toFloat()
                , 400f, 200f, mBackgroundPaint)
    }

    /**
     * 绘制需要被掏空形状的虚线
     */
    private fun drawHintDashPath(canvas: Canvas?) {
        //取消布尔运算
        mBackgroundPaint.xfermode = null
        //设置画直线格式
        mBackgroundPaint.style = Paint.Style.STROKE
        //设置虚线效果
        mBackgroundPaint.pathEffect = mDashPath
        mBackgroundPaint.color = Color.WHITE
        mBackgroundPaint.strokeWidth = 5f

//        canvas?.drawOval(mHintViewLocation[0].toFloat() - mHintMoreWidth - mDashLineOffset, //椭圆起始点x
//                mHintViewLocation [1].toFloat() - mHintMoreHeight  - mDashLineOffset, //椭圆起始点y
//                mHintView?.width?.toFloat()!! + mHintMoreWidth + mDashLineOffset, //椭圆结束点x
//                mHintView?.height?.toFloat()!! + mHintMoreWidth + mDashLineOffset//椭圆结束点y
//                ,mBackgroundPaint)

        canvas?.drawOval(mHintViewLocation[0].toFloat() - mDashLineOffset, mHintViewLocation [1].toFloat() - mDashLineOffset
                , 400f + mDashLineOffset, 200f + mDashLineOffset, mBackgroundPaint)

        //取消虚线效果
        mBackgroundPaint.style = Paint.Style.FILL
        mBackgroundPaint.pathEffect = null
        mBackgroundPaint.color = Color.BLACK
        mBackgroundPaint.strokeWidth = 0f
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

        //初始化默认View
//        initDefaultView()

        //初始化在onDraw方法中用到的数据
        initDraw()

        //初始化信息 如坐标等
        initInfo()
    }

    /**
     * 初始化默认显示的View
     */
    private fun initDefaultView() {
        var text = false
        var img = false
        var btn = false

        //如果开发者有自己添加ViewType 就不添加默认View
        eachChild {
            val param = it.layoutParams as LeadLayoutParams

            if (param.mViewType == LeadLayoutParams.NONE) return@eachChild

            when (param.mViewType) {
                LeadLayoutParams.ARROW_IMG -> img = true

                LeadLayoutParams.HINT_TEXT -> text = true

                LeadLayoutParams.UNDERSTAND_BTN -> btn = true
            }
        }


        //初始化默认arrow button hintText
/*        if (!text) {
            mHintText.layoutParams = LeadLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    .setViewType(LeadLayoutParams.HINT_TEXT)
            addView(mHintText)
        }

        if (!img) {
            mUnderstandButton.layoutParams = LeadLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    .setViewType(LeadLayoutParams.UNDERSTAND_BTN)
            addView(mUnderstandButton)
        }

        if (!btn) {
            mArrowImage.layoutParams = LeadLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    .setViewType(LeadLayoutParams.ARROW_IMG)
            addView(mArrowImage)
        }*/
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
             * 我知道、我了解按钮标识
             */
            val UNDERSTAND_BTN = 0x0000FF00

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

            mViewType = attrEmtity.getType(R.styleable.LeadLayout_Layout_layout_viewType)
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
    val viewLoaction = IntArray(2)

    getLocationOnScreen(myLocation)
    view.getLocationOnScreen(viewLoaction)

    return myLocation[0] <= viewLoaction[0] &&
            myLocation[1] <= viewLoaction[1] &&
            width > view.width &&
            height > view.height
}

fun ViewGroup.eachChild(c: ((view: View) -> Unit)): Unit {
    (0 until childCount).map { getChildAt(it) }
            .forEach {
                c.invoke(it)
            }
}
