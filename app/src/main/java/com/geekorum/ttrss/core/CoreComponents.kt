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

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.FragmentFactory
import com.geekorum.geekdroid.dagger.DaggerDelegateFragmentFactory
import com.geekorum.geekdroid.dagger.FragmentFactoriesModule
import com.geekorum.ttrss.BatteryFriendlyActivity
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject


/**
 * As it supports Dagger injection, the Activity must have a corresponding [AndroidInjector]
 */
@SuppressLint("Registered")
open class InjectableBaseActivity : BatteryFriendlyActivity() {
    @Inject
    lateinit var daggerDelegateFragmentFactory: FragmentFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        inject()
        supportFragmentManager.fragmentFactory = daggerDelegateFragmentFactory
        super.onCreate(savedInstanceState)
    }

    /**
     * Inject required field into the activity.
     * Default method use Dagger Hilt but can be overriden
     */
    protected open fun inject() {}
}

// BaseActivity is flavor dependant but extends InjectableBaseActivity


@Module(includes = [FragmentFactoriesModule::class])
@InstallIn(SingletonComponent::class)
abstract class CoreFactoriesModule {

    @Binds
    abstract fun bindsFragmentFactory(factory: DaggerDelegateFragmentFactory): FragmentFactory
}
