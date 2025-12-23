package si.f5.luna3419.script.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign

import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.ui.components.*
import si.f5.luna3419.script.ui.EditorViewModel

@Composable
fun EditorScreen(viewModel: EditorViewModel, window: ComposeWindow) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        keyEvent.isCtrlPressed && keyEvent.key == Key.N -> {
                            confirmSaveIfNeeded(viewModel, window) {
                                viewModel.createNewFile()
                            }
                            true
                        }

                        keyEvent.isCtrlPressed && keyEvent.key == Key.O -> {
                            confirmSaveIfNeeded(viewModel, window) {
                                val file = chooseFile(
                                    save = false, initialDirectory = viewModel.currentFile?.parentFile
                                )
                                if (file != null) {
                                    viewModel.createNewFile()
                                    viewModel.loadFile(file)
                                }
                            }
                            true
                        }

                        keyEvent.isCtrlPressed && keyEvent.key == Key.S -> {
                            if (viewModel.currentFile == null) {
                                val file = chooseFile(
                                    save = true, initialDirectory = viewModel.currentFile?.parentFile
                                )
                                if (file != null) viewModel.saveAs(file)
                            } else {
                                viewModel.saveFile()
                            }
                            true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }.focusRequester(focusRequester).focusable()) {
            CustomTitleBar(viewModel, window)

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(
                    modifier = Modifier.width(220.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(end = 1.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        PaletteView(viewModel)
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                    GroupListContainer(viewModel, window)
                }
            }
        }

        if (viewModel.draggingPaletteType != null || viewModel.draggingBlock != null) {
            val type = viewModel.draggingPaletteType ?: viewModel.draggingBlock?.type
            if (type != null) {
                Box(Modifier.offset { IntOffset(viewModel.dragOffset.x.toInt(), viewModel.dragOffset.y.toInt()) }
                    .width(220.dp).zIndex(100f)) {
                    DraggablePaletteItem(type, viewModel, isGhost = true)
                }
            }
        }
    }
}


@Composable
fun GroupListContainer(viewModel: EditorViewModel, window: ComposeWindow) {
    var activeGroupIndex by remember { mutableStateOf<Int?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {


            if (viewModel.groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    DropZone(
                        target = EditorViewModel.DropTarget.Root(-1, -1),
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    ) {}

                    if (viewModel.draggingPaletteType == null && viewModel.draggingBlock == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Drag from Palette to Create First Scene",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "OR",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                confirmSaveIfNeeded(viewModel, window) {
                                    val file =
                                        chooseFile(save = false, initialDirectory = viewModel.currentFile?.parentFile)
                                    if (file != null) {
                                        viewModel.createNewFile()
                                        viewModel.loadFile(file)
                                    }
                                }
                            }) {
                                Text("Open Script File")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    itemsIndexed(viewModel.groups) { index, group ->
                        val isActive = activeGroupIndex == index
                        SceneGroupCard(group, viewModel, isActive, index) { activeGroupIndex = index }
                    }
                    item {
                        Button(
                            onClick = { viewModel.addGroup(); activeGroupIndex = viewModel.groups.lastIndex },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Text("Add Label")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SceneGroupCard(
    group: EditorViewModel.SceneGroup,
    viewModel: EditorViewModel,
    isActive: Boolean,
    groupIndex: Int,
    onSelect: () -> Unit
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(vertical = 4.dp)) {
                Text("Label:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = group.id,
                    onValueChange = { group.id = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { viewModel.removeGroup(group) }) {
                    Icon(Icons.Filled.Delete, "Delete Group", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                DropZone(
                    target = EditorViewModel.DropTarget.Root(groupIndex, 0), viewModel = viewModel
                ) { viewModel.onPaletteDrop() }

                group.blocks.forEachIndexed { index, block ->
                    key(block.uuid) {
                        SceneBlock(
                            scene = block,
                            onUpdate = { viewModel.updateBlock(group, index, it) },
                            onDelete = { viewModel.removeBlockFromGroup(group, block) },
                            getAssetSuggestions = viewModel::getAssetSuggestions,
                            characterNames = viewModel.characterNames,
                            onDebug = { viewModel.debugBlock(block) },
                            onEditCoordinates = { viewModel.openCoordinateEditor(block.uuid) },
                            isDebugActive = (block.uuid == viewModel.debugPlayerViewModel?.currentSceneUuid),
                            viewModel = viewModel
                        )
                    }

                    DropZone(
                        target = EditorViewModel.DropTarget.Root(groupIndex, index + 1), viewModel = viewModel
                    ) { viewModel.onPaletteDrop() }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.addBlockToGroup(group, SceneType.TEXT) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Block")
                }
            }
        }
    }
}
