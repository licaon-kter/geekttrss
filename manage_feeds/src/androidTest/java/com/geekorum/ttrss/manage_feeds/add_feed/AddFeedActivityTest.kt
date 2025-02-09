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
package com.geekorum.ttrss.manage_feeds.add_feed

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.geekorum.ttrss.manage_feeds.R
import com.google.common.truth.Truth.assertWithMessage
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import com.geekorum.geekdroid.R as geekdroidR

@RunWith(AndroidJUnit4::class)
class AddFeedActivityTest {

    lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        // workaround ssl error on jdk 11
        // https://github.com/robolectric/robolectric/issues/5115
//        System.setProperty("javax.net.ssl.trustStoreType", "JKS")
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testThatWeCanLaunchTheActivity() {
        val response = MockResponse().apply {
            setHeader("Content-Type", "application/rss+xml")
            setResponseCode(200)
        }
        server.enqueue(response)
        val url = server.url("/feeds.xml").toString().toUri()
        val intent = Intent(null, url, getApplicationContext(), AddFeedActivity::class.java)

        val scenario: ActivityScenario<AddFeedActivity> = launchActivity(intent)
        scenario.use {
            // remember the activity. We should not but we are explicitely testing that it is finishing
            lateinit var currentActivity: Activity
            scenario.onActivity {
                currentActivity = it
            }

            // Check that the toolbar is there
            onView(withId(R.id.toolbar))
                .check(matches(isCompletelyDisplayed()))

            // click outside
            onView(withId(geekdroidR.id.touch_outside))
                .perform(click())

            assertWithMessage("The activity should finish on outside touch")
                .that(currentActivity.isFinishing).isTrue()
        }
    }
}
