package com.hossam.carenote.core.speech

import kotlinx.coroutines.flow.StateFlow

/** Which speech engine the app is using, for diagnostics such as the debug menu. Tells nothing about the engines. */
interface ActiveSpeechEngine {
    /** Name of the engine the last recording used; null before the first one or when none was ready. */
    val activeEngine: StateFlow<String?>
}
