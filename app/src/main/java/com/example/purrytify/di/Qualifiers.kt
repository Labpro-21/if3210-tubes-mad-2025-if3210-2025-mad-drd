package com.example.purrytify.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenDataStoreQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserPreferencesDataStoreQualifier