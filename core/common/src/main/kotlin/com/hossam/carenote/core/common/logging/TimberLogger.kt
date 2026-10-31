package com.hossam.carenote.core.common.logging

import timber.log.Timber
import javax.inject.Inject

/** Sets the tag explicitly; Timber's automatic tag would always be this class. */
internal class TimberLogger @Inject constructor() : Logger {
    override fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    override fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).w(throwable, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Timber.tag(tag).e(throwable, message)
    }
}
