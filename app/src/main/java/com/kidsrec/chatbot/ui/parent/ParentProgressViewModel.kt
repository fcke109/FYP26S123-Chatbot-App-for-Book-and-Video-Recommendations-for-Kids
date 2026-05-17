package com.kidsrec.chatbot.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kidsrec.chatbot.data.model.WeeklyLearningReport
import com.kidsrec.chatbot.data.repository.LearningProgressManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel responsible for loading and exposing a child's weekly learning progress to the parent screen
@HiltViewModel
class ParentProgressViewModel @Inject constructor(
    private val learningProgressManager: LearningProgressManager
) : ViewModel() {

    // Stores the current weekly learning report for the selected child
    private val _weeklyReport = MutableStateFlow(WeeklyLearningReport())
    val weeklyReport: StateFlow<WeeklyLearningReport> = _weeklyReport.asStateFlow()

    // Observes the selected child's progress and updates the weekly report in real time
    fun observeChildProgress(childUserId: String) {
        viewModelScope.launch {
            learningProgressManager.getWeeklyReportFlow(childUserId).collect { report ->
                // Updates the UI state whenever a new weekly report is received
                _weeklyReport.value = report
            }
        }
    }
}