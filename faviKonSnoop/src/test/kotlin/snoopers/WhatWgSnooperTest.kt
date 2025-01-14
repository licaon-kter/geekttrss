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
package com.geekorum.favikonsnoop.snoopers

import com.geekorum.favikonsnoop.AdaptiveDimension
import com.geekorum.favikonsnoop.FaviconInfo
import com.geekorum.favikonsnoop.FixedDimension
import com.geekorum.favikonsnoop.source
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private val MANY_HTML =
    """
    <html lang="en">
     <head>
      <title>lsForums — Inbox</title>
      <link rel=icon href=favicon.png sizes="16x16" type="image/png">
      <link rel=icon href=windows.ico sizes="32x32 48x48" type="image/vnd.microsoft.icon">
      <link rel=icon href=mac.icns sizes="128x128 512x512 8192x8192 32768x32768">
      <link rel=icon href=iphone.png sizes="57x57" type="image/png">
      <link rel=icon href=gnome.svg sizes="any" type="image/svg+xml">
      <link rel=stylesheet href=lsforums.css>
      <script src=lsforums.js></script>
      <meta name=application-name content="lsForums">
     </head>
    </html>
    """.trimIndent()

private val INVALID_HTML =
    """fw""".trimIndent()

class WhatWgSnooperTest {
    lateinit var subject: LinkRelSnooper
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setUp() {
        subject = WhatWgSnooper(testDispatcher)
    }

    @Test
    fun testInvalidReturnsFallbackFavicon() = testScope.runTest {
        val result = INVALID_HTML.source().use {
            subject.snoop("http://exemple.com/crazy/story.html", it)
        }

        assertThat(result).containsExactly(FaviconInfo("http://exemple.com/favicon.ico"))
    }

    @Test
    fun testManyLinkReturnsCorrectResult() = testScope.runTest {
        val result = MANY_HTML.source().use {
            subject.snoop("http://exemple.com", it)
        }

        assertThat(result).containsExactly(
            FaviconInfo("http://exemple.com/favicon.ico"),
            FaviconInfo("http://exemple.com/favicon.png",
                mimeType = "image/png",
                dimension = FixedDimension(16, 16)
            ),
            // mac
            FaviconInfo("http://exemple.com/mac.icns",
                dimension = FixedDimension(128, 128)
            ),
            FaviconInfo("http://exemple.com/mac.icns",
                dimension = FixedDimension(512, 512)
            ),
            FaviconInfo("http://exemple.com/mac.icns",
                dimension = FixedDimension(8192, 8192)
            ),
            FaviconInfo("http://exemple.com/mac.icns",
                dimension = FixedDimension(32768, 32768)
            ),
            // windo2s
            FaviconInfo("http://exemple.com/windows.ico",
                dimension = FixedDimension(32, 32),
                mimeType = "image/vnd.microsoft.icon"
            ),
            FaviconInfo("http://exemple.com/windows.ico",
                dimension = FixedDimension(48, 48),
                mimeType = "image/vnd.microsoft.icon"
            ),
            // iphone
            FaviconInfo("http://exemple.com/iphone.png",
                mimeType = "image/png",
                dimension = FixedDimension(57,57)
            ),
            // gnome
            FaviconInfo("http://exemple.com/gnome.svg",
                mimeType = "image/svg+xml",
                dimension = AdaptiveDimension
            )
        )
    }

}
