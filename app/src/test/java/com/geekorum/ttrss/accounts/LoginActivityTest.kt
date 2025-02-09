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
package com.geekorum.ttrss.accounts

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geekorum.ttrss.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
@UninstallModules(AndroidTinyrssAccountManagerModule::class)
class LoginActivityTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val composeTestRule = AndroidComposeTestRule(
        activityRule = ActivityScenarioRule(Intent(LoginActivity.ACTION_ADD_ACCOUNT).apply {
            setClass(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
        }),
        activityProvider = ::getActivityFromTestRule
    )

    private fun <A : ComponentActivity> getActivityFromTestRule(rule: ActivityScenarioRule<A>): A {
        var activity: A? = null
        rule.scenario.onActivity { activity = it }
        if (activity == null) {
            throw IllegalStateException("Activity was not set in the ActivityScenarioRule!")
        }
        return activity!!
    }

    @Test
    @Config(qualifiers = "w800dp")
    fun testThatWeCanStartTheActivityOnW800Dp() {
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.action_sign_in))
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("contentCard").assertIsDisplayed()
    }

    @Test
    fun testThatWeCanStartTheActivity() {
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.action_sign_in))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    internal object MocksModule {
        @Provides
        fun providesAndroidTinyrssAccountManager(): AndroidTinyrssAccountManager = mockk()

        @Provides
        fun providesTinyrssAccountManager(androidTinyrssAccountManager: AndroidTinyrssAccountManager): TinyrssAccountManager {
            return androidTinyrssAccountManager
        }
    }

}

