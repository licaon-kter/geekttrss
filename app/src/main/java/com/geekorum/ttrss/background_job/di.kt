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
package com.geekorum.ttrss.background_job

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import androidx.work.WorkerFactory
import com.geekorum.geekdroid.dagger.AppInitializersModule
import com.geekorum.ttrss.features_api.FeaturesWorkerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module(includes = [AppInitializersModule::class])
@InstallIn(SingletonComponent::class)
abstract class BackgroundJobsModule {

    companion object {

        @Provides
        fun provideWorkManagerConfiguration(workerFactory: WorkerFactory): Configuration {
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

        @Provides
        internal fun providesApplicationWorkerFactory(
            hiltWorkerFactory: HiltWorkerFactory,
            featuresWorkerFactory: FeaturesWorkerFactory
        ): WorkerFactory {
            return DelegatingWorkerFactory().apply {
                addFactory(hiltWorkerFactory)
                addFactory(featuresWorkerFactory)
            }
        }

    }
}
