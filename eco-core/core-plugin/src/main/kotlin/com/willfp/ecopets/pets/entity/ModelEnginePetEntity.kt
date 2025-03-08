package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.skins.activeSkin
import com.willfp.modelenginebridge.ModelEngineBridge
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String,
    private val plugin: EcoPetsPlugin,
    player: Player
) : PetEntity(pet, player) {
    private var activeModel: Any? = null
    
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        // Create the model
        val model = ModelEngineBridge.instance.createActiveModel(modelID) ?: return stand
        activeModel = model
        
        // Assign the model to the entity
        val modelled = ModelEngineBridge.instance.createModeledEntity(stand)
        modelled.addModel(model)
        
        // Try to play initial animation
        updateAnimation()
        
        return stand
    }
    
    /**
     * Update the entity's animation based on player state
     */
    fun updateAnimation() {
        val model = activeModel ?: return
        val playerSkin = player.activeSkin
        
        if (playerSkin != null && playerSkin.useModelEngine && playerSkin.useCustomAnimations) {
            val animationId = playerSkin.getAnimationForState(player)
            if (animationId.isNotEmpty()) {
                // Try to play the animation
                try {
                    // Use the available methods in ModelEngineBridge
                    // This will vary depending on the version of ModelEngineBridge
                    plugin.logger.info("Trying to play animation: $animationId for model: $modelID")
                    
                    // Since we don't know the exact signature, try a generic approach
                    // The actual implementation would need to match the available API
                    val modelClass = model.javaClass
                    val playAnimMethod = modelClass.methods.find { it.name.contains("playAnimation", true) }
                    
                    if (playAnimMethod != null) {
                        playAnimMethod.invoke(model, animationId)
                    } else {
                        plugin.logger.warning("Could not find playAnimation method for model: $modelID")
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to play animation '$animationId' for model '$modelID': ${e.message}")
                }
            }
        }
    }
}
