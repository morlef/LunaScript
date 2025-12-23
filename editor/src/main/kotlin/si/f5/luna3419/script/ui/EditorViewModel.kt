package si.f5.luna3419.script.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import si.f5.luna3419.script.data.SceneData
import si.f5.luna3419.script.data.SceneType
import si.f5.luna3419.script.io.ScriptManager
import si.f5.luna3419.script.player.PlayerViewModel
import si.f5.luna3419.script.resources.ResourceManager
import java.io.File

class EditorViewModel {
    var currentFile: File? by mutableStateOf(null)

    data class SceneGroup(
        var id: String, val blocks: SnapshotStateList<SceneData>
    )

    val groups = mutableStateListOf<SceneGroup>()

    var isDarkTheme by mutableStateOf(false)
    var sceneTitle by mutableStateOf("")

    sealed class DropTarget {
        data class Root(val groupIndex: Int, val index: Int) : DropTarget()
        data class Nested(val parentUuid: String, val branch: Boolean, val index: Int) : DropTarget()
    }

    var draggingPaletteType by mutableStateOf<SceneType?>(null)
    var draggingBlock by mutableStateOf<SceneData?>(null)

    var activeDropTarget by mutableStateOf<DropTarget?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)

    private val dropZones = mutableMapOf<DropTarget, Rect>()

    var draggingSourceGroup by mutableStateOf<SceneGroup?>(null)

    var statusMessage by mutableStateOf("Ready")

    val characterNames = try {
        val stream = javaClass.getResourceAsStream("/assets/characters.txt")
        val lines = stream?.bufferedReader()?.readLines() ?: emptyList()
        listOf("") + lines
    } catch (e: Exception) {
        e.printStackTrace()
        listOf("")
    }

    var isDirty by mutableStateOf(false)

    fun registerDropZone(target: DropTarget, bounds: Rect) {
        dropZones[target] = bounds
    }

    fun unregisterDropZone(target: DropTarget) {
        dropZones.remove(target)
    }

    fun onPaletteDrag(offset: Offset) {
        dragOffset = offset
        val ghostRect = Rect(offset, Size(220f, 40f))

        val hit = dropZones.entries.firstOrNull { it.value.overlaps(ghostRect) }
        activeDropTarget = hit?.key
    }

    fun onPaletteDrop() {
        val target = activeDropTarget
        val type = draggingPaletteType

        if (target != null) {
            if (type != null) {
                val newBlock = SceneData(type = type, commandName = type.value)
                insertBlockAtTarget(target, newBlock)
            }
        } else {
            if (groups.isEmpty()) {
                addGroup()
            }
        }

        draggingPaletteType = null
        draggingBlock = null
        draggingSourceGroup = null
        dragOffset = Offset.Zero
        activeDropTarget = null
        dropZones.clear()
    }

    private fun insertBlockAtTarget(target: DropTarget, newBlock: SceneData) {
        when (target) {
            is DropTarget.Root -> {
                if (target.groupIndex == -1) {
                    if (groups.isEmpty()) {
                        addGroup()
                    }
                    if (groups.isNotEmpty()) {
                        val group = groups[0]
                        val insertIndex =
                            if (target.index == -1) group.blocks.size else target.index.coerceIn(0, group.blocks.size)
                        group.blocks.add(insertIndex, newBlock)
                        renumberScenes(group)
                        isDirty = true
                    }
                } else if (target.groupIndex in groups.indices) {
                    val group = groups[target.groupIndex]
                    val insertIndex = target.index.coerceIn(0, group.blocks.size)
                    group.blocks.add(insertIndex, newBlock)
                    renumberScenes(group)
                    isDirty = true
                }
            }

            is DropTarget.Nested -> {
                startRecursiveUpdate(target, newBlock)
            }
        }
    }

    private fun startRecursiveUpdate(target: DropTarget.Nested, newBlock: SceneData) {
        for (group in groups) {
            val blocks = group.blocks
            for (i in blocks.indices) {
                val current = blocks[i]
                val updated = recursiveInsert(current, target, newBlock)
                if (updated !== current) {
                    blocks[i] = updated
                    isDirty = true
                    return
                }
            }
        }
    }

    private fun recursiveInsert(current: SceneData, target: DropTarget.Nested, newBlock: SceneData): SceneData {
        if (current.uuid == target.parentUuid) {
            if (target.branch) {
                val newList = current.trueBlock.toMutableList()
                val idx = target.index.coerceIn(0, newList.size)
                newList.add(idx, newBlock)
                return current.copy(trueBlock = newList)
            } else {
                val newList = current.falseBlock.toMutableList()
                val idx = target.index.coerceIn(0, newList.size)
                newList.add(idx, newBlock)
                return current.copy(falseBlock = newList)
            }
        }

        if (current.type == SceneType.IF) {
            var trueChanged = false
            val newTrueBlock = current.trueBlock.map { child ->
                val newChild = recursiveInsert(child, target, newBlock)
                if (newChild !== child) trueChanged = true
                newChild
            }.toMutableList()

            if (trueChanged) {
                return current.copy(trueBlock = newTrueBlock)
            }

            var falseChanged = false
            val newFalseBlock = current.falseBlock.map { child ->
                val newChild = recursiveInsert(child, target, newBlock)
                if (newChild !== child) falseChanged = true
                newChild
            }.toMutableList()

            if (falseChanged) {
                return current.copy(falseBlock = newFalseBlock)
            }
        }

        return current
    }

    fun removeGroup(group: SceneGroup) {
        groups.remove(group)
        isDirty = true
    }

    private fun renumberScenes(group: SceneGroup) {
        group.blocks.forEachIndexed { i, b -> b.num = i + 1 }
    }

    fun loadFile(file: File) {
        if (checkUnsavedChanges()) return
        try {
            val loaded = ScriptManager.loadSceneData(file)
            groups.clear()

            val rawScenes = loaded.scenes
            val grouped = rawScenes.groupBy { it.id }

            grouped.forEach { (id, list) ->
                val blockList = mutableStateListOf<SceneData>()
                blockList.addAll(list)
                groups.add(SceneGroup(id, blockList))
            }

            if (groups.isEmpty()) {
                val blockList = mutableStateListOf<SceneData>()
                groups.add(SceneGroup("scene001", blockList))
            }

            sceneTitle = loaded.title
            currentFile = file
            statusMessage = "Loaded ${file.name}"
            isDirty = false
        } catch (e: Exception) {
            statusMessage = "Error loading: ${e.message}"
            e.printStackTrace()
        }
    }

    fun saveFile() {
        if (currentFile == null) {
            statusMessage = "Please use Save As..."
            return
        }
        try {
            val allScenes = groups.flatMap { group ->
                group.blocks.map { it.copy(id = group.id) }
            }
            ScriptManager.saveSceneData(currentFile!!, allScenes)
            statusMessage = "Saved ${currentFile!!.name}"
            isDirty = false
        } catch (e: Exception) {
            statusMessage = "Error saving: ${e.message}"
        }
    }

    fun saveAs(file: File) {
        try {
            val finalFile = if (file.extension.lowercase() != "csv") {
                File(file.parentFile, file.nameWithoutExtension + ".csv")
            } else {
                file
            }

            val allScenes = groups.flatMap { group ->
                group.blocks.map { it.copy(id = group.id) }
            }
            ScriptManager.saveSceneData(finalFile, allScenes)
            currentFile = finalFile
            statusMessage = "Saved as ${finalFile.name}"
            isDirty = false
        } catch (e: Exception) {
            statusMessage = "Error saving: ${e.message}"
        }
    }

    fun createNewFile() {
        groups.clear()
        val blockList = mutableStateListOf<SceneData>()
        blockList.add(SceneData(id = "start", num = 1, type = SceneType.TEXT, text = "New Scene"))
        groups.add(SceneGroup("start", blockList))

        sceneTitle = "New Scene"
        currentFile = null
        statusMessage = "New File Created"
        isDirty = false
    }

    fun addGroup() {
        val newId = "label${groups.size + 1}"
        groups.add(SceneGroup(newId, mutableStateListOf()))
        isDirty = true
    }

    fun addBlockToGroup(group: SceneGroup, type: SceneType, index: Int = -1) {
        val newBlock = SceneData(
            id = group.id, num = group.blocks.size + 1, type = type
        )
        if (index >= 0 && index <= group.blocks.size) {
            group.blocks.add(index, newBlock)
        } else {
            group.blocks.add(newBlock)
        }
        group.blocks.forEachIndexed { i, b -> b.num = i + 1 }
        isDirty = true
    }

    fun removeBlockFromGroup(group: SceneGroup, block: SceneData) {
        group.blocks.remove(block)
        isDirty = true
    }

    fun updateBlock(group: SceneGroup, index: Int, newData: SceneData) {
        if (index in group.blocks.indices) {
            group.blocks[index] = newData
            isDirty = true
        }
    }

    fun checkUnsavedChanges(): Boolean = isDirty

    private fun findProjectRoot(file: File?): File {
        if (file == null) return File(".")

        var current = if (file.isDirectory) file else file.parentFile

        for (i in 0..5) {
            if (current == null) break

            if (File(current, "src").exists() || File(current, "resources").exists() || File(
                    current, "assets"
                ).exists() || File(current, "resource").exists()
            ) {
                return current
            }
            if (File(current, "gradlew").exists() || File(current, "build.gradle.kts").exists()) {
                return current
            }

            current = current.parentFile
            assert(i >= 0)
        }

        return file.parentFile?.parentFile ?: File(".")
    }

    fun getAssetSuggestions(type: String): List<String> {
        val root = findProjectRoot(currentFile)
        return ResourceManager.getAssets(root, type)
    }

    fun getCharacterIdSuggestions(): List<String> {
        val files = getAssetSuggestions(ResourceManager.TYPE_CHARACTER)
        return files.map { it.substringBefore('_') }.distinct().sorted()
    }

    fun getCharacterFaceSuggestions(id: String): List<String> {
        val files = getAssetSuggestions(ResourceManager.TYPE_CHARACTER)
        return files.filter { it.startsWith("${id}_") }.map { it.removePrefix("${id}_") }.distinct().sorted()
    }

    var debugPlayerViewModel by mutableStateOf<PlayerViewModel?>(null)

    var coordinateEditingBlockUuid by mutableStateOf<String?>(null)

    fun openCoordinateEditor(uuid: String) {
        coordinateEditingBlockUuid = uuid
    }

    fun closeCoordinateEditor() {
        coordinateEditingBlockUuid = null
    }

    fun updateBlockCoordinates(uuid: String, x: Int, y: Int) {
        for (group in groups) {
            val (parentBlock, targetBlock, indexInParent) = findBlockRecursively() ?: continue

            val params = targetBlock.commandParams.toMutableList()

            var xIndex = -1
            var yIndex = -1

            val type = targetBlock.type

            if (type == SceneType.CHARA_IN) {
                xIndex = 2
                yIndex = 3
            } else if (type == SceneType.IMAGE_IN) {
                xIndex = 1
                yIndex = 2
            } else if (targetBlock.type == SceneType.MOVE) {
                xIndex = 1
                yIndex = 2
            }

            if (xIndex != -1) {
                while (params.size <= yIndex) params.add("0")
                params[xIndex] = x.toString()
                params[yIndex] = y.toString()

                val newScene = targetBlock.copy(commandParams = params)

                if (parentBlock == null) {
                    group.blocks[indexInParent] = newScene
                } else {
                    replaceBlockInList(group.blocks, uuid, newScene)
                }
                isDirty = true
                return
            }
        }
    }

    private fun findBlockRecursively(): Triple<SceneData?, SceneData, Int>? {
        return null
    }

    private fun replaceBlockInList(list: MutableList<SceneData>, uuid: String, parsedBlock: SceneData): Boolean {
        for (i in list.indices) {
            if (list[i].uuid == uuid) {
                list[i] = parsedBlock
                return true
            }
            if (list[i].type == SceneType.IF) {
                if (replaceBlockInList(list[i].trueBlock, uuid, parsedBlock)) return true
                if (replaceBlockInList(list[i].falseBlock, uuid, parsedBlock)) return true
            }
        }
        return false
    }

    fun debugBlock(block: SceneData) {
        val root = findProjectRoot(currentFile)

        val all = getAllScenes()
        val index = all.indexOfFirst { it.id == block.id && it.num == block.num }

        val vm = PlayerViewModel()
        vm.start(all, if (index == -1) 0 else index, root)
        debugPlayerViewModel = vm
    }

    private fun getAllScenes(): List<SceneData> {
        return groups.flatMap { group ->
            group.blocks.map { it.copy(id = group.id) }
        }
    }
}
