package io.openremote.ourgrid

import android.os.Build
import android.os.Bundle
import io.openremote.orlib.ui.OrMainActivity


class MainActivity : OrMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val platform = "Android " + Build.VERSION.RELEASE
        val version = BuildConfig.VERSION_NAME

        loadUrl("https://staging.reschool.openremote.app/ourgrid/?consolePlatform=$platform&consoleName=ourgrid&consoleVersion=$version&consoleProviders=push storage")
    }
}