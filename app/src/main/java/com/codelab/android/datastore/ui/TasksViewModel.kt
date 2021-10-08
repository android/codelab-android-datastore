/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codelab.android.datastore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.codelab.android.datastore.data.SortOrder
import com.codelab.android.datastore.data.Task
import com.codelab.android.datastore.data.TasksRepository
import com.codelab.android.datastore.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

data class TasksUiModel(
    val tasks: List<Task>,
    val showCompleted: Boolean,
    val sortOrder: SortOrder
)

class TasksViewModel(
    repository: TasksRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Keep the show completed filter as a stream of changes
    private val showCompletedFlow = MutableStateFlow(false)

    // Keep the sort order as a stream of changes
    private val sortOrderFlow = userPreferencesRepository.sortOrderFlow

    // Every time the sort order, the show completed filter or the list of tasks emit,
    // we should recreate the list of tasks
    private val tasksUiModelFlow = combine(
        repository.tasks,
        showCompletedFlow,
        sortOrderFlow
    ) { tasks: List<Task>, showCompleted: Boolean, sortOrder: SortOrder ->
        return@combine TasksUiModel(
            tasks = filterSortTasks(tasks, showCompleted, sortOrder),
            showCompleted = showCompleted,
            sortOrder = sortOrder
        )
    }
    val tasksUiModel = tasksUiModelFlow.asLiveData()

    private fun filterSortTasks(
        tasks: List<Task>,
        showCompleted: Boolean,
        sortOrder: SortOrder
    ): List<Task> {
        // filter the tasks
        val filteredTasks = if (showCompleted) {
            tasks
        } else {
            tasks.filter { !it.completed }
        }
        // sort the tasks
        return when (sortOrder) {
            SortOrder.NONE -> filteredTasks
            SortOrder.BY_DEADLINE -> filteredTasks.sortedByDescending { it.deadline }
            SortOrder.BY_PRIORITY -> filteredTasks.sortedBy { it.priority }
            SortOrder.BY_DEADLINE_AND_PRIORITY -> filteredTasks.sortedWith(
                compareByDescending<Task> { it.deadline }.thenBy { it.priority }
            )
        }
    }

    fun showCompletedTasks(show: Boolean) {
        showCompletedFlow.value = show
    }

    fun enableSortByDeadline(enable: Boolean) {
        userPreferencesRepository.enableSortByDeadline(enable)
    }

    fun enableSortByPriority(enable: Boolean) {
        userPreferencesRepository.enableSortByPriority(enable)
    }
}

class TasksViewModelFactory(
    private val repository: TasksRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(repository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
