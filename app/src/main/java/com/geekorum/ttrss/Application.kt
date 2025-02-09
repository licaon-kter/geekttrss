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
package com.geekorum.ttrss

import android.app.Activity
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.geekorum.ttrss.di.ApplicationComponentEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import android.app.Application as AndroidApplication

/**
 * Initialize global component for the TTRSS application.
 */
@HiltAndroidApp
open class Application : AndroidApplication(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workManagerConfig: Configuration

    @Inject
    lateinit var imageLoader: ImageLoader

    open val applicationComponent by lazy {
        EntryPointAccessors.fromApplication(this, ApplicationComponentEntryPoint::class.java)
    }

    override fun getWorkManagerConfiguration(): Configuration = workManagerConfig

    override fun newImageLoader(): ImageLoader = imageLoader

}

val Activity.applicationComponent: ApplicationComponentEntryPoint
        get() = (applicationContext as Application).applicationComponent
