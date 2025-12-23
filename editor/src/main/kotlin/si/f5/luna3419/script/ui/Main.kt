package si.f5.luna3419.script.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import java.io.File
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.player.PlayerScreen
import si.f5.luna3419.script.ui.components.performSafeExit
import si.f5.luna3419.script.ui.screens.EditorScreen
import si.f5.luna3419.script.ui.screens.CoordinateEditorScreen
import si.f5.luna3419.script.ui.theme.AppTheme
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.WindowConstants


fun main() = application {
    val fontFile =
        File(System.getProperty("compose.application.resources.dir"), "fonts/noto-sans-jp-japanese-300-normal.ttf")
    val actualFontFile =
        if (fontFile.exists()) fontFile else File("src/main/resources/fonts/noto-sans-jp-japanese-300-normal.ttf")

    val fontFamily = if (actualFontFile.exists()) {
        FontFamily(Font(actualFontFile))
    } else {
        FontFamily.Default
    }

    val typography = androidx.compose.material3.Typography(
        bodyLarge = TextStyle(fontFamily = fontFamily),
        bodyMedium = TextStyle(fontFamily = fontFamily),
        labelLarge = TextStyle(fontFamily = fontFamily)
    )

    val editorViewModel = remember { EditorViewModel() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "LunaScript Editor",
        undecorated = true,
        transparent = false,
        onKeyEvent = {
            if (it.isCtrlPressed && it.key == Key.N) {
                false
            } else {
                false
            }
        }) {
        val window = this.window

        LaunchedEffect(Unit) {
            window.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            window.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    performSafeExit(editorViewModel, window)
                }
            })
        }

        AppTheme(
            darkTheme = editorViewModel.isDarkTheme, typography = typography
        ) {
            EditorScreen(editorViewModel, window)
        }
    }

    val debugVM = editorViewModel.debugPlayerViewModel
    if (debugVM != null) {
        Window(
            onCloseRequest = {
                debugVM.dispose()
                editorViewModel.debugPlayerViewModel = null
            }, title = "LunaScript Debug Player", resizable = false,

            state = rememberWindowState(width = 1176.dp, height = 696.dp)
        ) {
            LaunchedEffect(debugVM.shouldClose) {
                if (debugVM.shouldClose) {
                    debugVM.dispose()
                    editorViewModel.debugPlayerViewModel = null
                }
            }
            PlayerScreen(debugVM)
        }
    }

    val coordUuid = editorViewModel.coordinateEditingBlockUuid
    if (coordUuid != null) {
        Window(
            onCloseRequest = { editorViewModel.closeCoordinateEditor() },
            title = "Coordinate Editor",
            resizable = false,
            state = rememberWindowState(width = 1176.dp, height = 696.dp)
        ) {
            CoordinateEditorScreen(editorViewModel, coordUuid)
        }
    }
}
