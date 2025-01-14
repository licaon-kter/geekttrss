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
package com.geekorum.ttrss.settings.manage_features

import androidx.lifecycle.ViewModel
import com.geekorum.ttrss.Features
import com.geekorum.ttrss.on_demand_modules.ImmutableModuleManager
import com.geekorum.ttrss.on_demand_modules.OnDemandModuleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ManageFeaturesViewModel @Inject constructor(
    private val moduleManager: OnDemandModuleManager
) : ViewModel() {
    private val moduleStatus = MutableStateFlow<List<FeatureStatus>>(emptyList()).apply {
        value = Features.allFeatures.map {
            FeatureStatus(it,it in moduleManager.installedModules)
        }
    }

    val canModify = moduleManager !is ImmutableModuleManager
    val features = moduleStatus.asStateFlow()

    fun uninstallModule(module: String) {
        moduleManager.uninstall(module)
        refreshModuleStatus()
    }

    private fun refreshModuleStatus() {
        moduleStatus.value = Features.allFeatures.map {
            FeatureStatus(it,
                it in moduleManager.installedModules
            )
        }
    }
}

data class FeatureStatus(
    val name: String,
    val installed: Boolean
)
