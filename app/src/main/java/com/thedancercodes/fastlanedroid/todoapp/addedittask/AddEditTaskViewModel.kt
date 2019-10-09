package com.thedancercodes.fastlanedroid.todoapp.addedittask

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thedancercodes.fastlanedroid.todoapp.Event
import com.thedancercodes.fastlanedroid.todoapp.R
import com.thedancercodes.fastlanedroid.todoapp.data.Result.Success
import com.thedancercodes.fastlanedroid.todoapp.data.Task
import com.thedancercodes.fastlanedroid.todoapp.data.source.TasksRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Add/Edit screen.
 *
 *
 * This ViewModel only exposes [ObservableField]s, so it doesn't need to extend
 * [androidx.databinding.BaseObservable] and updates are notified automatically. See
 * [com.thedancercodes.fastlanedroid.todoapp.statistics.StatisticsViewModel] for
 * how to deal with more complex scenarios.
 */
class AddEditTaskViewModel(
    private val tasksRepository: TasksRepository
) : ViewModel() {

    // Two-way databinding, exposing MutableLiveData
    val title = MutableLiveData<String>()

    // Two-way databinding, exposing MutableLiveData
    val description = MutableLiveData<String>()

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> =_dataLoading

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarMessage: LiveData<Event<Int>> = _snackbarText

    private val _taskUpdated = MutableLiveData<Event<Unit>>()
    val taskUpdatedEvent: LiveData<Event<Unit>> = _taskUpdated

    private var taskId: String? = null

    private var isNewTask: Boolean = false

    private var isDataLoaded = false

    private var taskCompleted = false

    fun start(taskId: String?) {
        _dataLoading.value?.let { isLoading ->
            // Already loading, ignore.
            if (isLoading) return
        }
        this.taskId = taskId
        if (taskId == null) {
            // No need to populate, it's a new task
            isNewTask = true
            return
        }
        if (isDataLoaded) {
            // No need to populate, already have data.
            return
        }
        isNewTask = false
        _dataLoading.value = true

        viewModelScope.launch {
            tasksRepository.getTask(taskId).let { result ->
                if (result is Success) {
                    onTaskLoaded(result.data)
                } else {
                    onDataNotAvailable()
                }
            }
        }
    }

    private fun onTaskLoaded(task: Task) {
        title.value = task.title
        description.value = task.description
        taskCompleted = task.isCompleted
        _dataLoading.value = false
        isDataLoaded = true
    }

    private fun onDataNotAvailable() {
        _dataLoading.value = false
    }

    // Called when clicking on fab.
    fun saveTask() {
        val currentTitle = title.value
        val currentDescription = description.value

        if (currentTitle == null || currentDescription == null) {
            _snackbarText.value =  Event(R.string.empty_task_message)
            return
        }
        if (Task(currentTitle, currentDescription).isEmpty) {
            _snackbarText.value =  Event(R.string.empty_task_message)
            return
        }

        val currentTaskId = taskId
        if (isNewTask || currentTaskId == null) {
            createTask(Task(currentTitle, currentDescription))
        } else {
            val task = Task(currentTitle, currentDescription, taskCompleted, currentTaskId)
            updateTask(task)
        }
    }

    private fun createTask(newTask: Task) = viewModelScope.launch {
        tasksRepository.saveTask(newTask)
        _taskUpdated.value = Event(Unit)
    }

    private fun updateTask(task: Task) {
        if (isNewTask) {
            throw RuntimeException("updateTask() was called but task is new.")
        }
        viewModelScope.launch {
            tasksRepository.saveTask(task)
            _taskUpdated.value = Event(Unit)
        }
    }
}