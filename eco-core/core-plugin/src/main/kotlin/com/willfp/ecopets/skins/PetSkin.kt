package com.willfp.ecopets.skins

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registrable
import org.bukkit.entity.Player

class PetSkin(
    val id: String,
    val config: Config
) : Registrable {
    val displayName: String = config.getFormattedString("display-name")
    val textureHeadID: String = config.getString("texture-head-id")
    val modelEngineID: String? = config.getStringOrNull("modelengine-id")
    val useModelEngine: Boolean = config.getBool("use-modelengine")

    override fun getID(): String {
        return id
    }

    fun getTextureToUse(): String {
        return if (useModelEngine && modelEngineID != null) {
            "modelengine:$modelEngineID"
        } else {
            textureHeadID
        }
    }
} 