/*
 * Geekttrss is a RSS feed reader application on the Android Platform.
 *
 * Copyright (C) 2017-2019 by Frederic-Charles Barthelery.
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
package com.geekorum.ttrss.sync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.geekorum.ttrss.core.CoroutineDispatchersProvider
import com.geekorum.ttrss.data.Category
import com.geekorum.ttrss.network.ApiService
import com.geekorum.ttrss.sync.DatabaseService
import com.geekorum.ttrss.webapi.ApiCallException
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Synchronize feeds list and categories.
 */
class SyncFeedsWorker(
        context: Context,
        workerParams: WorkerParameters,
        private val dispatchers: CoroutineDispatchersProvider,
        private val apiService: ApiService,
        private val databaseService: DatabaseService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(dispatchers.io) {
        try {
            synchronizeFeeds()
            Result.success()
        } catch (e: ApiCallException) {
            Timber.w(e, "unable to sync feeds list")
            Result.failure()
        }
    }

    @Throws(ApiCallException::class)
    private suspend fun synchronizeFeeds() {
        Timber.i("Synchronizing feeds list")
        val categories = apiService.getCategories()
        val feeds = apiService.getFeeds()
        databaseService.runInTransaction {
            insertCategories(categories)
            deleteOldCategories(categories)
            insertFeeds(feeds)
            deleteOldFeeds(feeds)
        }
    }

    private suspend fun deleteOldFeeds(feeds: List<com.geekorum.ttrss.data.Feed>) {
        val feedsIds: List<Long> = feeds.map { it.id }
        val toBeDelete = databaseService.getFeeds().filter { it.id !in feedsIds }

        databaseService.deleteFeedsAndArticles(toBeDelete)
    }

    private suspend fun deleteOldCategories(categories: List<Category>) {
        val feedCategoriesId: List<Long> = categories.map { category -> category.id }
        val toDelete = databaseService.getCategories().filter { it.id !in feedCategoriesId }

        databaseService.deleteCategories(toDelete)
    }

    private suspend fun insertFeeds(feeds: List<com.geekorum.ttrss.data.Feed>) {
        databaseService.insertFeeds(feeds)
    }

    private suspend fun insertCategories(categories: List<Category>) {
        // remove virtual categories
        val realCategories = categories.filter { it.id >= 0 }
        databaseService.insertCategories(realCategories)
    }


    class WorkerFactory @Inject constructor(
            syncWorkerComponentBuilder: SyncWorkerComponent.Builder
    ) : SyncWorkerFactory(syncWorkerComponentBuilder) {

        override fun createWorker(
                appContext: Context, workerClassName: String, workerParameters: WorkerParameters
        ): ListenableWorker? {
            if (workerClassName != SyncFeedsWorker::class.java.name) {
                return null
            }

            val syncWorkerComponent = createSyncWorkerComponent(workerParameters)
            return with(syncWorkerComponent) {
                SyncFeedsWorker(appContext, workerParameters,
                        dispatchers,
                        apiService,
                        databaseService)
            }
        }
    }
}
