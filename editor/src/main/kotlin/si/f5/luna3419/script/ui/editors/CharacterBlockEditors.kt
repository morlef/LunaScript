package si.f5.luna3419.script.ui.editors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.resources.ResourceManager
import si.f5.luna3419.script.ui.components.SuggestionTextField
import si.f5.luna3419.script.ui.components.TimeWithEasingField

@Composable
fun CharacterBlockEditor(
    scene: SceneData,
    onUpdate: (SceneData) -> Unit,
    getAssetSuggestions: (String) -> List<String>,
    onEditCoordinates: () -> Unit,
    getCharacterIdSuggestions: () -> List<String> = { emptyList() },
    getCharacterFaceSuggestions: (String) -> List<String> = { emptyList() }
) {
    val isChara = scene.type == SceneType.CHARA_IN || scene.type == SceneType.CHARA_OUT
    val isShow = scene.type == SceneType.CHARA_IN || scene.type == SceneType.IMAGE_IN

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            var expandedAction by remember { mutableStateOf(false) }
            Box {
                Text(
                    text = if (isShow) "Action: Show" else "Action: Hide",
                    modifier = Modifier.clickable { expandedAction = true }.padding(4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                DropdownMenu(expanded = expandedAction, onDismissRequest = { expandedAction = false }) {
                    DropdownMenuItem(text = { Text("Show") }, onClick = {
                        val newType = if (isChara) SceneType.CHARA_IN else SceneType.IMAGE_IN
                        onUpdate(scene.copy(type = newType, commandName = newType.value))
                        expandedAction = false
                    })
                    DropdownMenuItem(text = { Text("Hide") }, onClick = {
                        val newType = if (isChara) SceneType.CHARA_OUT else SceneType.IMAGE_OUT
                        onUpdate(scene.copy(type = newType, commandName = newType.value))
                        expandedAction = false
                    })
                }
            }
        }

        fun getParam(index: Int): String = scene.commandParams.getOrNull(index) ?: ""
        fun setParam(index: Int, value: String) {
            val newParams = scene.commandParams.toMutableList()
            while (newParams.size <= index) newParams.add("")
            newParams[index] = value
            onUpdate(scene.copy(commandParams = newParams))
        }

        SuggestionTextField(
            value = getParam(0),
            onValueChange = { setParam(0, it) },
            label = "ID",
            suggestions = if (isChara) getCharacterIdSuggestions() else getAssetSuggestions(ResourceManager.TYPE_IMAGE),
            modifier = Modifier.fillMaxWidth()
        )

        if (isShow) {
            if (isChara) {
                SuggestionTextField(
                    value = getParam(1),
                    onValueChange = { setParam(1, it) },
                    label = "Face/File",
                    suggestions = getCharacterFaceSuggestions(getParam(0)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val xIndex = if (isChara) 2 else 1
            val yIndex = if (isChara) 3 else 2

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = getParam(xIndex),
                    onValueChange = { setParam(xIndex, it) },
                    label = { Text("X") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = getParam(yIndex),
                    onValueChange = { setParam(yIndex, it) },
                    label = { Text("Y") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditCoordinates) {
                    Icon(Icons.Filled.OpenWith, "Set Coordinates")
                }
            }
            TimeWithEasingField(
                value = getParam(4),
                onValueChange = { setParam(4, it) },
                label = "Fade Time(ms)",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TimeWithEasingField(
                value = getParam(1),
                onValueChange = { setParam(1, it) },
                label = "Fade Time(ms)",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
