package com.hossam.carenote.core.network.mock.di

import com.hossam.carenote.core.network.di.ExtraInterceptors
import com.hossam.carenote.core.network.mock.MockServerInterceptor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor

/** Plugs the mock server into core:network's interceptor set; core:network itself never knows. */
@Module
@InstallIn(SingletonComponent::class)
internal interface MockNetworkModule {
    @Binds
    @IntoSet
    @ExtraInterceptors
    fun bindMockServerInterceptor(impl: MockServerInterceptor): Interceptor
}
