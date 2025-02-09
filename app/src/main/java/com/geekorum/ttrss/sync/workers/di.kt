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
package com.geekorum.ttrss.sync.workers

import android.accounts.Account
import com.geekorum.favikonsnoop.FaviKonSnoop
import com.geekorum.favikonsnoop.snoopers.AppManifestSnooper
import com.geekorum.favikonsnoop.snoopers.AppleTouchIconSnooper
import com.geekorum.favikonsnoop.snoopers.WhatWgSnooper
import com.geekorum.ttrss.accounts.NetworkLoginModule
import com.geekorum.ttrss.accounts.PerAccount
import com.geekorum.ttrss.accounts.ServerInformation
import com.geekorum.ttrss.core.CoroutineDispatchersProvider
import com.geekorum.ttrss.network.ApiService
import com.geekorum.ttrss.sync.DatabaseService
import com.geekorum.ttrss.sync.FeedIconApiDownloader
import com.geekorum.ttrss.sync.FeedIconSynchronizer
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient


@Module(subcomponents = [SyncWorkerComponent::class])
@InstallIn(SingletonComponent::class)
abstract class WorkersModule

@Subcomponent(modules = [
    NetworkLoginModule::class
])
@PerAccount
interface SyncWorkerComponent {

    val account: Account
    val apiService: ApiService
    val serverInformation: ServerInformation
    val databaseService: DatabaseService
    val feedIconSynchronizerFactory: FeedIconSynchronizer.Factory
    val feedIconApiDownloaderFactory: FeedIconApiDownloader.Factory

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun seedAccount(account: Account): Builder

        fun build(): SyncWorkerComponent
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object FaviKonModule {

    @Provides
    fun providesFaviKonSnoop(okHttpClient: OkHttpClient, coroutineDispatchersProvider: CoroutineDispatchersProvider): FaviKonSnoop {
        val snoopers = listOf(
                AppManifestSnooper(coroutineDispatchersProvider.io),
                WhatWgSnooper(coroutineDispatchersProvider.io),
                AppleTouchIconSnooper(coroutineDispatchersProvider.io))
        return FaviKonSnoop(snoopers, okHttpClient, coroutineDispatchersProvider.io)
    }
}
