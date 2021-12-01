/* Copyright Airship and Contributors */

package com.urbanairship.android.layout.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.urbanairship.android.layout.BasePayload;
import com.urbanairship.android.layout.ModalPresentation;
import com.urbanairship.android.layout.event.Event;
import com.urbanairship.android.layout.event.EventListener;
import com.urbanairship.android.layout.model.BaseModel;
import com.urbanairship.android.layout.property.PresentationType;
import com.urbanairship.android.layout.view.ModalView;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonMap;

import java.io.IOException;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.urbanairship.android.layout.util.ResourceUtils.readJsonAsset;

public class ModalActivity extends AppCompatActivity {
    // TODO: this should be an IAA ID that we can load a layout for...
    public static final String EXTRA_MODAL_ASSET = "com.urbanairship.android.automation.renderer.EXTRA_MODAL_ASSET";

    @Nullable
    private ModalView modalView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String fileName = getIntent().getStringExtra(EXTRA_MODAL_ASSET);
        try {
            JsonMap json = Objects.requireNonNull(readJsonAsset(this, "sample_layouts/" + fileName));
            BasePayload payload = BasePayload.fromJson(json);
            if (payload.getPresentation().getType() != PresentationType.MODAL) {
                Toast.makeText(this, "Layout is not a modal!", Toast.LENGTH_LONG);
                finish();
                return;
            }

            ModalPresentation presentation = (ModalPresentation) payload.getPresentation();
            BaseModel view = payload.getView();

            modalView = ModalView.create(this, view, presentation);
            modalView.setLayoutParams(new ConstraintLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            setContentView(modalView);

            if (presentation.isDismissOnTouchOutside()) {
                modalView.setOnClickOutsideListener(v -> finish());
            }

            view.addListener(eventListener);

        } catch (@NonNull JsonException | IOException e) {
            Log.e(getClass().getSimpleName(), "Failed to load modal!", e);
            Toast.makeText(this, "Failed to load modal!", Toast.LENGTH_LONG);
            finish();
        }
    }

    private final EventListener eventListener = new EventListener() {
        @Override
        public boolean onEvent(@NonNull Event event) {
            switch (event.getType()) {
                case BUTTON_BEHAVIOR_CANCEL:
                case BUTTON_BEHAVIOR_DISMISS:
                    finish();
                    return true;
            }

            throw new IllegalStateException("Unhandled event at top level! event type = " + event.getType().name());

            // We're at the top level, so we should always consume events.
            //return true;
        }
    };
}