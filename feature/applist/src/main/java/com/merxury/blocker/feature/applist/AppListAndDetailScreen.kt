/*
 * Copyright 2023 Blocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.merxury.blocker.feature.applist

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.merxury.blocker.core.model.preference.AppSorting
import com.merxury.blocker.feature.applist.list.AppListContent
import com.merxury.blocker.feature.applist.list.HomeUiState

@Composable
fun AppListAndDetailScreen(
    uiState: HomeUiState.Success,
    onAppItemClick: (String) -> Unit,
    onClearCacheClick: (String) -> Unit,
    onClearDataClick: (String) -> Unit,
    onForceStopClick: (String) -> Unit,
    onUninstallClick: (String) -> Unit,
    onEnableClick: (String) -> Unit,
    onDisableClick: (String) -> Unit,
    onServiceStateUpdate: (String) -> Unit,
    onSortingUpdate: (AppSorting) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToSupportAndFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandedScreen: Boolean,
    appDetailLazyListStates: Map<String, LazyListState>,
) {
    Row {
        AppListContent(
            appList = uiState.appList,
            onAppItemClick = onAppItemClick,
            onClearCacheClick = onClearCacheClick,
            onClearDataClick = onClearDataClick,
            onForceStopClick = onForceStopClick,
            onUninstallClick = onUninstallClick,
            onEnableClick = onEnableClick,
            onDisableClick = onDisableClick,
            onServiceStateUpdate = onServiceStateUpdate,
            modifier = modifier,
        )
        Crossfade(targetState = uiState.selectedApp) { detailApp ->
            // Get the lazy list state for this detail view
            val detailLazyListState by remember {
                derivedStateOf {
                    appDetailLazyListStates.getValue(detailApp.packageName)
                }
            }
            key(detailApp.label) {

            }
        }

    }
}