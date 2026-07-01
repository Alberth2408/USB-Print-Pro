package com.usbprintpro.data.document

import android.text.Html
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HTMLProcessor @Inject constructor() {

    fun extractText(html: String): String {
        val stripped = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        return stripped.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}
