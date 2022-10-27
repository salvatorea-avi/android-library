/* Copyright Airship and Contributors */
package com.urbanairship.android.layout.model

import com.urbanairship.android.layout.ModelEnvironment
import com.urbanairship.android.layout.info.LabelInfo
import com.urbanairship.android.layout.info.VisibilityInfo
import com.urbanairship.android.layout.property.Border
import com.urbanairship.android.layout.property.Color
import com.urbanairship.android.layout.property.EnableBehaviorType
import com.urbanairship.android.layout.property.EventHandler
import com.urbanairship.android.layout.property.TextAppearance
import com.urbanairship.android.layout.property.ViewType

internal class LabelModel(
    val text: String,
    val textAppearance: TextAppearance,
    override val contentDescription: String? = null,
    backgroundColor: Color? = null,
    border: Border? = null,
    visibility: VisibilityInfo? = null,
    eventHandlers: List<EventHandler>? = null,
    enableBehaviors: List<EnableBehaviorType>? = null,
    environment: ModelEnvironment
) : BaseModel(
    viewType = ViewType.LABEL,
    backgroundColor = backgroundColor,
    border = border,
    visibility = visibility,
    eventHandlers = eventHandlers,
    enableBehaviors = enableBehaviors,
    environment = environment
), Accessible {
    constructor(info: LabelInfo, env: ModelEnvironment) : this(
        text = info.text,
        textAppearance = info.textAppearance,
        contentDescription = info.contentDescription,
        backgroundColor = info.backgroundColor,
        border = info.border,
        visibility = info.visibility,
        eventHandlers = info.eventHandlers,
        enableBehaviors = info.enableBehaviors,
        environment = env
    )
}
