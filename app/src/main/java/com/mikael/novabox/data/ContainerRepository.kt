package com.mikael.novabox.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.mikael.novabox.model.ContainerConfig
import com.mikael.novabox.model.CpuArchitecture
import com.mikael.novabox.model.GraphicsProfile

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
                            id = item.getString("id"),
                            name = item.getString("name"),
                            osProfileId = item.getString("osProfileId"),
                            architecture = CpuArchitecture.valueOf(item.optString("architecture", CpuArchitecture.X86_64.name)),
                            graphics = GraphicsProfile.valueOf(item.optString("graphics", GraphicsProfile.AUTO.name)),
                            resolution = item.optString("resolution", "1280x720"),
                            ramMb = item.optInt("ramMb", 1024)
                        )
                    }.getOrNull()
                    if (config != null && config.validate() == null) add(config)
                }
            }
        }.getOrElse { emptyList() }
    }

    fun save(containers: List<ContainerConfig>) {
        val array = JSONArray()
        containers.forEach { config ->
            array.put(
                JSONObject()
                    .put("id", config.id)
                    .put("name", config.name)
                    .put("osProfileId", config.osProfileId)
                    .put("architecture", config.architecture.name)
                    .put("graphics", config.graphics.name)
                    .put("resolution", config.resolution)
                    .put("ramMb", config.ramMb)
            )
        }
        preferences.edit().putString(KEY_CONTAINERS, array.toString()).apply()
    }

    companion object {
        private const val KEY_CONTAINERS = "containers_json"
    }
}
