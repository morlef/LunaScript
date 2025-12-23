package si.f5.luna3419.script.ui.editors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.resources.ResourceManager
import si.f5.luna3419.script.ui.components.SuggestionTextField
import si.f5.luna3419.script.ui.components.TimeWithEasingField

@Composable
fun BgmBlockEditor(
    scene: SceneData,
    onUpdate: (SceneData) -> Unit,
    getAssetSuggestions: (String) -> List<String>,
) {
    val isPlay = scene.type == SceneType.BGM_PLAY

    Column {
        var expanded by remember { mutableStateOf(false) }
        Box {
            Text(
                text = if (isPlay) "Action: Play" else "Action: Stop",
                modifier = Modifier.clickable { expanded = true }.padding(4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Play") }, onClick = {
                    onUpdate(scene.copy(type = SceneType.BGM_PLAY, commandName = SceneType.BGM_PLAY.value))
                    expanded = false
                })
                DropdownMenuItem(text = { Text("Stop") }, onClick = {
                    onUpdate(scene.copy(type = SceneType.BGM_STOP, commandName = SceneType.BGM_STOP.value))
                    expanded = false
                })
            }
        }

        fun getParam(index: Int): String = scene.commandParams.getOrNull(index) ?: ""
        fun setParam(index: Int, value: String) {
            val newParams = scene.commandParams.toMutableList()
            while (newParams.size <= index) newParams.add("")
            newParams[index] = value
            onUpdate(scene.copy(commandParams = newParams))
        }

        if (isPlay) {
            SuggestionTextField(
                value = getParam(0),
                onValueChange = { setParam(0, it) },
                label = "File",
                suggestions = getAssetSuggestions(ResourceManager.TYPE_BGM),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = getParam(1),
                onValueChange = { setParam(1, it) },
                label = { Text("Loop (true/false)") },
                modifier = Modifier.fillMaxWidth()
            )
            TimeWithEasingField(
                value = getParam(2),
                onValueChange = { setParam(2, it) },
                label = "Fade Time(ms)",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TimeWithEasingField(
                value = getParam(0),
                onValueChange = { setParam(0, it) },
                label = "Fade Time(ms)",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SeBlockEditor(
    scene: SceneData, onUpdate: (SceneData) -> Unit, getAssetSuggestions: (String) -> List<String>
) {
    fun getParam(index: Int): String = scene.commandParams.getOrNull(index) ?: ""
    fun setParam(index: Int, value: String) {
        val newParams = scene.commandParams.toMutableList()
        while (newParams.size <= index) newParams.add("")
        newParams[index] = value
        onUpdate(scene.copy(commandParams = newParams))
    }

    Column {
        SuggestionTextField(
            value = getParam(0),
            onValueChange = { setParam(0, it) },
            label = "File",
            suggestions = getAssetSuggestions(ResourceManager.TYPE_SE),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
