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
package com.geekorum.ttrss.manage_feeds.workers

import androidx.annotation.Keep
import androidx.work.WorkerFactory
import com.geekorum.ttrss.di.ApplicationComponentEntryPoint
import com.geekorum.ttrss.features_api.WorkerFactoryProvider
import com.geekorum.ttrss.manage_feeds.DaggerManageFeedComponent

@Keep
class ManageFeedWorkersProvider : WorkerFactoryProvider {
    override fun getWorkerFactories(appComponent: ApplicationComponentEntryPoint): List<WorkerFactory> {
        val manageFeedComponent = DaggerManageFeedComponent.builder()
            .manageFeedsDependencies(appComponent)
            .build()
        return listOf(manageFeedComponent.getWorkerFactory())
    }
}
