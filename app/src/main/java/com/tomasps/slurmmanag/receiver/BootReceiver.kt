package com.tomasps.slurmmanag.receiver

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
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var serverRepository: ServerRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverRepository.observeAll().first().forEach { server ->
                    PollWorker.enqueue(context, server.id, server.pollingIntervalMinutes)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
