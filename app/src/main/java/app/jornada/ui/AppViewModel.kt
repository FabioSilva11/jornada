package app.jornada.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import app.jornada.JornadaApp
import app.jornada.data.AppData
import app.jornada.data.DayRecord
import app.jornada.data.DayStatus
import app.jornada.data.Deduction
import app.jornada.data.Settings
import app.jornada.data.VacationPlan
import app.jornada.domain.Fmt
import app.jornada.domain.Ledger
import app.jornada.notify.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as JornadaApp).repository

    val data: StateFlow<AppData> = repo.data

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** Dia aberto na folha "Registrar dia". */
    var editingDate by mutableStateOf<LocalDate?>(null)
        private set

    fun refreshToday() {
        val now = LocalDate.now()
        if (now != _today.value) _today.value = now
    }

    fun openDay(date: LocalDate) {
        editingDate = date
    }

    fun closeDay() {
        editingDate = null
    }

    // ---------- Dias ----------

    fun setDay(rec: DayRecord) = repo.update { it.copy(days = it.days + (rec.date to rec)) }

    fun clearDay(date: LocalDate) = repo.update { it.copy(days = it.days - date.toString()) }

    /** Restaura o registro anterior (desfazer). */
    fun restoreDay(date: LocalDate, previous: DayRecord?) {
        if (previous == null) clearDay(date) else setDay(previous)
    }

    fun setDays(dates: Collection<LocalDate>, status: DayStatus?) = repo.update { d ->
        val days = d.days.toMutableMap()
        for (date in dates) {
            val key = date.toString()
            if (status == null) {
                days.remove(key)
            } else {
                val old = days[key]
                days[key] = DayRecord(key, status, extra = old?.extra ?: 0.0, note = old?.note ?: "")
            }
        }
        d.copy(days = days)
    }

    // ---------- Ajustes ----------

    fun updateSettings(transform: (Settings) -> Settings) {
        repo.update { it.copy(settings = transform(it.settings)) }
        Reminders.schedule(getApplication(), repo.data.value.settings)
    }

    // ---------- Descontos ----------

    fun upsertDeduction(d: Deduction) = repo.update { data ->
        val list = data.deductions.toMutableList()
        val i = list.indexOfFirst { it.id == d.id }
        if (i >= 0) list[i] = d else list += d
        data.copy(deductions = list)
    }

    fun deleteDeduction(id: String) = repo.update { it.copy(deductions = it.deductions.filterNot { d -> d.id == id }) }

    // ---------- Férias ----------

    fun addVacation(plan: VacationPlan) = repo.update { d ->
        val days = d.days.toMutableMap()
        val start = LocalDate.parse(plan.start)
        for (i in 0 until plan.days) {
            val key = start.plusDays(i.toLong()).toString()
            days[key] = DayRecord(key, DayStatus.VACATION, note = days[key]?.note ?: "")
        }
        d.copy(days = days, vacations = d.vacations + plan)
    }

    fun deleteVacation(id: String) = repo.update { d ->
        val plan = d.vacations.firstOrNull { it.id == id } ?: return@update d
        val days = d.days.toMutableMap()
        val start = LocalDate.parse(plan.start)
        for (i in 0 until plan.days) {
            val key = start.plusDays(i.toLong()).toString()
            if (days[key]?.status == DayStatus.VACATION) days.remove(key)
        }
        d.copy(days = days, vacations = d.vacations.filterNot { it.id == id })
    }

    // ---------- Dados ----------

    fun exportJson(): String = repo.exportJson()

    fun markBackup() = updateSettings { it.copy(lastBackup = LocalDate.now().toString()) }

    fun importJson(text: String): Boolean = try {
        repo.importJson(text)
        Reminders.schedule(getApplication(), repo.data.value.settings)
        true
    } catch (e: Exception) {
        false
    }

    fun wipe() {
        repo.update { AppData() }
        Reminders.schedule(getApplication(), repo.data.value.settings)
    }

    fun csv(): String {
        val d = repo.data.value
        val ledger = Ledger(d, LocalDate.now())
        val sb = StringBuilder("﻿")
        sb.append("Data;Dia;Status;Valor do dia;Extra;Total;Observação\n")
        for (rec in d.days.values.sortedBy { it.date }) {
            val date = rec.localDate
            val base = ledger.baseValue(rec)
            sb.append(Fmt.ddmmyyyy(date)).append(';')
                .append(Fmt.weekday(date)).append(';')
                .append(Fmt.status(rec.status)).append(';')
                .append(Fmt.number(base, 2)).append(';')
                .append(Fmt.number(rec.extra, 2)).append(';')
                .append(Fmt.number(base + rec.extra, 2)).append(';')
                .append(rec.note.replace(';', ',').replace('\n', ' '))
                .append('\n')
        }
        return sb.toString()
    }
}
