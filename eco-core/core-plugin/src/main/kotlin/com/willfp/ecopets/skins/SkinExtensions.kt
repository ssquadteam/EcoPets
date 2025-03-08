package com.willfp.ecopets.skins

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Get the active skin for a player.
 */
val Player.activeSkin: PetSkin?
    get() = PetSkins.getActiveSkin(this.uniqueId)

/**
 * Set the active skin for a player.
 */
fun Player.setActiveSkin(skin: PetSkin) {
    PetSkins.setActiveSkin(this.uniqueId, skin)
}

/**
 * Remove the active skin for a player.
 */
fun Player.removeActiveSkin() {
    PetSkins.removeSkin(this.uniqueId)
}

/**
 * Check if a skin is active for a player.
 */
fun Player.isSkinActive(skinID: String): Boolean {
    return PetSkins.isSkinActive(this.uniqueId, skinID)
}

/**
 * Extension function to get the skin ID.
 */
fun Player.getActiveSkinID(): String? {
    return activeSkin?.id
}

/**
 * Extension function to get the skin display name.
 */
fun Player.getActiveSkinDisplay(): String? {
    return activeSkin?.displayName
}

/**
 * Extension function to check if a player has an active skin.
 */
fun Player.hasActiveSkin(): Boolean {
    return activeSkin != null
} 