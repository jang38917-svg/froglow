package com.froglow.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.froglow.app.network.ReminderDto
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    fun scheduleAll(context: Context, reminders: List<ReminderDto>) {
        reminders.forEach { reminder ->
            cancelReminder(context, reminder.id)
        }

        reminders
            .filter { it.is_active }
            .forEach { reminder ->
                scheduleReminder(context, reminder)
            }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        Log.d("REMINDER_DEBUG", "cancel reminder id=$reminderId")
    }

    private fun scheduleReminder(context: Context, reminder: ReminderDto) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val message = buildMessage(reminder)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", "Froglow 리마인더")
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = getFirstTriggerMillis(reminder)

        Log.d(
            "REMINDER_DEBUG",
            "schedule id=${reminder.id}, type=${reminder.reminder_type}, key=${reminder.routine_key}, time=${reminder.remind_time}, triggerAt=$triggerAtMillis, message=$message"
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun buildMessage(reminder: ReminderDto): String {
        return when (reminder.reminder_type.uppercase()) {
            "ROUTINE" -> {
                when (reminder.routine_key?.uppercase()) {
                    "AM" -> "아침 루틴을 완료해 주세요!"
                    "PM" -> "저녁 루틴을 완료해 주세요!"
                    else -> reminder.label ?: "루틴을 확인해 주세요!"
                }
            }

            "PRODUCT" -> {
                val label = reminder.label ?: "제품"
                "${label} 사용 시간을 알려드려요!"
            }

            else -> reminder.label ?: "리마인더를 확인해 주세요!"
        }
    }

    private fun getFirstTriggerMillis(reminder: ReminderDto): Long {
        val zoneId = ZoneId.of(reminder.timezone ?: "Asia/Seoul")
        val now = LocalDateTime.now(zoneId)

        val time = try {
            LocalTime.parse(reminder.remind_time ?: "09:00")
        } catch (_: Exception) {
            LocalTime.of(9, 0)
        }

        var triggerDateTime = now
            .withHour(time.hour)
            .withMinute(time.minute)
            .withSecond(0)
            .withNano(0)

        if (!triggerDateTime.isAfter(now)) {
            triggerDateTime = triggerDateTime.plusDays(1)
        }

        Log.d(
            "REMINDER_DEBUG",
            "now=$now, remindTime=$time, triggerDateTime=$triggerDateTime"
        )

        return triggerDateTime
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}