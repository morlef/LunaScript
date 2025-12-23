package si.f5.luna3419.script.ui.editors

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.ui.components.SuggestionTextField

@Composable
fun TextBlockEditor(scene: SceneData, onUpdate: (SceneData) -> Unit, characterNames: List<String>) {
    Column {
        SuggestionTextField(
            value = scene.charName,
            onValueChange = { onUpdate(scene.copy(charName = it)) },
            label = "Name",
            suggestions = characterNames,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = scene.text,
            onValueChange = { onUpdate(scene.copy(text = it)) },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth().height(80.dp),
        )
    }
}

@Composable
fun WaitBlockEditor(scene: SceneData, onUpdate: (SceneData) -> Unit) {
    val isClick = scene.type == SceneType.WAIT_CLICK

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = scene.commandParams.getOrNull(0) ?: "", onValueChange = {
                val newParams = scene.commandParams.toMutableList()
                if (newParams.isEmpty()) newParams.add(it) else newParams[0] = it
                onUpdate(scene.copy(commandParams = newParams))
            }, label = { Text("Time(ms)") }, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isClick, onCheckedChange = { checked ->
                        if (checked) {
                            onUpdate(scene.copy(type = SceneType.WAIT_CLICK, commandName = SceneType.WAIT_CLICK.value))
                        } else {
                            onUpdate(scene.copy(type = SceneType.WAIT, commandName = SceneType.WAIT.value))
                        }
                    })
                Text("Wait Click")
            }
        }
    }
}
