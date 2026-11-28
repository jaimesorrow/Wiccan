package com.jaimesorrow.wiccan.ads

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {
    @Binds
    abstract fun bindAdConsentController(impl: UmpAdConsentController): AdConsentController
}
