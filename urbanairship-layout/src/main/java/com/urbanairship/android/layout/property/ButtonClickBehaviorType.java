/*
 Copyright Airship and Contributors
 */

package com.urbanairship.android.layout.property;

import com.urbanairship.json.JsonList;
import com.urbanairship.json.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

public enum ButtonClickBehaviorType {
    // When a button is tapped, behaviors will be run in the order they are declared here.
    // Take care when adding or removing types--form submit needs to occur before dismiss or cancel.
    FORM_SUBMIT("form_submit"),
    PAGER_NEXT("pager_next"),
    PAGER_PREVIOUS("pager_previous"),
    DISMISS("dismiss"),
    CANCEL("cancel");

    @NonNull
    private final String value;

    ButtonClickBehaviorType(@NonNull String value) {
        this.value = value;
    }

    @NonNull
    public static ButtonClickBehaviorType from(@NonNull String value) {
        for (ButtonClickBehaviorType type : ButtonClickBehaviorType.values()) {
            if (type.value.equals(value.toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ButtonClickBehaviorType value: " + value);
    }

    @NonNull
    public static List<ButtonClickBehaviorType> fromList(@NonNull JsonList json) {
        if (json.isEmpty()) {
            return Collections.emptyList();
        }

        List<ButtonClickBehaviorType> behaviorTypes = new ArrayList<>(json.size());
        for (JsonValue value : json) {
            behaviorTypes.add(from(value.optString()));
        }
        behaviorTypes.sort(Comparator.comparingInt(Enum::ordinal));
        return behaviorTypes;
    }

    @NonNull
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
