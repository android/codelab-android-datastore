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

package com.codelab.android.datastore.data

import android.content.Context
import android.util.Log
import androidx.datastore.DataStore
import androidx.datastore.preferences.PreferenceDataStoreFactory
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.SharedPreferencesMigration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException

private const val USER_PREFERENCES_NAME = "user_preferences"
private const val USER_PREFERENCES_STORE_FILE_NAME = "user.preferences_pb"
private const val SORT_ORDER_KEY = "sort_order"
private const val SHOW_COMPLETED_KEY = "show_completed"

enum class SortOrder {
    NONE,
    BY_DEADLINE,
    BY_PRIORITY,
    BY_DEADLINE_AND_PRIORITY
}

data class UserPreferences(
    val showCompleted: Boolean,
    val sortOrder: SortOrder
)

/**
 * Extension function on Preferences to easily get the sort order
 */
private fun Preferences.getSortOrder(): SortOrder {
    val order = getString(SORT_ORDER_KEY, SortOrder.NONE.name)
    return SortOrder.valueOf(order)
}

/**
 * Extension function on Preferences to easily set the sort order
 */
private fun Preferences.withSortOrder(newSortOrder: SortOrder) =
    this.toBuilder().setString(SORT_ORDER_KEY, newSortOrder.name).build()

/**
 * Class that handles saving and retrieving user preferences
 */
class UserPreferencesRepository(context: Context) {

    private val TAG: String = "UserPreferencesRepo"

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory().create(
            produceFile = {
                File(
                    context.applicationContext.filesDir,
                    USER_PREFERENCES_STORE_FILE_NAME
                )
            },
            // Since we're migrating from SharedPreferences, add a migration based on the
            // SharedPreferences name
            migrationProducers = listOf(SharedPreferencesMigration(context, USER_PREFERENCES_NAME))
        )
    }

    /**
     * Get the user preferences flow.
     */
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(Preferences.empty())
            } else {
                throw exception
            }
        }.map { preferences ->
            // Get the sort order from preferences and convert it to a [SortOrder] object
            val sortOrder = preferences.getSortOrder()
            val showCompleted = preferences.getBoolean(SHOW_COMPLETED_KEY, false)
            UserPreferences(showCompleted, sortOrder)
        }

    /**
     * Enable / disable sort by deadline.
     */
    suspend fun enableSortByDeadline(enable: Boolean) {
        // updateData handles data transactionally, ensuring that if the sort is updated at the same
        // time from another thread, we won't have conflicts
        dataStore.updateData { currentPreferences ->
            val currentOrder = currentPreferences.getSortOrder()
            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_PRIORITY) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_DEADLINE
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_PRIORITY
                    } else {
                        SortOrder.NONE
                    }
                }
            currentPreferences.withSortOrder(newSortOrder)
        }
    }

    /**
     * Enable / disable sort by priority.
     */
    suspend fun enableSortByPriority(enable: Boolean) {
        // updateData handles data transactionally, ensuring that if the sort is updated at the same
        // time from another thread, we won't have conflicts
        dataStore.updateData { currentPreferences ->
            val currentOrder = currentPreferences.getSortOrder()
            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_DEADLINE) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_PRIORITY
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_DEADLINE
                    } else {
                        SortOrder.NONE
                    }
                }
            currentPreferences.withSortOrder(newSortOrder)
        }
    }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        dataStore.updateData { currentPreferences ->
            currentPreferences.toBuilder().setBoolean(SHOW_COMPLETED_KEY, showCompleted).build()
        }
    }
}
