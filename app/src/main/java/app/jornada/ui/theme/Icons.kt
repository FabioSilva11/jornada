package app.jornada.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Ícones de traço (24×24, traço 2) desenhados para o app, os mesmos da direção de design. */
object JIcons {
    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            for (p in paths) {
                addPath(
                    pathData = addPathNodes(p),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
        "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}" +
            "a$rx $rx 0 0 1 ${-rx} ${rx}h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}" +
            "v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}z"

    val Home by lazy { icon("home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z") }
    val Calendar by lazy { icon("calendar", rect(3f, 5f, 18f, 16f, 2f), "M3 10h18M8 3v4M16 3v4") }
    val CalendarCheck by lazy {
        icon("calendar_check", rect(3f, 5f, 18f, 16f, 3f), "M3 10h18M8 3v4M16 3v4", "M8.5 15.2l2.4 2.4 4.6-4.6")
    }
    val Wallet by lazy {
        icon(
            "wallet",
            "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1",
            "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4",
        )
    }
    val Sun by lazy {
        icon(
            "sun",
            circle(12f, 12f, 4f),
            "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4",
        )
    }
    val Gear by lazy {
        icon(
            "gear",
            circle(12f, 12f, 3f),
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 " +
                "1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06" +
                "a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09" +
                "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68" +
                "a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06" +
                "a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09" +
                "a1.65 1.65 0 0 0-1.51 1z",
        )
    }
    val Check by lazy { icon("check", "M5 12.5l4.5 4.5L19 7.5") }
    val Plus by lazy { icon("plus", "M12 5v14M5 12h14") }
    val ChevronLeft by lazy { icon("chevron_left", "M15 6l-6 6 6 6") }
    val ChevronRight by lazy { icon("chevron_right", "M9 6l6 6-6 6") }
    val ChevronDown by lazy { icon("chevron_down", "M6 9l6 6 6-6") }
    val Back by lazy { icon("back", "M19 12H5M11 6l-6 6 6 6") }
    val Forward by lazy { icon("forward", "M5 12h14M13 6l6 6-6 6") }
    val Chart by lazy { icon("chart", "M4 20V11M10 20V5M16 20v-6M3 20h18") }
    val Bell by lazy { icon("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0") }
    val Close by lazy { icon("close", "M6 6l12 12M18 6 6 18") }
    val TrendUp by lazy { icon("trend_up", "M3 17l6-6 4 4 8-8M15 7h6v6") }
    val TrendDown by lazy { icon("trend_down", "M3 7l6 6 4-4 8 8M15 17h6v-6") }
    val Share by lazy { icon("share", "M12 3v12M7 8l5-5 5 5M5 14v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5") }
    val Info by lazy { icon("info", circle(12f, 12f, 9f), "M12 11v6M12 7.5v.01") }
    val Clock by lazy { icon("clock", circle(12f, 12f, 9f), "M12 7v5l3 2") }
    val User by lazy { icon("user", circle(12f, 8f, 4f), "M4 21c0-4 4-6 8-6s8 2 8 6") }
    val Flag by lazy { icon("flag", "M5 21V4M5 4h12l-2.5 4L17 12H5") }
    val Receipt by lazy { icon("receipt", "M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2z", "M9 8h6M9 12h6") }
    val Moon by lazy { icon("moon", "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z") }
    val Download by lazy { icon("download", "M12 3v12M7 10l5 5 5-5M5 21h14") }
    val Upload by lazy { icon("upload", "M12 21V9M7 14l5-5 5 5M5 3h14") }
    val Table by lazy { icon("table", rect(4f, 3f, 16f, 18f, 2f), "M4 9h16M4 15h16M10 3v18") }
    val Trash by lazy { icon("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3") }
    val Bus by lazy {
        icon("bus", rect(4f, 3f, 16f, 14f, 3f), "M4 10h16M7.5 21v-2M16.5 21v-2M8 13.5h.01M16 13.5h.01")
    }
    val Utensils by lazy { icon("utensils", "M7 3v8M5 3v5a2 2 0 0 0 4 0V3M7 11v10M18 3c-2 0-3.5 2-3.5 5.5S16 13 18 13v8") }
    val Banknote by lazy { icon("banknote", rect(2.5f, 6f, 19f, 12f, 2f), circle(12f, 12f, 2.5f), "M6 12h.01M18 12h.01") }
    val Shirt by lazy { icon("shirt", "M8 3 3 6l2 5 3-1v11h8V10l3 1 2-5-5-3c0 2-1.8 3.2-4 3.2S8 5 8 3z") }
    val Coin by lazy {
        icon(
            "coin",
            circle(12f, 12f, 9f),
            "M14.5 9.5c-.5-1-1.5-1.5-2.5-1.5-1.5 0-2.5.8-2.5 2s1 1.7 2.5 2 2.5.8 2.5 2-1 2-2.5 2c-1 0-2-.5-2.5-1.5M12 6.5V8M12 16v1.5",
        )
    }
    val Tag by lazy { icon("tag", "M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9z", circle(7.5f, 7.5f, 1.5f)) }
    val Edit by lazy { icon("edit", "M4 20h4L19 9l-4-4L4 16z", "M13.5 6.5l4 4") }
    val Flame by lazy {
        icon("flame", "M12 22c4 0 7-3 7-7 0-5-5-7-5-12-3 2-4 5-4 7-1-1-2-2-2-4-2 2-3 5-3 9 0 4 3 7 7 7z")
    }
    val Pause by lazy { icon("pause", "M8 5v14M16 5v14") }
}
