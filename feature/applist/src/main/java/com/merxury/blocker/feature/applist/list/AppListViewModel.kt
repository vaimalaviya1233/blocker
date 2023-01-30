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

package com.merxury.blocker.feature.applist.list

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.merxury.blocker.core.data.respository.userdata.UserDataRepository
import com.merxury.blocker.core.extension.exec
import com.merxury.blocker.core.model.AppServiceStatus
import com.merxury.blocker.core.model.Application
import com.merxury.blocker.core.model.preference.AppSorting
import com.merxury.blocker.core.model.preference.AppSorting.FIRST_INSTALL_TIME_ASCENDING
import com.merxury.blocker.core.model.preference.AppSorting.FIRST_INSTALL_TIME_DESCENDING
import com.merxury.blocker.core.model.preference.AppSorting.LAST_UPDATE_TIME_ASCENDING
import com.merxury.blocker.core.model.preference.AppSorting.LAST_UPDATE_TIME_DESCENDING
import com.merxury.blocker.core.model.preference.AppSorting.NAME_ASCENDING
import com.merxury.blocker.core.model.preference.AppSorting.NAME_DESCENDING
import com.merxury.blocker.core.network.BlockerDispatchers.DEFAULT
import com.merxury.blocker.core.network.BlockerDispatchers.IO
import com.merxury.blocker.core.network.Dispatcher
import com.merxury.blocker.core.ui.TabState
import com.merxury.blocker.core.ui.data.ErrorMessage
import com.merxury.blocker.core.utils.ApplicationUtil
import com.merxury.blocker.core.utils.FileUtils
import com.merxury.blocker.feature.applist.R.string
import com.merxury.blocker.feature.applist.list.HomeUiState.NoApps
import com.merxury.blocker.feature.applist.list.HomeUiState.Success
import com.merxury.blocker.feature.applist.list.state.AppStateCache
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class AppListViewModel @Inject constructor(
    app: android.app.Application,
    private val userDataRepository: UserDataRepository,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Dispatcher(DEFAULT) private val cpuDispatcher: CoroutineDispatcher,
) : AndroidViewModel(app) {
    private val viewModelState = MutableStateFlow(HomeViewModelState(isLoading = true))
    val uiState = viewModelState
        .map(HomeViewModelState::toUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            viewModelState.value.toUiState(),
        )

    private val _tabState = MutableStateFlow(
        TabState(
            titles = listOf(
                string.app_info,
                string.receiver,
                string.service,
                string.activity,
                string.content_provider,
            ),
            currentIndex = 0,
        ),
    )
    val tabState: StateFlow<TabState> = _tabState.asStateFlow()
    fun switchTab(newIndex: Int) {
        if (newIndex != tabState.value.currentIndex) {
            _tabState.update {
                it.copy(currentIndex = newIndex)
            }
        }
    }

    var errorState = mutableStateOf<ErrorMessage?>(null)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
        errorState.value = ErrorMessage(
            throwable.localizedMessage
                ?: throwable.message
                ?: throwable.stackTraceToString()
                    .split("\n")
                    .first(),
            throwable.stackTraceToString(),
        )
    }
    private val channel = Channel<Job>(capacity = Channel.UNLIMITED).apply {
        viewModelScope.launch {
            consumeEach { it.join() }
        }
    }

    init {
        loadData()
        listenSortingChanges()
        listenShowSystemAppsChanges()
    }

    fun loadData() = viewModelScope.launch {
        viewModelState.update { it.copy(isLoading = true) }
        val preference = userDataRepository.userData.first()
        val sortType = preference.appSorting
        val list = if (preference.showSystemApps) {
            ApplicationUtil.getApplicationList(getApplication())
        } else {
            ApplicationUtil.getThirdPartyApplicationList(getApplication())
        }
            .toMutableList()
        val stateAppList = mapToSnapshotStateList(list, getApplication())
        sortList(stateAppList, sortType)
        viewModelState.update { it.copy(appList = stateAppList) }
    }

    private fun loadDetail(app: android.app.Application) = viewModelScope.launch {
        val packageName = app.packageName
        val app = ApplicationUtil.getApplicationInfo(getApplication(), packageName)
        if (app == null) {
            val error = ErrorMessage("Can't find $packageName in this device.")
            Timber.e(error.message)
            viewModelState.update { it.copy(errorMessages = error) }
        } else {
            viewModelState.update { it.copy(selectedAppName = app.packageName) }
        }
    }

    private fun listenSortingChanges() = viewModelScope.launch {
        userDataRepository.userData
            .map { it.appSorting }
            .distinctUntilChanged()
            .collect {
                val uiState = viewModelState.value
                if (uiState.appList != null) {
                    sortList(uiState.appList, it)
                }
            }
    }

    private fun listenShowSystemAppsChanges() = viewModelScope.launch {
        userDataRepository.userData
            .map { it.showSystemApps }
            .distinctUntilChanged()
            .collect {
                loadData()
            }
    }

    fun updateSorting(sorting: AppSorting) = viewModelScope.launch {
        userDataRepository.setAppSorting(sorting)
    }

    fun updateServiceStatus(packageName: String) {
        channel.trySend(
            viewModelScope.launch(
                start = CoroutineStart.LAZY,
                context = ioDispatcher + exceptionHandler,
            ) {
                val userData = userDataRepository.userData.first()
                if (!userData.showServiceInfo) {
                    return@launch
                }
                Timber.d("Get service status for $packageName")
                val currentUiState = viewModelState.value
                if (currentUiState.isLoading || currentUiState.appList == null) {
                    Timber.e("Ui state is incorrect, don't update service status.")
                    return@launch
                }
                val currentList = currentUiState.appList
                val itemIndex = currentList.indexOfFirst { it.packageName == packageName }
                val oldItem = currentList.getOrNull(itemIndex) ?: return@launch
                if (oldItem.appServiceStatus != null) {
                    // Don't get service info again
                    return@launch
                }
                val status = AppStateCache.get(getApplication(), packageName)
                val serviceStatus = AppServiceStatus(
                    packageName = status.packageName,
                    running = status.running,
                    blocked = status.blocked,
                    total = status.total,
                )
                val newItem = oldItem.copy(appServiceStatus = serviceStatus)
                currentList[itemIndex] = newItem
            },
        )
    }

    fun dismissDialog() {
        errorState.value = null
    }

    fun clearData(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "pm clear $packageName".exec(ioDispatcher)
    }

    fun clearCache(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        val context: Context = getApplication()
        val cacheFolder = context.filesDir
            ?.parentFile
            ?.parentFile
            ?.resolve(packageName)
            ?.resolve("cache")
            ?: run {
                Timber.e("Can't resolve cache path for $packageName")
                return@launch
            }
        Timber.d("Delete cache folder: $cacheFolder")
        FileUtils.delete(cacheFolder.absolutePath, recursively = true, ioDispatcher)
    }

    fun uninstall(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "pm uninstall $packageName".exec(ioDispatcher)
    }

    fun forceStop(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "am force-stop $packageName".exec(ioDispatcher)
    }

    fun enable(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "pm enable $packageName".exec(ioDispatcher)
    }

    fun disable(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "pm disable $packageName".exec(ioDispatcher)
    }

    fun onRefresh() {
        // TODO
    }

    fun onShare() {
        // TODO
    }

    fun onFindInPage() {
        // TODO
    }

    fun onEnableApp() {
        // TODO
    }

    fun onEnableAll() {
        // TODO
    }

    fun onBlockAll() {
        // TODO
    }

    fun onExportRules() {
        // TODO
    }

    fun onImportRules() {
        // TODO
    }

    fun onExportIfw() {
        // TODO
    }

    fun onImportIfw() {
        // TODO
    }

    fun onResetIfw() {
        // TODO
    }

    private suspend fun sortList(
        list: SnapshotStateList<Application>,
        sorting: AppSorting,
    ) = withContext(cpuDispatcher) {
        when (sorting) {
            NAME_ASCENDING -> list.sortBy { it.label.lowercase() }
            NAME_DESCENDING -> list.sortByDescending { it.label.lowercase() }
            FIRST_INSTALL_TIME_ASCENDING -> list.sortBy { it.firstInstallTime }
            FIRST_INSTALL_TIME_DESCENDING -> list.sortByDescending { it.firstInstallTime }
            LAST_UPDATE_TIME_ASCENDING -> list.sortBy { it.lastUpdateTime }
            LAST_UPDATE_TIME_DESCENDING -> list.sortByDescending { it.lastUpdateTime }
        }
        list.sortBy { it.isEnabled }
    }

    private suspend fun mapToSnapshotStateList(
        list: MutableList<Application>,
        context: Context,
    ): SnapshotStateList<Application> = withContext(cpuDispatcher) {
        val stateAppList = mutableStateListOf<Application>()
        list.forEach {
            val appItem = Application(
                label = it.label,
                packageName = it.packageName,
                versionName = it.versionName.orEmpty(),
                versionCode = it.versionCode,
                isSystem = ApplicationUtil.isSystemApp(context.packageManager, it.packageName),
                // TODO detect if an app is running or not
                isRunning = false,
                isEnabled = it.isEnabled,
                firstInstallTime = it.firstInstallTime,
                lastUpdateTime = it.lastUpdateTime,
                // TODO get service status
                appServiceStatus = null,
                packageInfo = it.packageInfo,
            )
            stateAppList.add(appItem)
        }
        return@withContext stateAppList
    }
}

