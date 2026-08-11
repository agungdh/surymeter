package com.example.surymeter.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.surymeter.data.DailyUsage
import com.example.surymeter.data.DayKey
import com.example.surymeter.data.UsageStorage
import com.example.surymeter.meter.MeterService
import com.example.surymeter.meter.MeterState
import com.example.surymeter.meter.MeterUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsageViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = UsageStorage(app)

    private val _state = MutableStateFlow(MeterUiState())
    val state: StateFlow<MeterUiState> = _state.asStateFlow()

    init {
        loadFromStorage()
        viewModelScope.launch {
            MeterState.ui.collect { ui ->
                if (ui.running) {
                    _state.value = ui
                } else if (_state.value.running) {
                    loadFromStorage()
                }
            }
        }
    }

    private fun loadFromStorage() {
        val live = MeterState.ui.value
        if (live.running) {
            _state.value = live
            return
        }
        val s = storage.load()
        _state.value = MeterUiState(
            running = false,
            totals = s.totals,
            today = s.daily[DayKey.today()] ?: DailyUsage.empty(DayKey.today()),
            days = s.daily.values.sortedByDescending { it.date }
        )
    }

    fun startService() {
        val context = getApplication<Application>()
        ContextCompat.startForegroundService(context, Intent(context, MeterService::class.java))
    }

    fun stopService() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, MeterService::class.java))
    }
}
