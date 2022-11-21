/* Copyright Airship and Contributors */
package com.urbanairship.android.layout.view

import android.content.Context
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isGone
import com.urbanairship.android.layout.model.CheckableModel
import com.urbanairship.android.layout.model.CheckboxModel
import com.urbanairship.android.layout.property.CheckboxStyle
import com.urbanairship.android.layout.property.SwitchStyle
import com.urbanairship.android.layout.widget.CheckableView
import com.urbanairship.android.layout.widget.ShapeButton

internal class CheckboxView(
    context: Context,
    model: CheckboxModel,
) : CheckableView<CheckboxModel>(context, model) {
    init {
        model.listener = object : CheckableModel.Listener {
            override fun onSetChecked(isChecked: Boolean) = setCheckedInternal(isChecked)
            override fun onSetEnabled(isEnabled: Boolean) = setEnabled(isEnabled)
            override fun setVisibility(visible: Boolean) {
                this@CheckboxView.isGone = visible
            }
        }
    }

    override fun createSwitchView(style: SwitchStyle): SwitchCompat {
        return object : SwitchCompat(context) {
            override fun toggle() {
                // Not calling super, because the controller/model handles updating the view.
                checkedChangeListener?.onCheckedChange(this, !isChecked)
            }
        }
    }

    override fun createCheckboxView(style: CheckboxStyle): ShapeButton {
        val checked = style.bindings.selected
        val unchecked = style.bindings.unselected
        return object : ShapeButton(
            context, checked.shapes, unchecked.shapes, checked.icon, unchecked.icon
        ) {
            override fun toggle() {
                // Not calling super, because the controller/model handles updating the view.
                checkedChangeListener?.onCheckedChange(this, !isChecked)
            }
        }
    }
}
