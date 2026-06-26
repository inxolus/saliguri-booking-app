package com.example.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class AppEvent {
    RESERVATION_STATUS_CHANGED,
    RESERVATION_CREATED,
    RESERVATION_DELETED
}

object EventBus {
    private val _events = MutableSharedFlow<AppEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }
}
