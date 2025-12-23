package si.f5.luna3419.script.ui.components

import androidx.compose.ui.platform.LocalDensity
import si.f5.luna3419.script.ui.EditorViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.dp
import java.io.File
import si.f5.luna3419.script.data.SceneType
import kotlin.system.exitProcess

@Composable
fun CustomTitleBar(viewModel: EditorViewModel, window: ComposeWindow) {
    DisposableEffect(window) {
        val mouseAdapter = object : java.awt.event.MouseAdapter() {
            private var startPoint: java.awt.Point? = null
            override fun mousePressed(e: java.awt.event.MouseEvent) {
                if (e.point.y < 40) startPoint = e.point
            }

            override fun mouseDragged(e: java.awt.event.MouseEvent) {
                startPoint?.let { start ->
                    val current = e.locationOnScreen
                    window.location = java.awt.Point(current.x - start.x, current.y - start.y)
                }
            }

            override fun mouseReleased(e: java.awt.event.MouseEvent) {
                startPoint = null
            }
        }
        window.addMouseListener(mouseAdapter)
        window.addMouseMotionListener(mouseAdapter)
        onDispose {
            window.removeMouseListener(mouseAdapter)
            window.removeMouseMotionListener(mouseAdapter)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuBar(viewModel, window)
        Spacer(Modifier.weight(1f))
        val dirtyMark = if (viewModel.isDirty) "*" else ""
        Text(
            text = "LunaScript Editor - ${viewModel.currentFile?.name ?: "Untitled"}$dirtyMark",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        WindowControls(window, viewModel)
    }
}

@Composable
fun MenuBar(viewModel: EditorViewModel, window: ComposeWindow) {
    var fileMenuExpanded by remember { mutableStateOf(false) }

    Row {
        Box {
            TextButton(onClick = { fileMenuExpanded = true }) {
                Text(
                    "File", color = MaterialTheme.colorScheme.onSurface
                )
            }
            DropdownMenu(expanded = fileMenuExpanded, onDismissRequest = { fileMenuExpanded = false }) {
                DropdownMenuItem(text = { Text("New") }, onClick = {
                    confirmSaveIfNeeded(viewModel, window) {
                        viewModel.createNewFile()
                    }
                    fileMenuExpanded = false
                }, trailingIcon = { Text("Ctrl+N", style = MaterialTheme.typography.bodySmall) })
                DropdownMenuItem(text = { Text("Open") }, onClick = {
                    confirmSaveIfNeeded(viewModel, window) {
                        val file = chooseFile(save = false, initialDirectory = viewModel.currentFile?.parentFile)
                        if (file != null) {
                            viewModel.createNewFile()
                            viewModel.loadFile(file)
                        }
                    }
                    fileMenuExpanded = false
                }, trailingIcon = { Text("Ctrl+O", style = MaterialTheme.typography.bodySmall) })
                DropdownMenuItem(text = { Text("Save") }, onClick = {
                    if (viewModel.currentFile == null) {
                        val file = chooseFile(save = true, initialDirectory = viewModel.currentFile?.parentFile)
                        if (file != null) viewModel.saveAs(file)
                    } else {
                        viewModel.saveFile()
                    }
                    fileMenuExpanded = false
                }, trailingIcon = { Text("Ctrl+S", style = MaterialTheme.typography.bodySmall) })
                DropdownMenuItem(text = { Text("Save As...") }, onClick = {
                    val file = chooseFile(save = true, initialDirectory = viewModel.currentFile?.parentFile)
                    if (file != null) viewModel.saveAs(file)
                    fileMenuExpanded = false
                })
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Switch Theme") },
                    onClick = { viewModel.isDarkTheme = !viewModel.isDarkTheme; fileMenuExpanded = false })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Exit") }, onClick = {
                    performSafeExit(viewModel, window)
                })
            }
        }
    }
}

