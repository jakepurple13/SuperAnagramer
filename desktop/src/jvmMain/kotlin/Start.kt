import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.programmersbox.common.UIShow

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        UIShow()
    }
}
