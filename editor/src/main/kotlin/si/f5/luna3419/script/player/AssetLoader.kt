package si.f5.luna3419.script.player

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import javax.imageio.ImageIO

object AssetLoader {
    fun loadImage(url: java.net.URL): ImageBitmap? {
        return try {
            ImageIO.read(url)?.toComposeImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun findAssetUrl(root: File, type: String, name: String): java.net.URL? {
        val extensions = listOf("png", "jpg", "jpeg", "ogg", "mp3", "wav")
        val candidates = listOf(
            File(root, "src/resources/assets/$type"),
            File(root, "src/main/resources/assets/$type"),
            File(root, "resources/assets/$type"),
            File(root, "resource/assets/$type"),
            File(root, "assets/$type")
        )

        for (dir in candidates) {
            if (dir.exists()) {
                for (ext in extensions) {
                    val f = File(dir, "$name.$ext")
                    if (f.exists()) return f.toURI().toURL()
                }
            }
        }

        for (ext in extensions) {
            val path = "assets/$type/$name.$ext"
            val res = AssetLoader::class.java.classLoader.getResource(path)
            if (res != null) return res
        }
        return null
    }
}
