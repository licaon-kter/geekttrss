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
package com.geekorum.build

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project

internal fun Project.configureReleaseSigningConfig() {
    val releaseStoreFile = findProperty("RELEASE_STORE_FILE") as? String ?: ""
    val releaseStorePassword = findProperty("RELEASE_STORE_PASSWORD") as? String ?: ""
    val releaseKeyAlias= findProperty("RELEASE_KEY_ALIAS") as? String ?: ""
    val releaseKeyPassword= findProperty("RELEASE_KEY_PASSWORD") as? String ?: ""

    extensions.configure<ApplicationExtension>("android") {
        signingConfigs {
            register("release") {
                storeFile =  file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }

        buildTypes {
            named("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

