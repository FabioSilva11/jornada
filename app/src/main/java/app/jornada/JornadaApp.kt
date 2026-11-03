package app.jornada

import android.app.Application
import app.jornada.data.Repository
import app.jornada.notify.Notifications
import app.jornada.notify.Reminders
import java.io.File

class JornadaApp : Application() {

    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(File(filesDir, "jornada.json"))
        Notifications.createChannels(this)
        Reminders.schedule(this, repository.data.value.settings)
    }
}
