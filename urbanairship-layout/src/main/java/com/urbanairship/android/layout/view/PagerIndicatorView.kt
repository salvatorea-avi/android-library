/* Copyright Airship and Contributors */
package com.urbanairship.android.layout.view

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Checkable
import android.widget.ImageView
import android.widget.LinearLayout
import com.urbanairship.android.layout.environment.ViewEnvironment
import com.urbanairship.android.layout.model.PagerIndicatorModel
import com.urbanairship.android.layout.util.LayoutUtils
import com.urbanairship.android.layout.util.ResourceUtils
import com.urbanairship.android.layout.widget.ShapeView

internal class PagerIndicatorView(
    context: Context,
    private val model: PagerIndicatorModel,
    viewEnvironment: ViewEnvironment
) : LinearLayout(context), BaseView {

    private val modelListener: PagerIndicatorModel.Listener =
        object : PagerIndicatorModel.Listener {
            private var isInitialized = false

            override fun onInit(size: Int, position: Int) {
                if (!isInitialized) {
                    isInitialized = true
                    setCount(size)
                }
                setPosition(position)
            }

            override fun onUpdate(position: Int) = setPosition(position)
        }

    init {
        id = generateViewId()
        configure()
    }

    private fun configure() {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        model.setListener(modelListener)

        LayoutUtils.applyBorderAndBackground(this, model)

        model.onConfigured()
    }

    /**
     * Sets the number of indicator dots to be displayed.
     *
     * @param count The number of dots to display.
     */
    fun setCount(count: Int) {
        val bindings = model.bindings
        val checked = bindings.selected
        val unchecked = bindings.unselected
        val spacing = ResourceUtils.dpToPx(context, model.indicatorSpacing).toInt()
        val halfSpacing = (spacing / 2f).toInt()
        for (i in 0 until count) {
            val view: ImageView = ShapeView(
                context, checked.shapes, unchecked.shapes, checked.icon, unchecked.icon
            ).apply {
                id = model.getIndicatorViewId(i)
                adjustViewBounds = true
            }

            val lp = LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply {
                marginStart = if (i == 0) spacing else halfSpacing
                marginEnd = if (i == count - 1) spacing else halfSpacing
            }
            addView(view, lp)
        }
    }

    /**
     * Updates the highlighted dot view in the indicator.
     *
     * @param position The position of the dot to highlight.
     */
    fun setPosition(position: Int) {
        for (i in 0 until childCount) {
            (getChildAt(i) as Checkable).isChecked = i == position
        }
    }
}
