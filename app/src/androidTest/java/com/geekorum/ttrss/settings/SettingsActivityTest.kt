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
package com.geekorum.ttrss.settings

import android.app.Application
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.truth.content.IntentSubject.assertThat
import com.geekorum.ttrss.R
import com.geekorum.ttrss.settings.licenses.OpenSourceLicensesActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.Test
import androidx.preference.R as prefR

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SettingsActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(SettingsActivity::class.java)

    @Test
    fun testThatClickOnOpenSourceLicensesOpensOpenSourcesLicensesActivity() {
        // click on OSS licenses
        onView(withId(prefR.id.recycler_view))
            .perform(
                actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(R.string.pref_title_oss_license)),
                    click()
                )
            )

        val receivedIntent = Intents.getIntents().single()
        assertThat(receivedIntent).hasComponentClass(OpenSourceLicensesActivity::class.java)
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        assertThat(receivedIntent).hasComponentPackage(applicationContext.packageName)
    }
}
