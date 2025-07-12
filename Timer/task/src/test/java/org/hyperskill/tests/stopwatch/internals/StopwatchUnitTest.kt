package org.hyperskill.tests.stopwatch.internals

import android.app.Activity
import android.app.AlarmManager
import android.os.SystemClock
import androidx.core.content.getSystemService
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

open class StopwatchUnitTest<T : Activity>(clazz: Class<T>) : AbstractUnitTest<T>(clazz) {
    fun supportForAlarmManager() {
        val alarmManager = activity.getSystemService<AlarmManager>()
        val shadowAlarmManager: ShadowAlarmManager = shadowOf(alarmManager)
        val toTrigger = shadowAlarmManager.scheduledAlarms.filter { scheduledAlarm ->
            (scheduledAlarm.triggerAtMs - SystemClock.currentGnssTimeClock().millis()) <= 0
        }
        toTrigger.forEach(shadowAlarmManager::fireAlarm)
    }
}