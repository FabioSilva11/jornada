package app.jornada.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Guarda tudo em um único arquivo JSON no armazenamento interno do app.
 * O mesmo formato serve para backup e restauração.
 */
class Repository(private val file: File) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Any()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData {
        if (!file.exists()) return AppData()
        return try {
            decode(file.readText())
        } catch (e: Exception) {
            // Não sobrescreve um arquivo que não conseguimos ler: guarda uma cópia antes de começar do zero.
            file.copyTo(File(file.parentFile, file.name + ".corrompido"), overwrite = true)
            AppData()
        }
    }

    fun update(transform: (AppData) -> AppData) {
        _data.updateAndGet(transform)
        scope.launch { persist() }
    }

    /** Atualiza e grava antes de retornar; para uso fora da interface (notificações). */
    fun updateNow(transform: (AppData) -> AppData) {
        _data.updateAndGet(transform)
        persist()
    }

    private fun persist() {
        synchronized(writeLock) {
            val text = encode(_data.value)
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                file.writeText(text)
                tmp.delete()
            }
        }
    }

    fun exportJson(): String = encode(_data.value)

    /** Substitui todos os dados pelo conteúdo do backup. Lança exceção se o arquivo não for válido. */
    fun importJson(text: String) {
        val imported = decode(text)
        update { imported }
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
            prettyPrint = false
        }

        fun encode(data: AppData): String = json.encodeToString(AppData.serializer(), data)

        fun decode(text: String): AppData = json.decodeFromString(AppData.serializer(), text)
    }
}
