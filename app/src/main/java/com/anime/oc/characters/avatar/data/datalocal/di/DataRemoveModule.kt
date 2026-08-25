package com.anime.oc.characters.avatar.data.datalocal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

//    @Provides
//    @Singleton
//    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
//        .baseUrl(ApiConfig.BASE_URL)
//        .client(client)
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//
//    @Provides
//    @Singleton
//    fun provideAvatarApiService(retrofit: Retrofit): AvatarApiService =
//        retrofit.create(AvatarApiService::class.java)
}