package com.urbanairship.debug.deviceinfo.preferences

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.support.v7.preference.Preference
import android.util.AttributeSet

import com.urbanairship.UAirship
import com.urbanairship.debug.R
import com.urbanairship.push.RegistrationListener

import java.lang.ref.WeakReference

class PushOptInPreference : Preference {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    private val registrationListener = object : RegistrationListener {
        override fun onChannelCreated(channelId: String) {}

        override fun onChannelUpdated(channelId: String) {}

        override fun onPushTokenUpdated(token: String) { notifyChanged() }
    }

    override fun getSummary(): CharSequence {
        return if (UAirship.shared().pushManager.isOptIn) context.getString(R.string.ua_opted_in) else context.getString(R.string.ua_opted_out)
    }

    override fun onAttached() {
        super.onAttached()
        UAirship.shared().pushManager.addRegistrationListener(registrationListener)
    }

    override fun onDetached() {
        super.onDetached()
        UAirship.shared().pushManager.removeRegistrationListener(registrationListener)
    }
}