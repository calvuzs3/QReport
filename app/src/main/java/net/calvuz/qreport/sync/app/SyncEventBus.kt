package net.calvuz.qreport.sync.app

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<SyncEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: SyncEvent) = _events.emit(event)
}

sealed class SyncEvent {
    object LoginSuccess : SyncEvent()
    object LoggedOut : SyncEvent()
}