/*
 * Geekttrss is a RSS feed reader application on the Android Platform.
 *
 * Copyright (C) 2017-2023 by Frederic-Charles Barthelery.
 *
 * This file is part of Geekttrss.
 *
 * Geekttrss is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Geekttrss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Geekttrss.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.geekorum.ttrss.network

import com.geekorum.ttrss.BuildConfig
import com.geekorum.ttrss.accounts.ServerInformation
import com.geekorum.ttrss.webapi.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.*

/**
 * This module provides an [ApiService] to access a TinyRss server
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TinyrssApiModule {
    @BindsOptionalOf
    abstract fun bindLoggedRequestInterceptorFactory(): LoggedRequestInterceptorFactory

    companion object {

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = !BuildConfig.DEBUG
        }

        @Provides
        fun provideApiService(tokenRetriever: TokenRetriever, tinyRssApi: TinyRssApi, serverInformation: ServerInformation): ApiService {
            return ApiRetrofitService(tokenRetriever, tinyRssApi, serverInformation)
        }

        @Provides
        fun providesTinyRssApi(okHttpClient: OkHttpClient, serverInformation: ServerInformation, loggedRequestInterceptorFactory: Optional<LoggedRequestInterceptorFactory>): TinyRssApi {
            var tinyrssApiUrl = serverInformation.apiUrl
            if (!tinyrssApiUrl.endsWith("/")) {
                tinyrssApiUrl += "/"
            }
            val retrofitBuilder = Retrofit.Builder()
                .baseUrl(tinyrssApiUrl)
            loggedRequestInterceptorFactory.ifPresent { retrofitBuilder.addConverterFactory(it) }

            var okHttpClient = okHttpClient
            val basicHttpAuthPassword = serverInformation.basicHttpAuthPassword
            val basicHttpAuthUsername = serverInformation.basicHttpAuthUsername
            if (basicHttpAuthPassword != null && basicHttpAuthUsername != null) {
                val basicAuthInterceptor = BasicAuthInterceptor(basicHttpAuthUsername, basicHttpAuthPassword)
                okHttpClient = okHttpClient.newBuilder().addInterceptor(basicAuthInterceptor).build()
            }
            val jsonConverterFactory = json.asConverterFactory("application/json".toMediaType())
            retrofitBuilder.addConverterFactory(jsonConverterFactory)
                .client(okHttpClient)
            val retrofit = retrofitBuilder.build()
            return retrofit.create(TinyRssApi::class.java)
        }
    }
}
