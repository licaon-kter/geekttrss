/*
 * Geekttrss is a RSS feed reader application on the Android Platform.
 *
 * Copyright (C) 2017-2020 by Frederic-Charles Barthelery.
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
import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.geekorum.ttrss.accounts.AndroidTinyrssAccountManager
import com.geekorum.ttrss.accounts.NetworkLoginModule
import com.geekorum.ttrss.accounts.PerAccount
import com.geekorum.ttrss.accounts.ServerInformation
import com.geekorum.ttrss.accounts.TinyrssAccountTokenRetriever
import com.geekorum.ttrss.core.ActualCoroutineDispatchersModule
import com.geekorum.ttrss.core.CoroutineDispatchersProvider
import com.geekorum.ttrss.data.AccountInfoDao
import com.geekorum.ttrss.data.Article
import com.geekorum.ttrss.data.ArticleDao
import com.geekorum.ttrss.data.ArticlesDatabase
import com.geekorum.ttrss.data.ArticlesDatabaseModule
import com.geekorum.ttrss.data.FeedsDao
import com.geekorum.ttrss.data.SynchronizationDao
import com.geekorum.ttrss.data.Transaction
import com.geekorum.ttrss.data.TransactionsDao
import com.geekorum.ttrss.data.migrations.ALL_MIGRATIONS
import com.geekorum.ttrss.network.ApiService
import com.geekorum.ttrss.network.TinyrssApiModule
import com.geekorum.ttrss.providers.ArticlesContract.Transaction.Field
import com.geekorum.ttrss.providers.ArticlesContract.Transaction.Field.STARRED
import com.geekorum.ttrss.providers.ArticlesContract.Transaction.Field.UNREAD
import com.geekorum.ttrss.providers.PurgeArticlesDao
import com.geekorum.ttrss.sync.DatabaseAccessModule
import com.geekorum.ttrss.sync.DatabaseService
import com.geekorum.ttrss.sync.FeedIconSynchronizer
import com.geekorum.ttrss.webapi.LoggedRequestInterceptorFactory
import com.geekorum.ttrss.webapi.TokenRetriever
import com.google.common.truth.Truth.assertThat
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(ActualCoroutineDispatchersModule::class, WorkersModule::class, DatabaseAccessModule::class)
class SendTransactionsWorkerTest {
    private lateinit var workerBuilder: TestListenableWorkerBuilder<SendTransactionsWorker>
    private lateinit var apiService: MyMockApiService
    private lateinit var databaseService: MockDatabaseService
    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    @JvmField
    @BindValue
    val dispatchers = CoroutineDispatchersProvider(main = testCoroutineDispatcher,
        io = testCoroutineDispatcher,
        computation = testCoroutineDispatcher)

    @Module(subcomponents = [FakeSyncWorkerComponent::class])
    @InstallIn(ApplicationComponent::class)
    abstract class FakeWorkersModule {
        @Binds
        abstract fun bindsSyncWorkerComponentBuilder(builder: FakeSyncWorkerComponent.Builder): SyncWorkerComponent.Builder
    }

    @Module
    @InstallIn(ApplicationComponent::class)
    inner class MockModule {
        @Provides
        fun providesApiService(): ApiService = apiService
        @Provides
        fun providesDatabaseService(): DatabaseService = databaseService
    }

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BeforeTest
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testCoroutineDispatcher)

        val applicationContext: Context = ApplicationProvider.getApplicationContext()
        apiService = MyMockApiService()
        databaseService = MockDatabaseService()
        workerBuilder = TestListenableWorkerBuilder(applicationContext)
        val inputData = workDataOf(
            SyncWorkerFactory.PARAM_ACCOUNT_NAME to "account.name",
            SyncWorkerFactory.PARAM_ACCOUNT_TYPE to AndroidTinyrssAccountManager.ACCOUNT_TYPE
        )
        workerBuilder.setInputData(inputData)
        workerBuilder.setWorkerFactory(hiltWorkerFactory)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
    }


    @Test
    fun testTransactionAreSendAndRemovedWhenRunningWorker() = testCoroutineDispatcher.runBlockingTest {
        // insert some transactions On Article 1 and 2
        databaseService.insertArticles(listOf(
                Article(id = 1, isUnread = false),
                Article(id = 2, isUnread = true, isStarred = true)))
        databaseService.insertTransaction(
                Transaction(id = 1, articleId = 1, field = UNREAD.toString(), value = true))
        databaseService.insertTransaction(
                Transaction(id = 2, articleId = 2, field = STARRED.toString(), value = false))
        assertThat(databaseService.getTransactions()).hasSize(2)

        val worker = workerBuilder.build()
        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        assertThat(databaseService.getTransactions()).isEmpty()
        val article1 = databaseService.getArticle(1)
        assertThat(article1).isEqualTo(Article(id = 1, isUnread = true, isTransientUnread = true))
        val article2 = databaseService.getArticle(2)
        assertThat(article2).isEqualTo(Article(id = 2, isUnread = true, isStarred = false))
        assertThat(apiService.called).isEqualTo(2)
    }

    private class MyMockApiService : MockApiService() {
        internal var called = 0
        override suspend fun updateArticleField(id: Long, field: Field, value: Boolean) {
            called++
        }
    }
}


@Subcomponent(modules = [
    FakeNetworkLoginModule::class
])
interface FakeSyncWorkerComponent : SyncWorkerComponent {
    @Subcomponent.Builder
    interface Builder : SyncWorkerComponent.Builder
}

@Module
object FakeNetworkLoginModule {

    @Provides
    fun providesServerInformation(accountManager: AndroidTinyrssAccountManager, account: Account): ServerInformation {
        return object: ServerInformation() {
            override val apiUrl: String = "https://test.exemple.com/"
            override val basicHttpAuthUsername: String? = null
            override val basicHttpAuthPassword: String? = null
        }
    }

}
