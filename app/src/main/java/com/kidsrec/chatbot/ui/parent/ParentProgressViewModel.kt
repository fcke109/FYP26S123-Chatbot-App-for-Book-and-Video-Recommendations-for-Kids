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

@HiltViewModel
class ParentProgressViewModel @Inject constructor(
    private val learningProgressManager: LearningProgressManager
) : ViewModel() {

    private val _weeklyReport = MutableStateFlow(WeeklyLearningReport())
    val weeklyReport: StateFlow<WeeklyLearningReport> = _weeklyReport.asStateFlow()

    fun observeChildProgress(childUserId: String) {
        viewModelScope.launch {
            learningProgressManager.getWeeklyReportFlow(childUserId).collect { report ->
                _weeklyReport.value = report
            }
        }
    }
}