@Composable
fun WindowControls(window: ComposeWindow, viewModel: EditorViewModel) {
    Row {
        IconButton(onClick = { window.isMinimized = true }) {
            Icon(
                Icons.Filled.Minimize, "Minimize", tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = {
            if (window.extendedState == java.awt.Frame.MAXIMIZED_BOTH) window.extendedState = java.awt.Frame.NORMAL
            else window.extendedState = java.awt.Frame.MAXIMIZED_BOTH
        }) { Text("□", color = MaterialTheme.colorScheme.onSurface) }
        IconButton(onClick = { performSafeExit(viewModel, window) }) {
            Icon(
                Icons.Filled.Close, "Close", tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun DropZone(
    target: EditorViewModel.DropTarget, viewModel: EditorViewModel, modifier: Modifier = Modifier, onDrop: () -> Unit
) {
    DisposableEffect(target) {
        onDispose {
            viewModel.unregisterDropZone(target)
        }
    }

    val isHovered = viewModel.activeDropTarget == target

    val divHeight = 8.dp

    val color = when {
        isHovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        viewModel.draggingPaletteType != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val finalModifier = if (target is EditorViewModel.DropTarget.Root && target.groupIndex == -1) {
        modifier
    } else {
        modifier.fillMaxWidth().height(divHeight)
    }

    Box(modifier = finalModifier.onGloballyPositioned {
            if (it.isAttached) {
                viewModel.registerDropZone(target, it.boundsInRoot())
            }
        }.background(color, RoundedCornerShape(4.dp)))
}

@Composable
fun PaletteView(viewModel: EditorViewModel) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(
            "Palette",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(SceneType.entries.filter {
                it != SceneType.UNKNOWN && it != SceneType.LABEL && it != SceneType.END_CHOICE && it != SceneType.ELSE && it != SceneType.ENDIF && it != SceneType.CHARA_OUT && it != SceneType.IMAGE_OUT && it != SceneType.BGM_STOP && it != SceneType.WAIT_CLICK && it != SceneType.FLASH && it != SceneType.SCALE && it != SceneType.ROTATE && it != SceneType.SHAKE_OBJ && it != SceneType.TINT
            }) { _, type ->
                DraggablePaletteItem(type, viewModel)
            }
        }
    }
}

@Composable
fun DraggablePaletteItem(type: SceneType, viewModel: EditorViewModel, isGhost: Boolean = false) {
    var myPosition by remember { mutableStateOf(Offset.Zero) }

    val displayName = when (type) {
        SceneType.CHARA_IN -> "CHARACTER"
        SceneType.IMAGE_IN -> "IMAGE"
        SceneType.BGM_PLAY -> "BGM"
        SceneType.WAIT -> "WAIT"
        SceneType.SHAKE -> "SCREEN EFFECT"
        SceneType.MOVE -> "TRANSFORM"
        else -> type.name
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().alpha(if (isGhost) 0.8f else 1f).run {
                if (!isGhost) {
                    onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            myPosition = coords.positionInRoot()
                        }
                    }
                } else this
            }.run {
                if (!isGhost) {
                    pointerInput(Unit) {
                        detectDragGestures(onDragStart = {
                            viewModel.draggingPaletteType = type
                            viewModel.dragOffset = myPosition
                        }, onDrag = { change, dragAmount ->
                            change.consume()
                            viewModel.onPaletteDrag(viewModel.dragOffset + dragAmount)
                        }, onDragEnd = {
                            viewModel.onPaletteDrop()
                        }, onDragCancel = {
                            viewModel.draggingPaletteType = null
                            viewModel.activeDropTarget = null
                        })
                    }
                } else this
            }) {
        Box(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AddCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun chooseFile(save: Boolean, initialDirectory: File? = null): File? {
    val chooser = javax.swing.JFileChooser()
    if (initialDirectory != null && initialDirectory.exists()) {
        chooser.currentDirectory = initialDirectory
    } else {
        chooser.currentDirectory = File(System.getProperty("user.dir"))
    }

    if (save) {
        if (chooser.showSaveDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            return chooser.selectedFile
        }
    } else {
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            return chooser.selectedFile
        }
    }
    return null
}

fun confirmSaveIfNeeded(viewModel: EditorViewModel, window: java.awt.Component, onProceed: () -> Unit) {
    if (viewModel.checkUnsavedChanges()) {
        val result = javax.swing.JOptionPane.showConfirmDialog(
            window,
            "You have unsaved changes. Do you want to save before proceeding?",
            "Unsaved Changes",
            javax.swing.JOptionPane.YES_NO_CANCEL_OPTION
        )
        when (result) {
            javax.swing.JOptionPane.YES_OPTION -> {
                var saved = false
                if (viewModel.currentFile == null) {
                    val file = chooseFile(save = true, initialDirectory = viewModel.currentFile?.parentFile)
                    if (file != null) {
                        viewModel.saveAs(file)
                        saved = true
                    }
                } else {
                    viewModel.saveFile()
                    saved = true
                }
                if (saved) onProceed()
            }

            javax.swing.JOptionPane.NO_OPTION -> onProceed()
        }
    } else {
        onProceed()
    }
}

fun performSafeExit(viewModel: EditorViewModel, window: ComposeWindow) {
    confirmSaveIfNeeded(viewModel, window) {
        exitProcess(0)
    }
}

@Composable
fun SuggestionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var minWidth by remember { mutableStateOf(0) }

    Box(modifier = modifier) {
        OutlinedTextField(value = value, onValueChange = {
            onValueChange(it)
            expanded = true
        }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                minWidth = coordinates.size.width
            }, trailingIcon = {
            IconButton(onClick = { expanded = !expanded }) {
                Text("▼", style = MaterialTheme.typography.bodySmall)
            }
        })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { minWidth.toDp() })
        ) {
            val filtered = suggestions.filter { it.contains(value, ignoreCase = true) || value.isEmpty() }
            if (filtered.isNotEmpty()) {
                filtered.forEach { suggestion ->
                    DropdownMenuItem(text = { Text(suggestion) }, onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    })
                }
            }
        }
    }
}
