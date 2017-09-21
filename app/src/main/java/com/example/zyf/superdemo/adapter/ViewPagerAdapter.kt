@file:Suppress("UNREACHABLE_CODE")

package com.example.zyf.superdemo.adapter

import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup

/**
 * Created by zyf on 2017/9/8.
 */
class ViewPagerAdapter : PagerAdapter {

    var views : ArrayList<View>
    var tabs : List<String>

    constructor(views: ArrayList<View>,tabs : List<String>) : super() {
        this.views = views
        this.tabs = tabs
    }

    override fun getCount(): Int{
        return views.size
    }

    override fun isViewFromObject(view: View?, obj : Any?): Boolean {
        return view == obj
    }

    override fun destroyItem(container: ViewGroup?, position: Int, obj: Any?) {
        container?.removeView(views[position])

    }

    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
        container?.addView(views[position])
        return views[position]
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabs[position]
    }

}