/**
 * Data representation for the installed application.
 * App icon will be loaded by PackageName.
 */

sealed interface HomeUiState {
    val isLoading: Boolean
    val errorMessages: ErrorMessage

    data class NoApps(
        override val isLoading: Boolean,
        override val errorMessages: ErrorMessage,
    ) : HomeUiState

    data class Success(
        val appList: SnapshotStateList<Application>,
        val isDetailOpen: Boolean,
        val selectedApp: Application,
        override val isLoading: Boolean,
        override val errorMessages: ErrorMessage,
    ) : HomeUiState
}

enum class ScreenType {
    ListAndDetails,
    ListOnly,
    DetailsOnly
}

private data class HomeViewModelState(
    val appList: SnapshotStateList<Application>? = null,
    val isDetailOpen: Boolean = false,
    val selectedAppName: String? = null,
    val isLoading: Boolean = true,
    val errorMessages: ErrorMessage = ErrorMessage(message = ""),
) {

    /**
     * Converts this [HomeViewModelState] into a more strongly typed [HomeUiState] for driving
     * the ui.
     */
    fun toUiState(): HomeUiState =
        if (appList == null) {
            NoApps(
                isLoading = isLoading,
                errorMessages = errorMessages,
            )
        } else {
            Success(
                appList = appList,
                selectedApp = appList.find {
                    it.packageName == selectedAppName
                } ?: appList[0],
                isDetailOpen = isDetailOpen,
                isLoading = isLoading,
                errorMessages = errorMessages,
            )
        }
}
