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
package com.geekorum.ttrss.background_job

import android.accounts.Account
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ContentResolver
import android.content.Context
import android.content.SyncRequest
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.geekorum.ttrss.providers.ArticlesContract
import com.geekorum.ttrss.providers.PurgeArticlesWorker
import com.geekorum.ttrss.sync.workers.CollectNewArticlesWorker
import com.geekorum.ttrss.sync.workers.SyncWorkerFactory
import com.geekorum.ttrss.sync.workers.UpdateArticleStatusWorker
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Manage the different background jobs submitted to the JobScheduler class
 */
class BackgroundJobManager @Inject constructor(
    application: Application
) {
    private val impl: BackgroundJobManagerImpl =
        BackgroundJobManagerNougatImpl(application)

    fun refresh(account: Account) {
        impl.refresh(account)
    }

    /**
     * Refresh a feed.
     * @return workmanager job uuid
     */
    suspend fun refreshFeed(account: Account, feedId: Long): UUID {
        return impl.refreshFeed(account, feedId)
    }

    fun isRefreshingStatus(jobId: UUID): LiveData<Boolean> {
        return impl.isRefreshingStatus(jobId)
    }

    fun setupPeriodicJobs() {
        setupPeriodicPurge()
    }

    private fun setupPeriodicPurge() {
        impl.setupPeriodicPurge()
    }

    companion object {
        const val PERIODIC_PURGE_JOB_ID = 3
        const val PERIODIC_PURGE_JOB = "periodic_purge"

        val PERIODIC_PURGE_JOB_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(1)
        val PERIODIC_REFRESH_JOB_INTERVAL_S = TimeUnit.HOURS.toSeconds(2)
        val PERIODIC_FULL_REFRESH_JOB_INTERVAL_S = TimeUnit.DAYS.toSeconds(1)
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private class BackgroundJobManagerNougatImpl(
    context: Context
) : BackgroundJobManagerImpl(context){

    private val jobScheduler: JobScheduler = context.getSystemService()!!

    override fun setupPeriodicPurge() {
        // don't reschedule the job if it is already pending or running.
        // reschedule a job will stop a current running job and reset the timers
        val pendingJob = getPendingJob(
            BackgroundJobManager.PERIODIC_PURGE_JOB_ID)
        if (pendingJob != null) {
            Timber.i("Cancel periodic purge job to replace it with WorkManager implementation")
            jobScheduler.cancel(BackgroundJobManager.PERIODIC_PURGE_JOB_ID)
        }
        super.setupPeriodicPurge()
    }

    fun getPendingJob(id: Int): JobInfo? {
        return jobScheduler.getPendingJob(id)
    }
}


private open class BackgroundJobManagerImpl internal constructor(
    protected var context: Context
) {

    fun refresh(account: Account) {
        val extras = Bundle()
        requestSync(account, extras)
    }

    suspend fun refreshFeed(account: Account, feedId: Long): UUID {
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        var inputData = workDataOf(
                SyncWorkerFactory.PARAM_ACCOUNT_NAME to account.name,
                SyncWorkerFactory.PARAM_ACCOUNT_TYPE to account.type,
                CollectNewArticlesWorker.PARAM_FEED_ID to feedId
        )
        val collectNewArticleRequest = OneTimeWorkRequestBuilder<CollectNewArticlesWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()
        inputData = workDataOf(
                SyncWorkerFactory.PARAM_ACCOUNT_NAME to account.name,
                SyncWorkerFactory.PARAM_ACCOUNT_TYPE to account.type,
                UpdateArticleStatusWorker.PARAM_FEED_ID to feedId
        )
        val updateStatusRequest = OneTimeWorkRequestBuilder<UpdateArticleStatusWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()
        WorkManager.getInstance(context)
                .beginWith(collectNewArticleRequest)
                .then(updateStatusRequest)
                .enqueue().await()
        return updateStatusRequest.id
    }

    /**
     * Return a LiveData containing the running status of the job.
     * True if the job is not finished
     * False if the job is finished
     */
    fun isRefreshingStatus(jobId: UUID): LiveData<Boolean> {
        return WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(jobId).map { workInfo: WorkInfo? ->
                    if (workInfo == null) {
                        false
                    } else {
                        !workInfo.state.isFinished
                    }
                }
    }

    private fun requestSync(account: Account, extras: Bundle) {
        val builder = SyncRequest.Builder()
        builder.setSyncAdapter(account, ArticlesContract.AUTHORITY)
            .setManual(true)
            .setExpedited(true)
            .setNoRetry(true)
            .setExtras(extras)
            .syncOnce()
        ContentResolver.requestSync(builder.build())
    }

    open fun setupPeriodicPurge() {
        //TODO only setup on account creation
        val request = PeriodicWorkRequestBuilder<PurgeArticlesWorker>(
            BackgroundJobManager.PERIODIC_PURGE_JOB_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .setRequiresCharging(true)
                .build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                BackgroundJobManager.PERIODIC_PURGE_JOB, ExistingPeriodicWorkPolicy.KEEP, request)
    }

}

