package com.hossam.carenote.core.speech

import com.hossam.carenote.core.common.logging.Logger

/** For tests that don't check logging. */
internal object NoOpLogger : Logger {
    override fun d(tag: String, message: String) = Unit

    override fun i(tag: String, message: String) = Unit

    override fun w(tag: String, message: String, throwable: Throwable?) = Unit

    override fun e(tag: String, message: String, throwable: Throwable?) = Unit
}
