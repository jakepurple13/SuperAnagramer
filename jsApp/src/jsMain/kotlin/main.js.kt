import androidx.compose.ui.window.Window
import com.programmersbox.common.UIShow
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window("Anagramer") {
            UIShow()
        }
    }
}