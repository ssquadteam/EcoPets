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
    
    // Model Engine animation settings
    val useCustomAnimations: Boolean = config.getBool("modelengine-animations.enabled")
    
    /**
     * Get the appropriate animation for the player's current state.
     */
    fun getAnimationForState(player: Player): String {
        if (!useModelEngine || !useCustomAnimations) {
            return "idle" // Default animation if not using custom animations
        }
        
        // Get the default idle animation first - we'll use this if other animations are missing
        val defaultAnimation = config.getStringOrNull("modelengine-animations.idle") ?: "idle"
        
        // More sensitive movement detection
        // XZ plane movement (horizontal)
        val isMovingHorizontally = player.velocity.setY(0).lengthSquared() > 0.003 
            || player.location.direction.setY(0).lengthSquared() > 0.5
        
        return when {
            player.isFlying || player.isGliding -> config.getStringOrNull("modelengine-animations.flying") ?: defaultAnimation
            player.isSwimming -> config.getStringOrNull("modelengine-animations.swimming") ?: defaultAnimation
            player.isSneaking -> config.getStringOrNull("modelengine-animations.sneaking") ?: defaultAnimation
            player.isSprinting -> config.getStringOrNull("modelengine-animations.running") ?: defaultAnimation
            player.isInsideVehicle -> config.getStringOrNull("modelengine-animations.riding") ?: defaultAnimation
            isMovingHorizontally -> config.getStringOrNull("modelengine-animations.walking") ?: defaultAnimation
            else -> defaultAnimation
        }
    }

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