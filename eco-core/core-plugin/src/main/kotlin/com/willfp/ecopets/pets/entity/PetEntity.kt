package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.pets.Pet
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot

abstract class PetEntity(
    val pet: Pet,
    val player: Player
) {
    abstract fun spawn(location: Location): ArmorStand

    companion object {
        private val registrations = mutableMapOf<String, (Pet, String, Player) -> PetEntity>()

        @JvmStatic
        fun registerPetEntity(id: String, parse: (Pet, String, Player) -> PetEntity) {
            registrations[id] = parse
        }

        @JvmStatic
        fun create(pet: Pet, player: Player): PetEntity {
            val texture = pet.getEntityTextureWithSkin(player)

            if (!texture.contains(":")) {
                return SkullPetEntity(pet, player)
            }

            val id = texture.split(":")[0]
            val parse = registrations[id] ?: return SkullPetEntity(pet, player)
            return parse(pet, texture.removePrefix("$id:"), player)
        }
    }
}

internal fun emptyArmorStandAt(location: Location, pet: Pet): ArmorStand {
    val stand = location.world!!.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand

    stand.isVisible = false
    stand.isInvulnerable = true
    stand.isSmall = true
    stand.setGravity(false)
    stand.isCollidable = false
    stand.isPersistent = false

    for (slot in EquipmentSlot.values()) {
        stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)
    }

    stand.isCustomNameVisible = true
    @Suppress("DEPRECATION")
    stand.customName = pet.name

    return stand
}
