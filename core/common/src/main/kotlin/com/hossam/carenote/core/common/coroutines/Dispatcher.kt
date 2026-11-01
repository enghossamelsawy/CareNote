package com.hossam.carenote.core.common.coroutines

import javax.inject.Qualifier

enum class CareNoteDispatchers {
    Default,
    IO,

    /** The main thread, for Android APIs that must be called on it. */
    Main,
}

/** Injected instead of `Dispatchers.*`, so tests can swap in a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: CareNoteDispatchers)
