
package com.screenocr.monitor.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.*

class AlarmPlayer(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var alarmJob: Job? = null

    @Volatile
    var isAlarmRinging = false
        private set

    var onAlarmStateChanged: ((Boolean) -> Unit)? = null

    private val alarmScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        try {
            toneGenerator = ToneGenerator(
                AudioManager.STREAM_ALARM,
                100
            )
        } catch (e: Exception) {
            try {
                toneGenerator = ToneGenerator(
                    AudioManager.STREAM_MUSIC,
                    100
                )
            } catch (e: Exception) {
                toneGenerator = null
            }
        }
    }

    @Synchronized
    private fun playBeepTone() {
        try {
            toneGenerator?.startTone(
                ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE,
                700
            )
        } catch (e: Exception) {
            // Ignore audio playback errors.
        }
    }

    fun triggerAlarm(
        continuous: Boolean,
        beepIntervalSeconds: Double
    ) {
        if (continuous) {

            if (isAlarmRinging) return

            isAlarmRinging = true
            onAlarmStateChanged?.invoke(true)

            alarmJob?.cancel()

            alarmJob = alarmScope.launch {
                while (isAlarmRinging && isActive) {

                    playBeepTone()

                    val sleepMs =
                        (maxOf(0.5, beepIntervalSeconds) * 1000)
                            .toLong()

                    delay(sleepMs)
                }
            }

        } else {
            alarmScope.launch {
                playBeepTone()
            }
        }
    }

    @Synchronized
    fun stopAlarm() {
        isAlarmRinging = false

        alarmJob?.cancel()
        alarmJob = null

        try {
            toneGenerator?.stopTone()
        } catch (e: Exception) {
            // Ignore audio stopping errors.
        }

        onAlarmStateChanged?.invoke(false)
    }

    fun release() {
        stopAlarm()

        alarmScope.cancel()

        synchronized(this) {
            try {
                toneGenerator?.release()
                toneGenerator = null
            } catch (e: Exception) {
                // Ignore release errors.
            }
        }
    }
}
