package si.f5.luna3419.script.ui.editors

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType

@Composable
fun GenericCommandEditor(
    scene: SceneData, onUpdate: (SceneData) -> Unit
) {
    Column {
        when (scene.type) {
            SceneType.CHOICE -> {
                Text("Choices", style = MaterialTheme.typography.labelSmall)
                val choices = scene.choices.toMutableList()
                choices.forEachIndexed { i, (text, label) ->
                    Row {
                        OutlinedTextField(value = text, onValueChange = { v ->
                            choices[i] = v to label
                            onUpdate(scene.copy(choices = choices))
                        }, label = { Text("Text") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = label, onValueChange = { v ->
                            choices[i] = text to v
                            onUpdate(scene.copy(choices = choices))
                        }, label = { Text("Label") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            choices.removeAt(i)
                            onUpdate(scene.copy(choices = choices))
                        }) { Icon(Icons.Filled.Close, null) }
                    }
                }
                Button(onClick = {
                    choices.add("" to "")
                    onUpdate(scene.copy(choices = choices))
                }) { Text("Add Choice") }
            }

            else -> {
                OutlinedTextField(
                    value = scene.commandParams.joinToString(" "), onValueChange = {
                    onUpdate(scene.copy(commandParams = it.split(" ").toMutableList()))
                }, label = { Text("Command Params") }, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
