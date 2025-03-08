package com.willfp.ecopets.pets.entity

import com.willfp.eco.core.items.builder.SkullBuilder
import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SkullPetEntity(pet: Pet, player: Player) : PetEntity(pet, player) {
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        val texture = pet.getEntityTextureWithSkin(player)
        val skull: ItemStack = SkullBuilder()
            .setSkullTexture(texture)
            .build()

        @Suppress("UNNECESSARY_SAFE_CALL") // Can be null.
        stand.equipment?.helmet = skull

        return stand
    }
}
