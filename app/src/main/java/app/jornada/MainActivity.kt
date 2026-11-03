package app.jornada

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.jornada.ui.AppViewModel
import app.jornada.ui.JornadaRoot

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { JornadaRoot(vm) }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshToday()
    }
}
