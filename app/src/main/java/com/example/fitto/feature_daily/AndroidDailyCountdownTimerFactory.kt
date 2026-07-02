package com.example.fitto.feature_daily

import android.os.CountDownTimer

class AndroidDailyCountdownTimerFactory : DailyCountdownTimerFactory {

    override fun create(request: DailyCountdownTimerRequest): DailyCountdownTimer {
        return object : DailyCountdownTimer {
            private val timer = object : CountDownTimer(
                request.durationMillis,
                request.intervalMillis
            ) {
                override fun onTick(millisUntilFinished: Long) {
                    request.onTick(millisUntilFinished)
                }

                override fun onFinish() {
                    request.onFinish()
                }
            }

            override fun start() {
                timer.start()
            }

            override fun cancel() {
                timer.cancel()
            }
        }
    }
}
