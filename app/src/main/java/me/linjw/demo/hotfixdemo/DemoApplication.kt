package me.linjw.demo.hotfixdemo

import android.app.Application
import android.os.FileUtils
import java.io.File

class DemoApplication : Application() {
    companion object {
        private const val HOTFIX_DEX = "hotfix.dex"
    }

    override fun onCreate() {
        super.onCreate()

        val patch = File(cacheDir, HOTFIX_DEX)
        assets.open(HOTFIX_DEX).use { src ->
            patch.outputStream().use { dest ->
                FileUtils.copy(src, dest)
            }
        }
        PatchLoader.loadPatch(this, patch)
    }
}