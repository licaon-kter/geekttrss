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
package com.geekorum.ttrss.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Provider for main [CoroutineDispatcher]s.
 * Allow CoroutineDispatchers to be injected.
 */
data class CoroutineDispatchersProvider(
        val main: CoroutineDispatcher,
        val io: CoroutineDispatcher,
        val computation: CoroutineDispatcher
)

@Module
@InstallIn(SingletonComponent::class)
object ActualCoroutineDispatchersModule {

    @Provides
    @Singleton
    fun providesCoroutineDispatchersProvider(): CoroutineDispatchersProvider =
            CoroutineDispatchersProvider(
                    Dispatchers.Main,
                    Dispatchers.IO,
                    Dispatchers.Default
            )
}
