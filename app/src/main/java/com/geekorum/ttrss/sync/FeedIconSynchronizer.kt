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
package com.geekorum.ttrss.sync

import android.security.NetworkSecurityPolicy
import androidx.core.net.toUri
import com.geekorum.favikonsnoop.AdaptiveDimension
import com.geekorum.favikonsnoop.FaviKonSnoop
import com.geekorum.favikonsnoop.FaviconInfo
import com.geekorum.favikonsnoop.FixedDimension
import com.geekorum.ttrss.core.CoroutineDispatchersProvider
import com.geekorum.ttrss.data.Feed
import com.geekorum.ttrss.network.ApiService
import com.geekorum.ttrss.network.ServerInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val NB_LOOKUP_COROUTINES = 5

/**
 * Update icon's url for every feed.
 */
class FeedIconSynchronizer @AssistedInject constructor(
    private val dispatchers: CoroutineDispatchersProvider,
    private val databaseService: DatabaseService,
    private val apiService: ApiService,
    private val faviKonSnoop: FaviKonSnoop,
    private val okHttpClient: OkHttpClient,
    private val httpCacher: HttpCacher,
    private val networkSecurityPolicy: NetworkSecurityPolicy,
    @Assisted private val feedIconApiDownloader: FeedIconApiDownloader
) {

    @AssistedFactory
    interface Factory {
        fun create(feedIconApiDownloader: FeedIconApiDownloader): FeedIconSynchronizer
    }

    private var serverInfo: ServerInfo? = null
    private val serverInfoLock = Mutex()

    suspend fun synchronizeFeedIcons() {
        coroutineScope {
            val feedChannel = Channel<Feed>()
            databaseService.getFeeds().asFlow()
                .onEach { feedChannel.send(it) }
                .onCompletion { feedChannel.close() }
                .launchIn(this)

            dispatchUpdateFeedIcons(feedChannel)
        }

        // now update cache for http urls
        databaseService.getFeedFavIcons()
            .mapNotNull {
                it.url.toHttpUrlOrNull()
            }.forEach {
                httpCacher.cacheHttpRequest(it)
            }
    }

    private fun CoroutineScope.dispatchUpdateFeedIcons(feedChannel: Channel<Feed>) {
        repeat(NB_LOOKUP_COROUTINES) {
            launch(dispatchers.io) {
                for (feed in feedChannel) {
                    try {
                        updateFeedIcon(feed)
                    } catch (e: Exception) {
                        Timber.w(e, "Unable to update feed icon for feed ${feed.title}")
                    }
                }
            }
        }
    }

    private suspend fun updateFeedIcon(feed: Feed) {
        // TODO this doesn't really work with planet or aggregator
        // we should read the feed xml to get the feed's website and then the icon
        val article = databaseService.getLatestArticleFromFeed(feed.id)
        val articleUrl = (article?.link ?: feed.url).toHttpUrlOrNull()

        val favIconInfos = articleUrl?.newBuilder()?.encodedPath("/")?.build()?.let { url ->
            findFavicons(url)
        } ?: emptyList()

        val selectedIcon = selectBestIcon(favIconInfos)

        if (selectedIcon != null) {
            databaseService.updateFeedIconUrl(feed.id, selectedIcon.url.toString())
            return
        }

        // download feed icon from api
        var iconUrl = downloadFeedIcon(feed)
        if (iconUrl != null ) {
            databaseService.updateFeedIconUrl(feed.id, iconUrl)
            return
        }

        // fallback to icon from config
        iconUrl = getIconUrlFromServer(feed)
        if (iconUrl != null ) {
            databaseService.updateFeedIconUrl(feed.id, iconUrl)
        }
    }

    private suspend fun downloadFeedIcon(feed: Feed): String? {
        val serverInfo = getServerInfo()
        val apiLevel = serverInfo.apiLevel ?: 0
        if (apiLevel < 19) return null

        val feedFile = feedIconApiDownloader.downloadFeedIcon(feed)
        return feedFile.toUri().toString()
    }

    private suspend fun getServerInfo(): ServerInfo {
        return serverInfoLock.withLock {
            // fetch again if we didn't get feedsIconsUrl
            if (serverInfo?.feedsIconsUrl == null) {
                serverInfo = apiService.getServerInfo()
            }
            serverInfo!!
        }
    }

    private suspend fun getIconUrlFromServer(feed: Feed): String? {
        val serverInfo = getServerInfo()
        val apiLevel = serverInfo.apiLevel ?: 0
        if (apiLevel >= 19) return null

        val baseFeedsIconsUrl = serverInfo.feedsIconsUrl?.let {
            "${serverInfo.apiUrl}${serverInfo.feedsIconsUrl}"
        } ?: return null
        return "$baseFeedsIconsUrl/${feed.id}.ico"
    }

    private suspend fun findFavicons(url: HttpUrl) : Collection<FaviconInfo> {
        val httpsUrl = url.newBuilder().scheme("https").build()
        try {
            return faviKonSnoop.findFavicons(httpsUrl)
        } catch (e: Exception) {
            Timber.w(e, "Unable to find favicons for $httpsUrl")
        }
        // fallback to http
        if (networkSecurityPolicy.isCleartextTrafficPermitted(url.host) && !url.isHttps) {
            try {
                return faviKonSnoop.findFavicons(httpsUrl)
            } catch (e: Exception) {
                Timber.w(e, "Unable to find favicons for $url")
            }
        }
        return emptyList()
    }

    private suspend fun selectBestIcon(favIconInfos: Collection<FaviconInfo>): FaviconInfo? {
        val sortedIcons = favIconInfos.sortedByDescending {
            when (val dimension = it.dimension) {
                is AdaptiveDimension -> Int.MAX_VALUE
                is FixedDimension -> dimension.height * dimension.width
                null -> Int.MIN_VALUE
            }
        }.filter {
            it.url.isHttps || networkSecurityPolicy.isCleartextTrafficPermitted(it.url.host)
        }

        return sortedIcons.firstOrNull {
            try {
                val request = Request.Builder()
                    .head()
                    .url(it.url)
                    .build()
                okHttpClient.newCall(request).suspendExecute().use { resp ->
                    resp.isSuccessful
                }
            } catch (e: IOException) {
                Timber.w(e, "Unable to get feed icon")
                false
            }
        }
    }


    private suspend fun Call.suspendExecute(): Response {
        return suspendCancellableCoroutine { cont ->
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response)
                }
            })
            cont.invokeOnCancellation { cancel() }
        }
    }

}
