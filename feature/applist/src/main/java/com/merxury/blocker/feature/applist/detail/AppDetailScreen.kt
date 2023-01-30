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

package com.merxury.blocker.feature.applist.detail

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.merxury.blocker.core.designsystem.component.BlockerCollapsingTopAppBar
import com.merxury.blocker.core.designsystem.component.BlockerScrollableTabRow
import com.merxury.blocker.core.designsystem.component.BlockerTab
import com.merxury.blocker.core.designsystem.icon.BlockerIcons
import com.merxury.blocker.core.designsystem.theme.BlockerTheme
import com.merxury.blocker.core.model.Application
import com.merxury.blocker.core.ui.TabState
import com.merxury.blocker.feature.applist.R.string
import com.merxury.blocker.feature.applist.detail.cmplist.ComponentListContentRoute
import com.merxury.blocker.feature.applist.detail.component.AppInfoCard
import com.merxury.blocker.feature.applist.detail.component.AppInfoTabContent
import com.merxury.blocker.feature.applist.detail.component.TopAppBarMoreMenu
import kotlinx.datetime.Clock.System

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    app: Application,
    tabState: TabState,
    onRefresh: () -> Unit,
    switchTab: (Int) -> Unit,
    onBackClick: () -> Unit,
    onShare: () -> Unit,
    onFindInPage: () -> Unit,
    onEnableApp: () -> Unit,
    onEnableAll: () -> Unit,
    onBlockAll: () -> Unit,
    isCollapsed: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    onExportRules: () -> Unit,
    onImportRules: () -> Unit,
    onExportIfw: () -> Unit,
    onImportIfw: () -> Unit,
    onResetIfw: () -> Unit,
    isExpendedScreen: Boolean,
) {
    Row(modifier.fillMaxSize()) {
        AppDetailContent(
            app = app,
            tabState = tabState,
            onRefresh = onRefresh,
            switchTab = switchTab,
            onBackClick = onBackClick,
            onShare = onShare,
            onFindInPage = onFindInPage,
            onEnableApp = onEnableApp,
            onEnableAll = onEnableAll,
            onBlockAll = onBlockAll,
            isCollapsed = isCollapsed,
            scrollBehavior = scrollBehavior,
            modifier = modifier,
            onExportRules = onExportRules,
            onImportRules = onImportRules,
            onExportIfw = onExportIfw,
            onImportIfw = onImportIfw,
            onResetIfw = onResetIfw,
            isExpendedScreen = isExpendedScreen,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailContent(
    app: Application,
    tabState: TabState,
    onRefresh: () -> Unit,
    switchTab: (Int) -> Unit,
    onBackClick: () -> Unit,
    onShare: () -> Unit,
    onFindInPage: () -> Unit,
    onEnableApp: () -> Unit,
    onEnableAll: () -> Unit,
    onBlockAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    isCollapsed: Boolean,
    modifier: Modifier = Modifier,
    onExportRules: () -> Unit,
    onImportRules: () -> Unit,
    onExportIfw: () -> Unit,
    onImportIfw: () -> Unit,
    onResetIfw: () -> Unit,
    isExpendedScreen: Boolean,
) {
    Scaffold(
        topBar = {
            BlockerCollapsingTopAppBar(
                title = app.label,
                content = {
                    AppInfoCard(
                        label = app.label,
                        packageName = app.packageName,
                        versionCode = app.versionCode,
                        versionName = app.versionName,
                        packageInfo = app.packageInfo,
                        onAppIconClick = { /* TODO add click callback */ },
                    )
                },
                isCollapsed = isCollapsed,
                scrollBehavior = scrollBehavior,
                onNavigationClick = onBackClick,
                hasNavigationIcon = !isExpendedScreen,
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = BlockerIcons.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onFindInPage) {
                        Icon(
                            imageVector = BlockerIcons.Find,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (tabState.currentIndex != 0) {
                        TopAppBarMoreMenu(
                            onEnableApp = onEnableApp,
                            onRefresh = onRefresh,
                            onEnableAll = onEnableAll,
                            onBlockAll = onBlockAll,
                        )
                    }
                },
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxWidth(),
        ) {
            BlockerScrollableTabRow(
                selectedTabIndex = tabState.currentIndex,
            ) {
                tabState.titles.forEachIndexed { index, titleRes ->
                    BlockerTab(
                        selected = index == tabState.currentIndex,
                        onClick = { switchTab(index) },
                        text = { Text(text = stringResource(id = titleRes)) },
                    )
                }
            }
            when (tabState.currentIndex) {
                0 -> AppInfoTabContent(
                    app = app,
                    onExportRules = onExportRules,
                    onImportRules = onImportRules,
                    onExportIfw = onExportIfw,
                    onImportIfw = onImportIfw,
                    onResetIfw = onResetIfw,
                )

                1 -> ComponentListContentRoute()
                2 -> ComponentListContentRoute()
                3 -> ComponentListContentRoute()
                4 -> ComponentListContentRoute()
            }
        }
    }
}

@Composable
fun ErrorAppDetailScreen(message: String) {
    Text(text = message)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun AppDetailScreenPreview() {
    val app = Application(
        label = "Blocker",
        packageName = "com.mercury.blocker",
        versionName = "1.2.69-alpha",
        isEnabled = false,
        firstInstallTime = System.now(),
        lastUpdateTime = System.now(),
        packageInfo = null,
    )
    val tabState = TabState(
        titles = listOf(
            string.app_info,
            string.service,
            string.service,
            string.activity,
            string.content_provider,
        ),
        currentIndex = 0,
    )
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    BlockerTheme {
        Surface {
            AppDetailScreen(
                app = app,
                tabState = tabState,
                onRefresh = {},
                switchTab = {},
                onBackClick = {},
                onShare = {},
                onFindInPage = {},
                onEnableApp = {},
                onEnableAll = {},
                onBlockAll = {},
                isCollapsed = false,
                scrollBehavior = scrollBehavior,
                onExportRules = {},
                onImportRules = {},
                onExportIfw = {},
                onImportIfw = {},
                onResetIfw = {},
                isExpendedScreen = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
fun AppDetailScreenCollapsedPreview() {
    val app = Application(
        label = "Blocker",
        packageName = "com.mercury.blocker",
        versionName = "1.2.69-alpha",
        isEnabled = false,
        firstInstallTime = System.now(),
        lastUpdateTime = System.now(),
        packageInfo = null,
    )
    val tabState = TabState(
        titles = listOf(
            string.app_info,
            string.receiver,
            string.service,
            string.activity,
            string.content_provider,
        ),
        currentIndex = 0,
    )
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    BlockerTheme {
        Surface {
            AppDetailScreen(
                app = app,
                tabState = tabState,
                onRefresh = {},
                switchTab = {},
                onBackClick = {},
                onShare = {},
                onFindInPage = {},
                onEnableApp = {},
                onEnableAll = {},
                onBlockAll = {},
                isCollapsed = true,
                scrollBehavior = scrollBehavior,
                onExportRules = {},
                onImportRules = {},
                onExportIfw = {},
                onImportIfw = {},
                onResetIfw = {},
                isExpendedScreen = false,
            )
        }
    }
}
