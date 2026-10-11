package com.stitten.stitteniptv.di

import com.stitten.stitteniptv.data.network.StreamUrlBuilder
import com.stitten.stitteniptv.data.network.XtreamCredentials
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamModule {

    @Provides
    @Singleton
    fun provideStreamUrlBuilder(): StreamUrlBuilder {
        // عدّل هذه القيم حسب طريقة تخزين بيانات الجلسة لديك
        return StreamUrlBuilder(
            XtreamCredentials(
                baseUrl = "",
                username = "",
                password = ""
            )
        )
    }
}
