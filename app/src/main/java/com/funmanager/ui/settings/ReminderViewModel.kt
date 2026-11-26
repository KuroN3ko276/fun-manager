package com.funmanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funmanager.domain.ReminderSettings
import com.funmanager.reminder.ReminderManager
import com.funmanager.ui.container
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime

class ReminderViewModel(private val reminders: ReminderManager) : ViewModel() {

    val settings: StateFlow<ReminderSettings> = reminders.settings

    fun setEnabled(enabled: Boolean) = reminders.setEnabled(enabled)

    fun setTime(time: LocalTime) = reminders.setTime(time)

    companion object {
        val Factory = viewModelFactory {
            initializer { ReminderViewModel(container.reminders) }
        }
    }
}
