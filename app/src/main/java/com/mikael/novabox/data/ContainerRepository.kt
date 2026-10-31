package com.mikael.novabox.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.mikael.novabox.model.AudioMode
import com.mikael.novabox.model.Box64Preset
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.CpuArchitecture
import com.mikael.novabox.model.GraphicsProfile
import com.mikael.novabox.model.MouseMode
import com.mikael.novabox.model.NetworkMode
import com.mikael.novabox.model.WinePreset

class ContainerRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "novabox_container_store",
        Context.MODE_PRIVATE
    )

    fun load(): List<ContainerConfig> {
        val raw = preferences.getString(KEY_CONTAINERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val config = runCatching {
                        ContainerConfig(
                            id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                            name = item.getString("name"),
                            osProfileId = item.getString("osProfileId"),
                            architecture = enumOrDefault(item.optString("architecture"), CpuArchitecture.X86_64),
                            graphics = enumOrDefault(item.optString("graphics"), GraphicsProfile.AUTO),
                            resolution = item.optString("resolution", "1280x720"),
                            ramMb = item.optInt("ramMb", 1024),
                            cpuCores = item.optInt("cpuCores", 2),
                            diskGb = item.optInt("diskGb", 8),
                            dpi = item.optInt("dpi", 120),
                            fullscreen = item.optBoolean("fullscreen", true),
                            virtualDesktop = item.optBoolean("virtualDesktop", true),
                            audio = enumOrDefault(item.optString("audio"), AudioMode.AUTO),
                            network = enumOrDefault(item.optString("network"), NetworkMode.NAT),
                            mouseMode = enumOrDefault(item.optString("mouseMode"), MouseMode.TOUCHPAD),
                            mouseSensitivity = item.optInt("mouseSensitivity", 5),
                            vsync = item.optBoolean("vsync", true),
                            dxvkEnabled = item.optBoolean("dxvkEnabled", false),
                            vkd3dEnabled = item.optBoolean("vkd3dEnabled", false),
                            esyncEnabled = item.optBoolean("esyncEnabled", true),
                            fsyncEnabled = item.optBoolean("fsyncEnabled", false),
                            wow64Enabled = item.optBoolean("wow64Enabled", true),
                            winePreset = enumOrDefault(item.optString("winePreset"), WinePreset.STABLE),
                            box64Preset = enumOrDefault(item.optString("box64Preset"), Box64Preset.BALANCED),
                            graphicsMemoryMb = item.optInt("graphicsMemoryMb", 512),
                            fpsLimit = item.optInt("fpsLimit", 60),
                            locale = item.optString("locale", "pt_BR"),
                            environmentVariables = item.optString("environmentVariables", ""),
                            sharedFolderPath = item.optString("sharedFolderPath", "")
                        )
                    }.getOrNull()
                    if (config != null && config.validate() == null) add(config)
                }
            }
        }.getOrElse { emptyList() }
    }

    fun save(containers: List<ContainerConfig>) {
        val array = JSONArray()
        containers.filter { it.validate() == null }.forEach { config ->
            array.put(
                JSONObject()
                    .put("id", config.id)
                    .put("name", config.name)
                    .put("osProfileId", config.osProfileId)
                    .put("architecture", config.architecture.name)
                    .put("graphics", config.graphics.name)
                    .put("resolution", config.resolution)
                    .put("ramMb", config.ramMb)
                    .put("cpuCores", config.cpuCores)
                    .put("diskGb", config.diskGb)
                    .put("dpi", config.dpi)
                    .put("fullscreen", config.fullscreen)
                    .put("virtualDesktop", config.virtualDesktop)
                    .put("audio", config.audio.name)
                    .put("network", config.network.name)
                    .put("mouseMode", config.mouseMode.name)
                    .put("mouseSensitivity", config.mouseSensitivity)
                    .put("vsync", config.vsync)
                    .put("dxvkEnabled", config.dxvkEnabled)
                    .put("vkd3dEnabled", config.vkd3dEnabled)
                    .put("esyncEnabled", config.esyncEnabled)
                    .put("fsyncEnabled", config.fsyncEnabled)
                    .put("wow64Enabled", config.wow64Enabled)
                    .put("winePreset", config.winePreset.name)
                    .put("box64Preset", config.box64Preset.name)
                    .put("graphicsMemoryMb", config.graphicsMemoryMb)
                    .put("fpsLimit", config.fpsLimit)
                    .put("locale", config.locale)
                    .put("environmentVariables", config.environmentVariables)
                    .put("sharedFolderPath", config.sharedFolderPath)
            )
        }
        preferences.edit().putString(KEY_CONTAINERS, array.toString()).apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T {
        return runCatching { enumValueOf<T>(value) }.getOrDefault(default)
    }

    companion object {
        private const val KEY_CONTAINERS = "containers_json"
    }
}
