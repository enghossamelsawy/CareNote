package com.hossam.carenote.core.data.sync

import com.hossam.carenote.core.common.logging.Logger

/** Keeps warnings and errors, so a test can check that a problem was logged. */
internal class RecordingLogger : Logger {
    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()

    override fun d(tag: String, message: String) = Unit

    override fun i(tag: String, message: String) = Unit

    override fun w(tag: String, message: String, throwable: Throwable?) {
        warnings += message
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        errors += message
    }
}
