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

import android.accounts.Account
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.geekorum.ttrss.webapi.ApiCallException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker to unsubscribe from a feed.
 */
@HiltWorker
class UnsubscribeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted private val params: WorkerParameters,
    workerComponentBuilder: WorkerComponent.Builder
) : BaseManageFeedWorker(appContext, params, workerComponentBuilder) {

    private val apiService: ManageFeedService = workerComponent.getManageFeedService()

    override suspend fun doWork(): Result {
        val feedId = params.inputData.getLong("feed_id", -1)
        if (feedId == -1L) {
            return Result.failure()
        }
        return try {
            val success = apiService.unsubscribeFromFeed(feedId)
            if (success) {
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: ApiCallException) {
            // TODO check when we can retry or not
            Result.retry()
        }
    }

    companion object {
        fun getInputData(account: Account,
                         feedId: Long): Data {
            return workDataOf(
                "account_name" to account.name,
                "account_type" to account.type,
                "feed_id" to feedId
            )
        }
    }

}


