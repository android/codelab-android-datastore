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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.codelab.android.datastore.R
import com.codelab.android.datastore.data.Task
import com.codelab.android.datastore.data.TaskPriority
import com.codelab.android.datastore.databinding.TaskViewItemBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Holder for a task item in the tasks list
 */
class TaskViewHolder(
    private val binding: TaskViewItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    // Format date as: Apr 6, 2020
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    /**
     * Bind the task to the UI elements
     */
    fun bind(todo: Task) {
        binding.task.text = todo.name
        setTaskPriority(todo)
        binding.deadline.text = dateFormat.format(todo.deadline)
        // if a task was completed, show it grayed out
        val color = if (todo.completed) {
            R.color.greyAlpha
        } else {
            R.color.white
        }
        itemView.setBackgroundColor(
            ContextCompat.getColor(
                itemView.context,
                color
            )
        )
    }

    private fun setTaskPriority(todo: Task) {
        binding.priority.text = itemView.context.resources.getString(
            R.string.priority_value,
            todo.priority.name
        )
        // set the priority color based on the task priority
        val textColor = when (todo.priority) {
            TaskPriority.HIGH -> R.color.red
            TaskPriority.MEDIUM -> R.color.yellow
            TaskPriority.LOW -> R.color.green
        }
        binding.priority.setTextColor(ContextCompat.getColor(itemView.context, textColor))
    }

    companion object {
        fun create(parent: ViewGroup): TaskViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.task_view_item, parent, false)
            val binding = TaskViewItemBinding.bind(view)
            return TaskViewHolder(binding)
        }
    }
}
