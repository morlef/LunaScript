package si.f5.luna3419.script.ui.editors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.ui.EditorViewModel
import si.f5.luna3419.script.ui.components.DropZone
import si.f5.luna3419.script.ui.components.SceneBlock

@Composable
fun IfBlockEditor(
    scene: SceneData,
    onUpdate: (SceneData) -> Unit,
    getAssetSuggestions: (String) -> List<String>,
    onEditCoordinates: () -> Unit,
    viewModel: EditorViewModel
) {
    fun getParam(index: Int): String = scene.commandParams.getOrNull(index) ?: ""
    fun setParam(index: Int, value: String) {
        val newParams = scene.commandParams.toMutableList()
        while (newParams.size <= index) newParams.add("")
        newParams[index] = value
        onUpdate(scene.copy(commandParams = newParams))
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = getParam(0),
                onValueChange = { setParam(0, it) },
                label = { Text("Var") },
                modifier = Modifier.weight(1f)
            )

            Box(modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)) {
                var expanded by remember { mutableStateOf(false) }
                val currentOp = getParam(1).ifEmpty { "==" }

                OutlinedTextField(
                    value = currentOp,
                    onValueChange = { setParam(1, it) },
                    label = { Text("Op") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(Modifier.matchParentSize().clickable { expanded = true })

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("==", "!=", ">", "<", ">=", "<=").forEach { op ->
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
        Spacer(Modifier.height(8.dp))

        Text("True Block", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                DropZone(
                    target = EditorViewModel.DropTarget.Nested(scene.uuid, true, 0), viewModel = viewModel
                ) { viewModel.onPaletteDrop() }

                scene.trueBlock.forEachIndexed { index, subScene ->
                    key(subScene.uuid) {
                        SceneBlock(
                            scene = subScene,
                            onUpdate = { updated ->
                                val list = scene.trueBlock.toMutableList()
                                list[index] = updated
                                onUpdate(scene.copy(trueBlock = list))
                            },
                            onDelete = {
                                val list = scene.trueBlock.toMutableList()
                                list.removeAt(index)
                                onUpdate(scene.copy(trueBlock = list))
                            },
                            getAssetSuggestions = getAssetSuggestions,
                            characterNames = listOf(),
                            onDebug = {},
                            onEditCoordinates = onEditCoordinates,
                            isDebugActive = false,
                            viewModel = viewModel
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    DropZone(
                        target = EditorViewModel.DropTarget.Nested(scene.uuid, true, index + 1), viewModel = viewModel
                    ) { viewModel.onPaletteDrop() }
                }
                Button(onClick = {
                    val list = scene.trueBlock.toMutableList()
                    list.add(SceneData(type = SceneType.TEXT, text = "New Text"))
                    onUpdate(scene.copy(trueBlock = list))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Add True Action")
                }
            }
        }

        Text("False Block", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                DropZone(
                    target = EditorViewModel.DropTarget.Nested(scene.uuid, false, 0), viewModel = viewModel
                ) { viewModel.onPaletteDrop() }

                scene.falseBlock.forEachIndexed { index, subScene ->
                    key(subScene.uuid) {
                        SceneBlock(
                            scene = subScene,
                            onUpdate = { updated ->
                                val list = scene.falseBlock.toMutableList()
                                list[index] = updated
                                onUpdate(scene.copy(falseBlock = list))
                            },
                            onDelete = {
                                val list = scene.falseBlock.toMutableList()
                                list.removeAt(index)
                                onUpdate(scene.copy(falseBlock = list))
                            },
                            getAssetSuggestions = getAssetSuggestions,
                            characterNames = listOf(),
                            onDebug = {},
                            onEditCoordinates = onEditCoordinates,
                            isDebugActive = false,
                            viewModel = viewModel
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    DropZone(
                        target = EditorViewModel.DropTarget.Nested(scene.uuid, false, index + 1), viewModel = viewModel
                    ) { viewModel.onPaletteDrop() }
                }
                Button(onClick = {
                    val list = scene.falseBlock.toMutableList()
                    list.add(SceneData(type = SceneType.TEXT, text = "New Text"))
                    onUpdate(scene.copy(falseBlock = list))
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Add False Action")
                }
            }
        }
    }
}
