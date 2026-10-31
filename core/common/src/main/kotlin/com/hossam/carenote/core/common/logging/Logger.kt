package com.hossam.carenote.core.common.logging

/**
 * App-wide logging. Depend on this instead of Timber so tests can check what was logged.
 *
 * Never log transcripts, vitals or resident names: that is health data, and logcat can be
 * read over adb. Log ids and states instead.
 */
interface Logger {
    fun d(tag: String, message: String)

    fun i(tag: String, message: String)

    fun w(tag: String, message: String, throwable: Throwable? = null)

    fun e(tag: String, message: String, throwable: Throwable? = null)
}
