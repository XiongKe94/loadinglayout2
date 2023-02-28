/*
 * Copyright 2016 czy1121
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ezy.ui.layout

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import ezy.library.loadinglayout.R

class LoadingLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                              defStyleAttr: Int = R.attr.styleLoadingLayout) :
        FrameLayout(context, attrs, defStyleAttr) {
    enum class State {
        ERROR, CONTENT, EMPTY, LOADING
    }

    interface OnInflateListener {
        fun onInflate(inflated: View?)
    }

    var mEmptyImageResId: Int
    var mEmptyText: CharSequence?
    var mErrorImageResId: Int
    var mErrorText: CharSequence?
    var mRetryText: CharSequence?
    var mRetryButtonClickListener = OnClickListener { v ->
        if (mRetryListener != null) {
            mRetryListener!!.onClick(v)
        }
    }
    var mRetryListener: OnClickListener? = null
    var mOnEmptyInflateListener: OnInflateListener? = null
    var mOnErrorInflateListener: OnInflateListener? = null
    var mTextColor: Int
    var mTextSize: Int
    var mButtonTextColor: Int
    var mButtonTextSize: Int
    var mButtonBackground: Drawable?
    var mEmptyResId = NO_ID
    var mLoadingResId = NO_ID
    var mErrorResId = NO_ID
    var mContentId = NO_ID
    var pageState: State? = null
    var mLayouts: MutableMap<Int, View> = HashMap()


    companion object {
        fun wrap(activity: Activity): LoadingLayout {
            return wrap((activity.findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0))
        }

        fun wrap(fragment: Fragment): LoadingLayout {
            return wrap(fragment.view)
        }

        fun wrap(view: View?): LoadingLayout {
            if (view == null) {
                throw RuntimeException("content view can not be null")
            }
            val parent = view.parent as ViewGroup
            val lp = view.layoutParams
            val index = parent.indexOfChild(view)
            parent.removeView(view)
            val layout = LoadingLayout(view.context)
            parent.addView(layout, index, lp)
            layout.addView(view)
            layout.setContentView(view)
            return layout
        }
    }

    private fun dp2px(dp: Float): Int {
        return (resources.displayMetrics.density * dp).toInt()
    }

    var mInflater: LayoutInflater

    init {
        mInflater = LayoutInflater.from(context)
        val a = context.obtainStyledAttributes(attrs, R.styleable.LoadingLayout, defStyleAttr, R.style.LoadingLayout_Style)
        mEmptyImageResId = a.getResourceId(R.styleable.LoadingLayout_llEmptyImage, NO_ID)
        mEmptyText = a.getString(R.styleable.LoadingLayout_llEmptyText)
        mErrorImageResId = a.getResourceId(R.styleable.LoadingLayout_llErrorImage, NO_ID)
        mErrorText = a.getString(R.styleable.LoadingLayout_llErrorText)
        mRetryText = a.getString(R.styleable.LoadingLayout_llRetryText)
        mTextColor = a.getColor(R.styleable.LoadingLayout_llTextColor, -0x666667)
        mTextSize = a.getDimensionPixelSize(R.styleable.LoadingLayout_llTextSize, dp2px(16f))
        mButtonTextColor = a.getColor(R.styleable.LoadingLayout_llButtonTextColor, -0x666667)
        mButtonTextSize = a.getDimensionPixelSize(R.styleable.LoadingLayout_llButtonTextSize, dp2px(16f))
        mButtonBackground = a.getDrawable(R.styleable.LoadingLayout_llButtonBackground)
        mEmptyResId = a.getResourceId(R.styleable.LoadingLayout_llEmptyResId, R.layout._loading_layout_empty)
        mLoadingResId = a.getResourceId(R.styleable.LoadingLayout_llLoadingResId, R.layout._loading_layout_loading)
        mErrorResId = a.getResourceId(R.styleable.LoadingLayout_llErrorResId, R.layout._loading_layout_error)
        val mLoadingVisibility = a.getBoolean(R.styleable.LoadingLayout_llLoadingVisibility, false)
        a.recycle()
        if (mLoadingVisibility) {
            showLoading()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount == 0) {
            return
        }
        if (childCount > 1) {
            removeViews(1, childCount - 1)
        }
        val view = getChildAt(0)
        setContentView(view)
        pageState = State.CONTENT
    }

    private fun setContentView(view: View) {
        mContentId = view.id
        mLayouts[mContentId] = view
    }

    fun setLoading(@LayoutRes id: Int): LoadingLayout {
        if (mLoadingResId != id) {
            remove(mLoadingResId)
            mLoadingResId = id
        }
        return this
    }

    fun setEmpty(@LayoutRes id: Int): LoadingLayout {
        if (mEmptyResId != id) {
            remove(mEmptyResId)
            mEmptyResId = id
        }
        return this
    }

    fun setOnEmptyInflateListener(listener: OnInflateListener): LoadingLayout {
        mOnEmptyInflateListener = listener
        if (mOnEmptyInflateListener != null && mLayouts.containsKey(mEmptyResId)) {
            listener.onInflate(mLayouts[mEmptyResId])
        }
        return this
    }

    fun setOnErrorInflateListener(listener: OnInflateListener): LoadingLayout {
        mOnErrorInflateListener = listener
        if (mOnErrorInflateListener != null && mLayouts.containsKey(mErrorResId)) {
            listener.onInflate(mLayouts[mErrorResId])
        }
        return this
    }

    fun setEmptyImage(@DrawableRes resId: Int): LoadingLayout {
        mEmptyImageResId = resId
        image(mEmptyResId, R.id.empty_image, mEmptyImageResId)
        return this
    }

    fun setEmptyText(value: String?): LoadingLayout {
        mEmptyText = value
        text(mEmptyResId, R.id.empty_text, mEmptyText)
        return this
    }

    fun setErrorImage(@DrawableRes resId: Int): LoadingLayout {
        mErrorImageResId = resId
        image(mErrorResId, R.id.error_image, mErrorImageResId)
        return this
    }

    fun setErrorText(value: String?): LoadingLayout {
        mErrorText = value
        text(mErrorResId, R.id.error_text, mErrorText)
        return this
    }

    fun setRetryText(text: String?): LoadingLayout {
        mRetryText = text
        text(mErrorResId, R.id.retry_button, mRetryText)
        return this
    }

    fun setRetryListener(listener: OnClickListener?): LoadingLayout {
        mRetryListener = listener
        return this
    }

    fun showLoading() {
        pageState = State.LOADING
        show(mLoadingResId)
    }

    fun showEmpty() {
        pageState = State.EMPTY
        show(mEmptyResId)
    }

    fun showError() {
        pageState = State.ERROR
        show(mErrorResId)
    }

    fun showContent() {
        pageState = State.CONTENT
        show(mContentId)
    }

    private fun show(layoutId: Int) {
        for (view in mLayouts.values) {
            view.visibility = GONE
        }
        layout(layoutId)!!.visibility = VISIBLE
    }

    private fun remove(layoutId: Int) {
        if (mLayouts.containsKey(layoutId)) {
            val vg = mLayouts.remove(layoutId)
            removeView(vg)
        }
    }

    private fun layout(layoutId: Int): View? {
        if (mLayouts.containsKey(layoutId)) {
            return mLayouts[layoutId]
        }
        val layout = mInflater.inflate(layoutId, this, false)
        layout.visibility = GONE
        addView(layout)
        mLayouts[layoutId] = layout
        if (layoutId == mEmptyResId) {
            val img = layout.findViewById<ImageView>(R.id.empty_image)
            if (img != null && mEmptyImageResId != NO_ID) {
                img.setImageResource(mEmptyImageResId)
            }
            val view = layout.findViewById<TextView>(R.id.empty_text)
            if (view != null) {
                view.text = mEmptyText
                view.setTextColor(mTextColor)
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize.toFloat())
            }
            if (mOnEmptyInflateListener != null) {
                mOnEmptyInflateListener!!.onInflate(layout)
            }
        } else if (layoutId == mErrorResId) {
            val img = layout.findViewById<ImageView>(R.id.error_image)
            if (img != null && mErrorImageResId != NO_ID) {
                img.setImageResource(mErrorImageResId)
            }
            val txt = layout.findViewById<TextView>(R.id.error_text)
            if (txt != null) {
                txt.text = mErrorText
                txt.setTextColor(mTextColor)
                txt.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize.toFloat())
            }
            val btn = layout.findViewById<TextView>(R.id.retry_button)
            if (btn != null) {
                btn.text = mRetryText
                btn.setTextColor(mButtonTextColor)
                btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, mButtonTextSize.toFloat())
                btn.background = mButtonBackground
                btn.setOnClickListener(mRetryButtonClickListener)
            }
            if (mOnErrorInflateListener != null) {
                mOnErrorInflateListener!!.onInflate(layout)
            }
        }
        return layout
    }

    private fun text(layoutId: Int, ctrlId: Int, value: CharSequence?) {
        if (mLayouts.containsKey(layoutId)) {
            val view = mLayouts[layoutId]!!.findViewById<TextView>(ctrlId)
            if (view != null) {
                view.text = value
            }
        }
    }

    private fun image(layoutId: Int, ctrlId: Int, resId: Int) {
        if (mLayouts.containsKey(layoutId)) {
            val view = mLayouts[layoutId]!!.findViewById<ImageView>(ctrlId)
            view?.setImageResource(resId)
        }
    }

}