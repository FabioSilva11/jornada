package app.jornada.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.jornada.JornadaApp
import app.jornada.MainActivity
import app.jornada.R
import app.jornada.data.DayRecord
import app.jornada.data.DayStatus
import app.jornada.data.Settings
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.domain.Periods
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object Notifications {
    const val CHANNEL_REMINDERS = "lembretes"
    const val CHANNEL_SUMMARY = "resumos"
    const val ID_DAILY = 1001
    const val ID_WEEKLY = 1002
    const val ID_VACATION = 1003

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Lembrete do dia", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pergunta se você foi trabalhar e avisa prazos de férias"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SUMMARY, "Fechamento da semana", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Quanto a semana rendeu"
            },
        )
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun post(ctx: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canPost(ctx)) return
        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showDaily(ctx: Context, date: LocalDate, value: Double) {
        val mark = PendingIntent.getBroadcast(
            ctx,
            1,
            Intent(ctx, MarkWorkedReceiver::class.java).putExtra(MarkWorkedReceiver.EXTRA_DATE, date.toString()),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (value > 0) "Marcar soma ${Fmt.money(value)} ao período." else "Toque para marcar o dia."
        post(
            ctx,
            ID_DAILY,
            NotificationCompat.Builder(ctx, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Você foi trabalhar hoje?")
                .setContentText(text)
                .setContentIntent(openApp(ctx))
                .addAction(0, "Fui trabalhar", mark)
                .setAutoCancel(true),
        )
    }

    fun showText(ctx: Context, id: Int, channel: String, title: String, text: String) {
        post(
            ctx,
            id,
            NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openApp(ctx))
                .setAutoCancel(true),
        )
    }
}

object Reminders {
    private const val WORK_DAILY = "lembrete_diario"
    private const val WORK_WEEKLY = "fechamento_semanal"
    const val KEY_KIND = "kind"
    const val KIND_DAILY = "daily"
    const val KIND_WEEKLY = "weekly"

    /** Agenda (ou cancela) os lembretes conforme os ajustes. */
    fun schedule(ctx: Context, s: Settings) {
        enqueueDaily(ctx, s, ExistingWorkPolicy.REPLACE)
        enqueueWeekly(ctx, s, ExistingWorkPolicy.REPLACE)
    }

    fun enqueueDaily(ctx: Context, s: Settings, policy: ExistingWorkPolicy) {
        val wm = WorkManager.getInstance(ctx)
        if (!s.onboarded || (!s.dailyReminder && !s.vacationAlert)) {
            wm.cancelUniqueWork(WORK_DAILY)
            return
        }
        val delay = delayUntil(LocalDateTime.now(), null, s.reminderMinutes)
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_KIND to KIND_DAILY))
            .build()
        wm.enqueueUniqueWork(WORK_DAILY, policy, req)
    }

    fun enqueueWeekly(ctx: Context, s: Settings, policy: ExistingWorkPolicy) {
        val wm = WorkManager.getInstance(ctx)
        if (!s.onboarded || !s.weeklySummary) {
            wm.cancelUniqueWork(WORK_WEEKLY)
            return
        }
        val delay = delayUntil(LocalDateTime.now(), s.summaryWeekday, s.summaryMinutes)
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_KIND to KIND_WEEKLY))
            .build()
        wm.enqueueUniqueWork(WORK_WEEKLY, policy, req)
    }

    /** Tempo até o próximo horário (opcionalmente num dia da semana, 1 = segunda … 7 = domingo). */
    fun delayUntil(now: LocalDateTime, weekday: Int?, minutes: Int): Duration {
        var target = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        if (weekday != null) target = target.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(weekday.coerceIn(1, 7))))
        if (!target.isAfter(now.plusSeconds(30))) target = if (weekday != null) target.plusWeeks(1) else target.plusDays(1)
        return Duration.between(now, target)
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as JornadaApp
        val data = app.repository.data.value
        val s = data.settings
        val today = LocalDate.now()
        val ledger = Ledger(data, today)
        when (inputData.getString(Reminders.KEY_KIND)) {
            Reminders.KIND_DAILY -> {
                if (s.onboarded && s.dailyReminder && ledger.isWorkday(today) && ledger.record(today) == null) {
                    Notifications.showDaily(applicationContext, today, ledger.defaultValue(today, DayStatus.WORKED))
                }
                if (s.onboarded && s.vacationAlert) vacationAlert(ledger, today)
                Reminders.enqueueDaily(applicationContext, s, ExistingWorkPolicy.APPEND_OR_REPLACE)
            }
            Reminders.KIND_WEEKLY -> {
                if (s.onboarded && s.weeklySummary) weeklySummary(ledger, today)
                Reminders.enqueueWeekly(applicationContext, s, ExistingWorkPolicy.APPEND_OR_REPLACE)
            }
        }
        return Result.success()
    }

    private fun weeklySummary(ledger: Ledger, today: LocalDate) {
        val s = ledger.settings
        var week = Periods.week(today, s.weekStartsSunday)
        if (week.start == today) week = Periods.week(today.minusDays(1), s.weekStartsSunday)
        val sum = ledger.summary(week)
        val title = "Sua semana rendeu ${Fmt.money(sum.gross)}"
        val text = buildString {
            append("${Fmt.shortRange(week)}: ${Fmt.daysDecimal(sum.workedEquivalent)} trabalhados")
            if (sum.totalDeductions > 0) append(" · líquido ${Fmt.money(sum.net)}")
            if (sum.absences > 0) append(" · ${sum.absences} ${if (sum.absences == 1) "falta" else "faltas"}")
        }
        Notifications.showText(applicationContext, Notifications.ID_WEEKLY, Notifications.CHANNEL_SUMMARY, title, text)
    }

    private fun vacationAlert(ledger: Ledger, today: LocalDate) {
        if (!ledger.employmentIsClt()) return
        val info = ledger.vacation() ?: return
        val p = info.pending.firstOrNull() ?: return
        val left = ChronoUnit.DAYS.between(today, p.deadline)
        if (left == 60L || left == 30L || left == 7L) {
            Notifications.showText(
                applicationContext,
                Notifications.ID_VACATION,
                Notifications.CHANNEL_REMINDERS,
                "Prazo das férias em $left dias",
                "Você tem ${Fmt.days(p.remaining)} de férias para tirar até ${Fmt.ddmmyyyy(p.deadline)}.",
            )
        }
    }
}

/** Ação "Fui trabalhar" da notificação: marca o dia sem abrir o app. */
class MarkWorkedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as JornadaApp
        val date = intent.getStringExtra(EXTRA_DATE) ?: LocalDate.now().toString()
        Thread {
            try {
                app.repository.updateNow { d ->
                    if (d.days.containsKey(date)) d
                    else d.copy(days = d.days + (date to DayRecord(date, DayStatus.WORKED)))
                }
                NotificationManagerCompat.from(context).cancel(Notifications.ID_DAILY)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val EXTRA_DATE = "date"
    }
}
