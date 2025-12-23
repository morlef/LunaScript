package si.f5.luna3419.script.resources

import java.io.File
import java.net.URLDecoder
import java.util.jar.JarFile

object ResourceManager {
    const val TYPE_BG = "bg"
    const val TYPE_BGM = "bgm"
    const val TYPE_SE = "se"
    const val TYPE_CHARACTER = "characters"
    const val TYPE_IMAGE = "image"

    fun getAssets(projectRoot: File, type: String): List<String> {
        val allCandidates = mutableSetOf<String>()

        val candidates = listOf(
            File(projectRoot, "src/resources/assets/$type"),
            File(projectRoot, "src/main/resources/assets/$type"),
            File(projectRoot, "resources/assets/$type"),
            File(projectRoot, "resource/assets/$type"),
            File(projectRoot, "assets/$type")
        )

        for (dir in candidates) {
            if (dir.exists() && dir.isDirectory) {
                val files =
                    dir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.map { it.nameWithoutExtension }

                if (files != null) {
                    allCandidates.addAll(files)
                }
            }
        }

        try {
            val classRes = ResourceManager::class.java.getResource("ResourceManager.class")
            if (classRes != null && classRes.protocol == "jar") {
                val path = classRes.path.substringAfter("file:").substringBefore("!")
                val decodedPath = URLDecoder.decode(path, "UTF-8")
                val jarFile = JarFile(File(decodedPath))

                val entries = jarFile.entries()
                val targetPath = "assets/$type/".lowercase()

                println("Scanning Jar: ${jarFile.name} for $targetPath")

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (!entry.isDirectory && name.lowercase().startsWith(targetPath)) {
                        val fileName = name.substringAfterLast('/')
                        if (fileName.isNotEmpty() && !fileName.startsWith(".")) {
                            allCandidates.add(fileName.substringBeforeLast('.'))
                        }
                    }
                }
                jarFile.close()
            }

            val src = ResourceManager::class.java.protectionDomain.codeSource
            if (src != null) {
                val jarUrl = src.location
                if (jarUrl.protocol == "file" && jarUrl.path.endsWith(".jar")) {
                    val jar = File(jarUrl.toURI())

                    val jarFile = JarFile(jar)
                    val entries = jarFile.entries()
                    val targetPath = "assets/$type/".lowercase()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (!entry.isDirectory && name.lowercase().startsWith(targetPath)) {
                            val fileName = name.substringAfterLast('/')
                            if (fileName.isNotEmpty() && !fileName.startsWith(".")) {
                                allCandidates.add(fileName.substringBeforeLast('.'))
                            }
                        }
                    }
                    jarFile.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return allCandidates.sorted().toList()
    }
}
