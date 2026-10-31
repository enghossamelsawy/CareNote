package com.hossam.carenote.core.common.coroutines

import javax.inject.Qualifier

/** A scope that lives as long as the app, for work that must outlive any screen. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
