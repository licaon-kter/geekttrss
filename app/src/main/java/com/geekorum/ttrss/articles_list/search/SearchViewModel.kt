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
package com.geekorum.ttrss.articles_list.search

import androidx.annotation.MainThread
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.geekorum.ttrss.session.SessionActivityComponent
import com.geekorum.ttrss.articles_list.ArticlesRepository
import com.geekorum.ttrss.data.Article

class SearchViewModel @ViewModelInject constructor(
    componentFactory: SessionActivityComponent.Factory
) : ViewModel() {

    private val articlesRepository: ArticlesRepository = componentFactory.newComponent().articleRepository

    private val searchQuery = MutableLiveData<String>().apply { value = "" }

    val articles: LiveData<PagingData<Article>> = Transformations.switchMap(searchQuery) {
        val factory = articlesRepository.searchArticles(it)
        Pager(PagingConfig(pageSize = 50)) {
            factory
        }.liveData
    }

    @MainThread
    fun setSearchQuery(keyword: String) {
        searchQuery.value = keyword
    }

    fun setArticleStarred(articleId: Long, newValue: Boolean) {
        articlesRepository.setArticleStarred(articleId, newValue)
    }

    fun setArticleUnread(articleId: Long, newValue: Boolean) {
        articlesRepository.setArticleUnread(articleId, newValue)
    }

}
