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
package com.geekorum.ttrss.webapi

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An interceptor that add basic HTTP Authentication request headers
 *
 */
class BasicAuthInterceptor(
    private val username: String = "",
    private val password: String = ""
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val credentials = Credentials.basic(
            username,
            password)

        val authenticatedRequest = chain.request().newBuilder()
            .header("Authorization", credentials)
            .build()
        return chain.proceed(authenticatedRequest)
    }
}