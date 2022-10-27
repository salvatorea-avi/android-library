/* Copyright Airship and Contributors */
package com.urbanairship.android.layout.view

import android.content.Context
import android.view.View
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.urbanairship.android.layout.Thomas
import com.urbanairship.android.layout.environment.ViewEnvironment
import com.urbanairship.android.layout.model.ScrollLayoutModel
import com.urbanairship.android.layout.property.Direction
import com.urbanairship.android.layout.util.LayoutUtils

internal class ScrollLayoutView(
    context: Context,
    private val model: ScrollLayoutModel,
    private val viewEnvironment: ViewEnvironment
) : NestedScrollView(context), BaseView {

    init {
        id = model.viewId
        configure()
    }

    private fun configure() {
        isFillViewport = false
        clipToOutline = true

        LayoutUtils.applyBorderAndBackground(this, model)

        val contentView = Thomas.view(context, model.view, viewEnvironment).apply {
            layoutParams = if (model.direction == Direction.VERTICAL) {
                LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            } else {
                LayoutParams(WRAP_CONTENT, MATCH_PARENT)
            }
        }
        addView(contentView)

        // Pass along any calls to apply insets to the view.
        ViewCompat.setOnApplyWindowInsetsListener(this) { _: View, insets: WindowInsetsCompat ->
            ViewCompat.dispatchApplyWindowInsets(contentView, insets)
        }
    }
}
