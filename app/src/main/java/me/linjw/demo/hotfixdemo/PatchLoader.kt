package me.linjw.demo.hotfixdemo

import android.content.Context
import android.util.Log
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import java.io.File
import java.lang.reflect.Array

object PatchLoader {
    private const val TAG = "PatchLoader"

    fun loadPatch(context: Context, dexFile: File) {
        if (!dexFile.exists()) {
            Log.d(TAG, "load hotfix dex failed : $dexFile not exists")
            return
        }

        Log.d(TAG, "load hotfix dex : $dexFile")

        try {
            // 用DexClassLoader加载外部dex，并获取Element数组
            val dexClassLoader = DexClassLoader(dexFile.path, context.cacheDir.path, null, context.classLoader)
            val newPathList = getDeclaredField(dexClassLoader, BaseDexClassLoader::class.java, "pathList")!!
            val newDexElements = getDeclaredField(newPathList, "dalvik.system.DexPathList", "dexElements")!!

            // 获取进程原本的Element数组
            val oldPathList = getDeclaredField(context.classLoader, BaseDexClassLoader::class.java, "pathList")!!
            val oldDexElements = getDeclaredField(oldPathList, "dalvik.system.DexPathList", "dexElements")!!

            // 合并两个Element数组,把DexClassLoader的Element数组放在前面
            val combineArray = combineDexArray(newDexElements, oldDexElements)

            // 修改进程原本的Element数组为合并的新数组
            setDeclaredField(oldPathList, "dalvik.system.DexPathList", "dexElements", combineArray)
        } catch (t: Throwable) {
            Log.e(TAG, "load $dexFile failed", t)
        }
    }

    private fun setDeclaredField(obj: Any?, clazz: String, field: String, value: Any?) {
        Class.forName(clazz)
            .getDeclaredField(field)
            .apply { isAccessible = true }
            .set(obj, value)
    }

    private fun getDeclaredField(obj: Any?, clazz: String, field: String): Any? {
        return getDeclaredField(obj, Class.forName(clazz), field)
    }

    private fun getDeclaredField(obj: Any?, clazz: Class<*>, field: String): Any? {
        return clazz
            .getDeclaredField(field)
            .apply { isAccessible = true }
            .get(obj)
    }

    private fun combineDexArray(firstArray: Any, secondArray: Any): Any {
        val localClass = firstArray.javaClass.componentType!!
        val firstArrayLength = Array.getLength(firstArray)
        val secondArrayLength = Array.getLength(secondArray)
        val result = Array.newInstance(localClass, firstArrayLength + secondArrayLength)
        System.arraycopy(firstArray, 0, result, 0, firstArrayLength)
        System.arraycopy(secondArray, 0, result, firstArrayLength, secondArrayLength)
        return result
    }
}