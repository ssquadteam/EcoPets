package com.willfp.ecopets.pets

import com.willfp.ecopets.EcoPetsPlugin
import org.bukkit.entity.Player

/**
 * Get the PetDisplay instance for the player.
 */
fun Player.getPetDisplay(): PetDisplay? {
    return EcoPetsPlugin.instance.petDisplay
} 