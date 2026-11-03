package com.hossam.carenote.di

import android.os.Build
import com.hossam.carenote.BuildConfig
import com.hossam.carenote.core.network.ClientInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** What the app knows about itself, the same in every flavor. What differs per flavor is in EnvironmentModule. */
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    /** E.g. `CareNote/1.0 (Android 15; Pixel 8)`; mock builds say `1.0-mock`. */
    @Provides
    fun provideClientInfo(): ClientInfo = ClientInfo(
        userAgent = "CareNote/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})",
    )
}
