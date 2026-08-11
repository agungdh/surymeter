package com.example.surymeter.meter

import com.example.surymeter.data.DailyUsage
import com.example.surymeter.data.Speeds
import com.example.surymeter.data.Totals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MeterUiState(
    val running: Boolean = false,
    val speeds: Speeds = Speeds(),
    val today: DailyUsage = DailyUsage.empty(),
    val totals: Totals = Totals(),
    val days: List<DailyUsage> = emptyList()
)

object MeterState {
    private val _ui = MutableStateFlow(MeterUiState())
    val ui: StateFlow<MeterUiState> = _ui.asStateFlow()

    fun update(transform: (MeterUiState) -> MeterUiState) {
        _ui.value = transform(_ui.value)
    }

    fun snapshot(): MeterUiState = _ui.value
}
