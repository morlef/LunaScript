package si.f5.luna3419.script.ui.editors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.data.SceneData

@Composable
fun SetBlockEditor(scene: SceneData, onUpdate: (SceneData) -> Unit) {
    fun getParam(index: Int): String = scene.commandParams.getOrNull(index) ?: ""
    fun setParam(index: Int, value: String) {
        val newParams = scene.commandParams.toMutableList()
        while (newParams.size <= index) newParams.add("")
        newParams[index] = value
        onUpdate(scene.copy(commandParams = newParams))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = getParam(0),
            onValueChange = { setParam(0, it) },
            label = { Text("Variable ($...)") },
            modifier = Modifier.weight(1f)
        )

        Box(modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)) {
            var expanded by remember { mutableStateOf(false) }
            val currentOp = getParam(1).ifEmpty { "=" }

            OutlinedTextField(
                value = currentOp,
                onValueChange = { setParam(1, it) },
                label = { Text("Op") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Box(Modifier.matchParentSize().clickable { expanded = true })

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("=", "+=", "-=").forEach { op ->
                    DropdownMenuItem(text = { Text(op) }, onClick = {
                        setParam(1, op)
                        expanded = false
                    })
                }
            }
        }

        OutlinedTextField(
            value = getParam(2),
            onValueChange = { setParam(2, it) },
            label = { Text("Value") },
            modifier = Modifier.weight(1f)
        )
    }
}
