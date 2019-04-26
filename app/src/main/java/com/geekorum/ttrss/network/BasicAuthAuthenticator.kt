/*
 * Geekttrss is a RSS feed reader application on the Android Platform.
 *
 * Copyright (C) 2017-2018 by Frederic-Charles Barthelery.
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

import com.geekorum.ttrss.accounts.ServerInformation
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * A basic HTTP Authenticator
 */
class BasicAuthAuthenticator(
    private val serverInformation: ServerInformation
): Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request().header("Authorization") != null) {
            return null
        }

        val credentials = Credentials.basic(
            serverInformation.basicHttpAuthUsername ?: "",
            serverInformation.basicHttpAuthPassword ?: "")
        return response.request().newBuilder()
            .header("Authorization", credentials)
            .build()
    }
}