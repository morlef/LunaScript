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
fun ScreenEffectBlockEditor(scene: SceneData, onUpdate: (SceneData) -> Unit) {
    Column {
        var expanded by remember { mutableStateOf(false) }
        Box {
            Text(
                text = "Type: ${scene.type.name}",
                modifier = Modifier.clickable { expanded = true }.padding(4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(SceneType.SHAKE, SceneType.FLASH).forEach { type ->
                    DropdownMenuItem(text = { Text(type.name) }, onClick = {
                        onUpdate(scene.copy(type = type, commandName = type.value))
                        expanded = false
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

        if (scene.type == SceneType.SHAKE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = getParam(0),
                    onValueChange = { setParam(0, it) },
                    label = { Text("Intensity") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = getParam(1),
                    onValueChange = { setParam(1, it) },
                    label = { Text("Time(ms)") },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = getParam(0),
                    onValueChange = { setParam(0, it) },
                    label = { Text("Color") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = getParam(1),
                    onValueChange = { setParam(1, it) },
                    label = { Text("Time(ms)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ObjectEffectBlockEditor(
    scene: SceneData,
    onUpdate: (SceneData) -> Unit,
    getAssetSuggestions: (String) -> List<String>,
    onEditCoordinates: () -> Unit
) {
    Column {
        var expanded by remember { mutableStateOf(false) }
        Box {
            Text(
                text = "Type: ${scene.type.name}",
                modifier = Modifier.clickable { expanded = true }.padding(4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(
                    SceneType.MOVE, SceneType.SCALE, SceneType.ROTATE, SceneType.SHAKE_OBJ, SceneType.TINT
                ).forEach { type ->
                    DropdownMenuItem(text = { Text(type.name) }, onClick = {
                        onUpdate(scene.copy(type = type, commandName = type.value))
                        expanded = false
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
            label = "Target ID",
            suggestions = getAssetSuggestions(ResourceManager.TYPE_CHARACTER),
            modifier = Modifier.fillMaxWidth()
        )

        when (scene.type) {
            SceneType.MOVE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = getParam(1),
                        onValueChange = { setParam(1, it) },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = getParam(2),
                        onValueChange = { setParam(2, it) },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditCoordinates) {
                        Icon(Icons.Filled.OpenWith, "Set Coordinates")
                    }
                }
                OutlinedTextField(
                    value = getParam(3),
                    onValueChange = { setParam(3, it) },
                    label = { Text("Time(ms)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = getParam(4),
                    onValueChange = { setParam(4, it) },
                    label = { Text("Easing") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SceneType.SCALE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = getParam(1),
                        onValueChange = { setParam(1, it) },
                        label = { Text("Scale X(%)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = getParam(2),
                        onValueChange = { setParam(2, it) },
                        label = { Text("Scale Y(%)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                TimeWithEasingField(
                    value = getParam(3),
                    onValueChange = { setParam(3, it) },
                    label = "Time(ms)",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SceneType.ROTATE -> {
                OutlinedTextField(
                    value = getParam(1),
                    onValueChange = { setParam(1, it) },
                    label = { Text("Angle") },
                    modifier = Modifier.fillMaxWidth()
                )
                TimeWithEasingField(
                    value = getParam(2),
                    onValueChange = { setParam(2, it) },
                    label = "Time(ms)",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SceneType.SHAKE_OBJ -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = getParam(1),
                        onValueChange = { setParam(1, it) },
                        label = { Text("Intensity") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    TimeWithEasingField(
                        value = getParam(2),
                        onValueChange = { setParam(2, it) },
                        label = "Time(ms)",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SceneType.TINT -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = getParam(1),
                        onValueChange = { setParam(1, it) },
                        label = { Text("Color/R") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = getParam(2),
                        onValueChange = { setParam(2, it) },
                        label = { Text("G") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = getParam(3),
                        onValueChange = { setParam(3, it) },
                        label = { Text("B") },
                        modifier = Modifier.weight(1f)
                    )
                }
                TimeWithEasingField(
                    value = getParam(4),
                    onValueChange = { setParam(4, it) },
                    label = "Time(ms)",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {}
        }
    }
}

@Composable
fun BgBlockEditor(
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
            label = "Image File",
            suggestions = listOf("_ (Transparent)") + getAssetSuggestions(ResourceManager.TYPE_BG),
            modifier = Modifier.fillMaxWidth()
        )
        TimeWithEasingField(
            value = getParam(1),
            onValueChange = { setParam(1, it) },
            label = "Fade(ms)",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
