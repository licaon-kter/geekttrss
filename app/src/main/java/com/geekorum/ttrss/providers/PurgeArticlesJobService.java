/**
 * Geekttrss is a RSS feed reader application on the Android Platform.
 *
 * Copyright (C) 2017-2018 by Frederic-Charles Barthelery.
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
package com.geekorum.ttrss.providers;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.os.Build;
import com.geekorum.geekdroid.jobs.JobThread;
import com.geekorum.geekdroid.jobs.ThreadedJobService;
import com.geekorum.ttrss.Application;
import com.geekorum.ttrss.di.ApplicationComponent;

import javax.inject.Inject;

/**
 * JobService to purge old Articles
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PurgeArticlesJobService extends ThreadedJobService {

    @Inject ArticlesProvidersDao articlesProvidersDao;

    @Override
    public void onCreate() {
        super.onCreate();
        createArticleProviderComponent()
                .inject(this);
    }

    private ApplicationComponent getApplicationComponent() {
        return ((Application) getApplication()).getApplicationComponent();
    }

    private ArticleProviderComponent createArticleProviderComponent() {
        return getApplicationComponent().createArticleProviderComponent()
                .bindAndroidComponent(this)
                .build();
    }

    @Override
    protected JobThread createJobThread(JobParameters jobParameters) {
        return new ArticlePurgerThread(this, jobParameters, articlesProvidersDao);
    }

}
