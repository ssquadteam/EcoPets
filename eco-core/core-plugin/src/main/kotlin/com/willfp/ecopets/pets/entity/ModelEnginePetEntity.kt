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
    private var lastPlayedAnimation: String = "idle"
    private var debugEnabled: Boolean = try {
        plugin.configYml.getBool("debug-logging")
    } catch (e: Exception) {
        false
    }
    
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        try {
            // Create the model
            if (debugEnabled) {
                plugin.logger.info("[Debug] Creating ModelEngine model: $modelID for player ${player.name}")
            }
            
            val model = ModelEngineBridge.instance.createActiveModel(modelID)
            
            if (model == null) {
                plugin.logger.warning("Failed to create ModelEngine model: $modelID - Model not found!")
                return stand
            }
            
            activeModel = model
            
            // Assign the model to the entity
            val modelled = ModelEngineBridge.instance.createModeledEntity(stand)
            if (modelled == null) {
                plugin.logger.warning("Failed to create modelled entity for model: $modelID")
                return stand
            }
            
            modelled.addModel(model)
            
            // Try to play initial animation
            updateAnimation(true) // Force update on spawn
            
            if (debugEnabled) {
                plugin.logger.info("[Debug] Successfully spawned ModelEngine pet: $modelID with ${player.activeSkin?.id ?: "no skin"}")
            }
            
        } catch (e: Exception) {
            plugin.logger.severe("Error spawning ModelEngine pet: ${e.message}")
            e.printStackTrace()
        }
        
        return stand
    }
    
    /**
     * Update the entity's animation based on player state
     */
    fun updateAnimation(forceUpdate: Boolean = false) {
        val model = activeModel ?: return
        val playerSkin = player.activeSkin
        
        try {
            if (playerSkin != null && playerSkin.useModelEngine && playerSkin.useCustomAnimations) {
                val animationId = playerSkin.getAnimationForState(player)
                
                // Only update if animation changed or force update is requested
                if (forceUpdate || animationId != lastPlayedAnimation) {
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Player ${player.name} state change: playing animation '$animationId' for model: $modelID (previous: $lastPlayedAnimation)")
                    }
                    
                    // Try different approaches to play the animation
                    tryAllAnimationMethods(model, animationId)
                    
                    // Update last played animation
                    lastPlayedAnimation = animationId
                }
            } else if (debugEnabled && forceUpdate) {
                plugin.logger.info("[Debug] ModelEngine animations disabled for pet: $modelID")
                if (playerSkin == null) {
                    plugin.logger.info("[Debug] - Reason: No skin active")
                } else if (!playerSkin.useModelEngine) {
                    plugin.logger.info("[Debug] - Reason: use-modelengine is false")
                } else if (!playerSkin.useCustomAnimations) {
                    plugin.logger.info("[Debug] - Reason: modelengine-animations.enabled is false")
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error updating animation: ${e.message}")
        }
    }
    
    /**
     * Try multiple approaches to play an animation on a ModelEngine model
     */
    private fun tryAllAnimationMethods(model: Any, animationId: String) {
        val modelClass = model.javaClass
        var success = false
        
        // Method 1: Try direct playAnimation method
        try {
            val directMethod = modelClass.getMethod("playAnimation", String::class.java)
            directMethod.invoke(model, animationId)
            if (debugEnabled) {
                plugin.logger.info("[Debug] Successfully played animation using direct method: $animationId")
            }
            success = true
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.info("[Debug] Couldn't play animation with direct method: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        
        // Method 2: Try ModelEngineBridge methods
        if (!success) {
            try {
                val bridgeClass = ModelEngineBridge.instance.javaClass
                val playMethod = bridgeClass.methods.find { it.name.contains("playAnimation", true) }
                
                if (playMethod != null) {
                    playMethod.invoke(ModelEngineBridge.instance, model, animationId)
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Successfully played animation using bridge method: $animationId")
                    }
                    success = true
                }
            } catch (e: Exception) {
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Couldn't play animation with bridge method: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }
        
        // Method 3: Try reflection on model methods
        if (!success) {
            try {
                val methods = modelClass.methods
                val animationMethod = methods.find { 
                    (it.name.contains("play", true) || it.name.contains("animation", true) || it.name.contains("anim", true)) 
                    && it.parameterCount == 1 
                    && it.parameterTypes[0] == String::class.java 
                }
                
                if (animationMethod != null) {
                    animationMethod.invoke(model, animationId)
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Successfully played animation using reflection method '${animationMethod.name}': $animationId")
                    }
                    success = true
                } else if (debugEnabled) {
                    plugin.logger.info("[Debug] Couldn't find any suitable animation methods on model class ${modelClass.simpleName}")
                    plugin.logger.info("[Debug] Available methods: ${methods.joinToString(", ") { it.name }}")
                }
            } catch (e: Exception) {
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Couldn't play animation with reflection: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }
        
        if (!success) {
            plugin.logger.warning("Failed to play animation for model: $modelID, animation: $animationId - No suitable method found")
        }
    }
}
