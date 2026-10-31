package com.hossam.carenote.core.common.coroutines

import javax.inject.Qualifier

enum class CareNoteDispatchers {
    Default,
    IO,
}

/** Injected instead of `Dispatchers.*`, so tests can swap in a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: CareNoteDispatchers)
