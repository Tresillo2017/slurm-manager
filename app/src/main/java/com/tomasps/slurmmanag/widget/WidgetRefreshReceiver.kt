package com.tomasps.slurmmanag.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tomasps.slurmmanag.data.worker.PollWorker
import com.tomasps.slurmmanag.domain.repository.ServerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetRefreshReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REFRESH_ALL = "com.tomasps.slurmmanag.widget.ACTION_REFRESH_ALL"
    }

    @Inject lateinit var serverRepository: ServerRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFRESH_ALL) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverRepository.observeAll().first().forEach { server ->
                    PollWorker.enqueueImmediate(context, server.id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
