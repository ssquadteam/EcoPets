package com.willfp.ecopets.pets.entity

import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.ecopets.pets.Pet
import com.willfp.ecopets.skins.activeSkin
import com.willfp.modelenginebridge.ModelEngineBridge
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import java.lang.reflect.Method

// Using ModelEngine conditionally - these classes might not exist at runtime
// We'll use reflection to work with the API instead of direct imports

class ModelEnginePetEntity(
    pet: Pet,
    private val modelID: String,
    private val plugin: EcoPetsPlugin,
    player: Player
) : PetEntity(pet, player) {
    private var activeModel: Any? = null
    private var modelEngineModel: Any? = null
    private var lastPlayedAnimation: String = "idle"
    private var debugEnabled: Boolean = try {
        plugin.configYml.getBool("debug-logging")
    } catch (e: Exception) {
        false
    }
    
    override fun spawn(location: Location): ArmorStand {
        val stand = emptyArmorStandAt(location, pet)

        try {
            // First try with direct ModelEngine API if available
            try {
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Trying to create model with direct ModelEngine API: $modelID")
                }
                
                // Check if ModelEngine is present using reflection
                val modelEngineApiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI")
                
                // According to the ModelEngine Javadoc, the API should be accessed through correct method
                // Try all possible patterns for getting the API instance
                var modelEngineApi: Any? = null
                
                // The correct name is getAPI() based on the R4.0.8 JavaDoc
                try {
                    val getApiMethod = modelEngineApiClass.getMethod("getAPI")
                    modelEngineApi = getApiMethod.invoke(null)
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Found ModelEngine API using getAPI()")
                    }
                } catch (e: Exception) {
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] getAPI() not found: ${e.message}")
                    }
                }
                
                // If the primary method didn't work, try fallbacks for compatibility
                if (modelEngineApi == null) {
                    // Other methods for backward compatibility with older versions
                    for (methodName in listOf("get", "getInstance", "instance")) {
                        try {
                            val method = modelEngineApiClass.getDeclaredMethod(methodName)
                            modelEngineApi = method.invoke(null)
                            if (modelEngineApi != null) {
                                if (debugEnabled) {
                                    plugin.logger.info("[Debug] Found ModelEngine API using $methodName()")
                                }
                                break
                            }
                        } catch (e: Exception) {
                            // Continue to next method
                        }
                    }
                    
                    // Try static field patterns if methods didn't work
                    if (modelEngineApi == null) {
                        for (fieldName in listOf("API", "instance", "INSTANCE")) {
                            try {
                                val field = modelEngineApiClass.getDeclaredField(fieldName)
                                field.isAccessible = true
                                modelEngineApi = field.get(null)
                                if (modelEngineApi != null) {
                                    if (debugEnabled) {
                                        plugin.logger.info("[Debug] Found ModelEngine API using $fieldName field")
                                    }
                                    break
                                }
                            } catch (e: Exception) {
                                // Continue to next field
                            }
                        }
                    }
                }
                
                // Last resort - print all methods and fields for debugging
                if (modelEngineApi == null && debugEnabled) {
                    plugin.logger.info("[Debug] ModelEngine API access methods: ${modelEngineApiClass.methods.joinToString(", ") { it.name }}")
                    plugin.logger.info("[Debug] ModelEngine API fields: ${modelEngineApiClass.declaredFields.joinToString(", ") { it.name }}")
                }
                
                if (modelEngineApi != null) {
                    // Based on ModelEngine R4.0.8 docs, we need to use createActiveModel and createModeledEntity methods
                    try {
                        // Create ActiveModel
                        val createActiveModelMethod = modelEngineApiClass.getMethod("createActiveModel", String::class.java)
                        val model = createActiveModelMethod.invoke(null, modelID) // Static method
                        
                        if (model != null) {
                            // Create ModeledEntity
                            val createModeledEntityMethod = modelEngineApiClass.getMethod("createModeledEntity", org.bukkit.entity.Entity::class.java)
                            val modeledEntity = createModeledEntityMethod.invoke(null, stand) // Static method
                            
                            if (modeledEntity != null) {
                                // Add model to entity
                                val addModelMethod = modeledEntity.javaClass.getMethod("addModel", model.javaClass.interfaces[0])
                                addModelMethod.invoke(modeledEntity, model)
                                
                                // Store model for later use
                                modelEngineModel = model
                                
                                if (debugEnabled) {
                                    plugin.logger.info("[Debug] Successfully created ModelEngine model using direct API: $modelID")
                                }
                                
                                // Try to play initial animation
                                updateAnimation(true)
                                
                                if (debugEnabled) {
                                    plugin.logger.info("[Debug] Successfully spawned ModelEngine pet using direct API: $modelID with ${player.activeSkin?.id ?: "no skin"}")
                                }
                                
                                return stand
                            }
                        }
                    } catch (e: Exception) {
                        if (debugEnabled) {
                            plugin.logger.info("[Debug] Error using direct static methods: ${e.message}, trying instance methods")
                        }
                        
                        // If static methods failed, try instance methods
                        try {
                            // Try to create the model using instance methods
                            val model = if (modelEngineApi.javaClass.methods.any { it.name == "createActiveModel" }) {
                                val method = modelEngineApi.javaClass.getMethod("createActiveModel", String::class.java)
                                method.invoke(modelEngineApi, modelID)
                            } else {
                                val method = modelEngineApi.javaClass.getMethod("getBlueprint", String::class.java)
                                val blueprint = method.invoke(modelEngineApi, modelID)
                                
                                if (blueprint != null) {
                                    val createMethod = modelEngineApi.javaClass.getMethod("createActiveModel", blueprint.javaClass)
                                    createMethod.invoke(modelEngineApi, blueprint)
                                } else null
                            }
                            
                            if (model != null) {
                                // Create modeled entity
                                val createModeledEntityMethod = modelEngineApi.javaClass.getMethod("createModeledEntity", org.bukkit.entity.Entity::class.java)
                                val modeledEntity = createModeledEntityMethod.invoke(modelEngineApi, stand)
                                
                                if (modeledEntity != null) {
                                    // Add model to entity
                                    val addModelMethod = modeledEntity.javaClass.getMethod("addModel", model.javaClass.interfaces[0])
                                    addModelMethod.invoke(modeledEntity, model)
                                    
                                    // Store model for later use
                                    modelEngineModel = model
                                    
                                    if (debugEnabled) {
                                        plugin.logger.info("[Debug] Successfully created ModelEngine model using instance methods: $modelID")
                                    }
                                    
                                    // Try to play initial animation
                                    updateAnimation(true)
                                    
                                    if (debugEnabled) {
                                        plugin.logger.info("[Debug] Successfully spawned ModelEngine pet using instance methods: $modelID with ${player.activeSkin?.id ?: "no skin"}")
                                    }
                                    
                                    return stand
                                }
                            }
                        } catch (e2: Exception) {
                            if (debugEnabled) {
                                plugin.logger.info("[Debug] Error using instance methods: ${e2.message}")
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                // If there's any error with the direct API, fall back to bridge
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Error using direct ModelEngine API: ${e.message}. Falling back to ModelEngineBridge.")
                    if (e is ClassNotFoundException) {
                        plugin.logger.info("[Debug] ModelEngine classes not found. Make sure ModelEngine is installed.")
                    } else {
                        e.printStackTrace()
                    }
                }
            }
            
            // Fall back to ModelEngineBridge if direct API failed
            if (debugEnabled) {
                plugin.logger.info("[Debug] Creating ModelEngine model with bridge: $modelID for player ${player.name}")
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
                plugin.logger.info("[Debug] Successfully spawned ModelEngine pet with bridge: $modelID with ${player.activeSkin?.id ?: "no skin"}")
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
        val playerSkin = player.activeSkin
        
        try {
            if (playerSkin != null && playerSkin.useModelEngine && playerSkin.useCustomAnimations) {
                val animationId = playerSkin.getAnimationForState(player)
                
                // Check if we should log debug info for this tick
                val shouldLogDebug = debugEnabled && (forceUpdate || animationId != lastPlayedAnimation)
                
                // Only update if animation changed or force update is requested
                // We'll also update more frequently (every 20 ticks) for continual animations
                // that might need to be retriggered
                val tickBasedUpdate = System.currentTimeMillis() % 1000 < 50 // Update roughly every second
                
                if (forceUpdate || animationId != lastPlayedAnimation || tickBasedUpdate) {
                    if (shouldLogDebug) {
                        plugin.logger.info("[Debug] Player ${player.name} state change: playing animation '$animationId' for model: $modelID (previous: $lastPlayedAnimation)")
                    }
                    
                    // Try to play the animation, first with the direct API, then with the bridge
                    var success = false
                    
                    // Try with direct ModelEngine API first if available
                    if (modelEngineModel != null) {
                        success = playAnimationWithDirectAPI(modelEngineModel!!, animationId)
                    }
                    
                    // If direct API failed or wasn't available, use the bridge approach
                    if (!success && activeModel != null) {
                        tryAllAnimationMethods(activeModel!!, animationId)
                    }
                    
                    // Update last played animation only if it changed
                    if (animationId != lastPlayedAnimation) {
                        lastPlayedAnimation = animationId
                    }
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
     * Play animation using the direct ModelEngine API via reflection
     */
    private fun playAnimationWithDirectAPI(model: Any, animationId: String): Boolean {
        try {
            if (debugEnabled) {
                plugin.logger.info("[Debug] Attempting to play animation '$animationId' using direct ModelEngine API")
            }
            
            // Get the animation handler from the active model using reflection
            val getAnimationHandlerMethod = model.javaClass.getMethod("getAnimationHandler")
            val animationHandler = getAnimationHandlerMethod.invoke(model)
            
            if (animationHandler != null) {
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Got animation handler: ${animationHandler.javaClass.simpleName}")
                    plugin.logger.info("[Debug] Animation handler methods: ${animationHandler.javaClass.methods.joinToString(", ") { it.name }}")
                }
                
                // First try the standard playAnimation method with the correct signature from R4.0.8
                // The signature is: playAnimation(String animation, double lerpIn, double lerpOut, double speed, boolean force)
                try {
                    val playAnimMethod = animationHandler.javaClass.getMethod(
                        "playAnimation", 
                        String::class.java, 
                        Double::class.javaPrimitiveType, 
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType
                    )
                    
                    // Call with standard parameters: lerpIn=0.2, lerpOut=0.2, speed=1.0, force=true
                    playAnimMethod.invoke(animationHandler, animationId, 0.2, 0.2, 1.0, true)
                    
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Successfully played animation using standard 5-param playAnimation")
                    }
                    
                    return true
                } catch (e: Exception) {
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Error using standard playAnimation method: ${e.message}")
                    }
                }
                
                // Try playState method with different parameter types
                val playStates = animationHandler.javaClass.methods.filter { it.name == "playState" }
                if (playStates.isNotEmpty()) {
                    val playState = playStates.first()
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Found playState method: ${playState.parameterTypes.joinToString(", ") { it.name }}")
                    }
                    
                    // Check if it's an enum parameter
                    if (playState.parameterTypes.isNotEmpty() && playState.parameterTypes[0].isEnum) {
                        val enumClass = playState.parameterTypes[0]
                        val enumConstants = enumClass.enumConstants
                        
                        if (enumConstants != null) {
                            if (debugEnabled) {
                                plugin.logger.info("[Debug] Found enum parameters: ${enumConstants.joinToString(", ")}")
                            }
                            
                            // Try to find matching enum constant
                            val matchingConstant = enumConstants.find { 
                                it.toString().equals(animationId, ignoreCase = true) 
                            }
                            
                            if (matchingConstant != null) {
                                playState.invoke(animationHandler, matchingConstant)
                                if (debugEnabled) {
                                    plugin.logger.info("[Debug] Successfully played animation with enum: $matchingConstant")
                                }
                                return true
                            } else {
                                if (debugEnabled) {
                                    plugin.logger.info("[Debug] No matching enum found for '$animationId', available values: ${enumConstants.joinToString()}")
                                }
                            }
                        }
                    } else {
                        // Try with string parameter
                        try {
                            playState.invoke(animationHandler, animationId)
                            if (debugEnabled) {
                                plugin.logger.info("[Debug] Successfully played animation with string parameter: $animationId")
                            }
                            return true
                        } catch (e: Exception) {
                            if (debugEnabled) {
                                plugin.logger.info("[Debug] Failed to play animation with string: ${e.message}")
                            }
                        }
                    }
                }
                
                // Try other playAnimation signatures
                val playAnimationMethods = animationHandler.javaClass.methods.filter { it.name == "playAnimation" }
                
                for (method in playAnimationMethods) {
                    try {
                        if (method.parameterCount == 1 && method.parameterTypes[0] == String::class.java) {
                            // Single parameter version
                            method.invoke(animationHandler, animationId)
                            if (debugEnabled) {
                                plugin.logger.info("[Debug] Successfully played animation with 1-param playAnimation")
                            }
                            return true
                        } else if (method.parameterCount == 5) {
                            // Check if this is a different 5-param method than the one we tried above
                            var differentSignature = false
                            for (i in 0 until method.parameterTypes.size) {
                                val paramType = method.parameterTypes[i]
                                if ((i == 0 && paramType != String::class.java) ||
                                    (i > 0 && i < 4 && paramType != Double::class.javaPrimitiveType) ||
                                    (i == 4 && paramType != Boolean::class.javaPrimitiveType)) {
                                    differentSignature = true
                                    break
                                }
                            }
                            
                            if (differentSignature) {
                                // Try to adapt to the method's parameter types
                                val args = Array<Any?>(5) { null }
                                args[0] = animationId
                                
                                for (i in 1..4) {
                                    val paramType = method.parameterTypes[i]
                                    args[i] = when {
                                        paramType == Double::class.javaPrimitiveType -> 1.0
                                        paramType == Float::class.javaPrimitiveType -> 1.0f
                                        paramType == Int::class.javaPrimitiveType -> 1
                                        paramType == Boolean::class.javaPrimitiveType -> true
                                        paramType == String::class.java -> "INSTANT"
                                        else -> null
                                    }
                                }
                                
                                method.invoke(animationHandler, *args)
                                if (debugEnabled) {
                                    plugin.logger.info("[Debug] Successfully played animation with adapted 5-param playAnimation")
                                }
                                return true
                            }
                        }
                    } catch (e: Exception) {
                        if (debugEnabled) {
                            plugin.logger.info("[Debug] Failed to play animation with method ${method.name}: ${e.message}")
                        }
                    }
                }
                
                // Try any other animation-related methods
                for (methodName in listOf("play", "setAnimation", "setState")) {
                    val methods = animationHandler.javaClass.methods.filter { it.name == methodName }
                    for (method in methods) {
                        try {
                            if (method.parameterCount == 1 && method.parameterTypes[0].isAssignableFrom(String::class.java)) {
                                method.invoke(animationHandler, animationId)
                                if (debugEnabled) {
                                    plugin.logger.info("[Debug] Successfully played animation with $methodName")
                                }
                                return true
                            }
                        } catch (e: Exception) {
                            // Ignore and try next method
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.info("[Debug] Error playing animation with direct API: ${e.message}")
            }
        }
        
        return false
    }
    
    /**
     * Try multiple approaches to play an animation on a ModelEngine model
     */
    private fun tryAllAnimationMethods(model: Any, animationId: String) {
        val modelClass = model.javaClass
        var success = false
        
        // First, let's check if this is a V4ActiveModel and try to get the handle
        if (modelClass.simpleName == "V4ActiveModel") {
            try {
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Detected V4ActiveModel, trying to get handle")
                }
                
                val getHandleMethod = modelClass.getMethod("getHandle")
                val modelHandle = getHandleMethod.invoke(model)
                
                if (modelHandle != null) {
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Got model handle: ${modelHandle.javaClass.simpleName}")
                    }
                    
                    // Try to play animation on the handle instead
                    success = tryAnimationMethodsOnHandle(modelHandle, animationId)
                }
            } catch (e: Exception) {
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Error getting handle from V4ActiveModel: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }
        
        // If we haven't succeeded with the handle, try the original methods
        if (!success) {
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
            
            // Method 3: Try to use the animation handler (especially for PriorityHandler)
            if (!success) {
                try {
                    // Try to get animation handler
                    val getAnimationHandlerMethod = modelClass.getMethod("getAnimationHandler")
                    val animHandler = getAnimationHandlerMethod.invoke(model)
                    
                    if (animHandler != null) {
                        val handlerClass = animHandler.javaClass
                        if (debugEnabled) {
                            plugin.logger.info("[Debug] Found animation handler: ${handlerClass.simpleName}")
                            plugin.logger.info("[Debug] Animation handler methods: ${handlerClass.methods.joinToString(", ") { it.name }}")
                        }
                        
                        // Try various animation methods
                        success = tryCommonAnimationMethods(animHandler, animationId)
                    }
                } catch (e: Exception) {
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Couldn't play animation with animation handler: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            }
            
            // Method 4: Try reflection on model methods (original Method 3)
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
        }
        
        if (!success) {
            plugin.logger.warning("Failed to play animation for model: $modelID, animation: $animationId - No suitable method found")
        }
    }
    
    /**
     * Try to play animation on the model handle
     */
    private fun tryAnimationMethodsOnHandle(handle: Any, animationId: String): Boolean {
        if (debugEnabled) {
            plugin.logger.info("[Debug] Trying animation methods on model handle class: ${handle.javaClass.simpleName}")
            plugin.logger.info("[Debug] Handle methods: ${handle.javaClass.methods.joinToString(", ") { it.name }}")
        }
        
        var success = false
        
        // Method 1: Try to get Animation Handler from handle
        try {
            val getAnimationHandlerMethod = handle.javaClass.getMethod("getAnimationHandler")
            val animHandler = getAnimationHandlerMethod.invoke(handle)
            
            if (animHandler != null) {
                val handlerClass = animHandler.javaClass
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Found handle animation handler: ${handlerClass.simpleName}")
                    plugin.logger.info("[Debug] Animation handler methods: ${handlerClass.methods.joinToString(", ") { it.name }}")
                }
                
                // Try various animation methods
                success = tryCommonAnimationMethods(animHandler, animationId)
            }
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.info("[Debug] Couldn't get animation handler from handle: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        
        // Method 2: Try direct animation methods on handle if handler approach failed
        if (!success) {
            for (methodName in listOf("playAnimation", "play", "playState", "setAnimation", "animate")) {
                try {
                    val method = handle.javaClass.getMethod(methodName, String::class.java)
                    method.invoke(handle, animationId)
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Successfully played animation on handle using direct method '$methodName': $animationId")
                    }
                    return true
                } catch (e: Exception) {
                    // Continue to next method
                }
            }
        }
        
        return success
    }
    
    /**
     * Try common animation methods on a handler object
     */
    private fun tryCommonAnimationMethods(handler: Any, animationId: String): Boolean {
        // First, try the standard R4.0.8 playAnimation method with correct signature
        try {
            val playAnimMethod = handler.javaClass.getMethod(
                "playAnimation", 
                String::class.java, 
                Double::class.javaPrimitiveType, 
                Double::class.javaPrimitiveType,
                Double::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            
            // Call with standard parameters: lerpIn=0.2, lerpOut=0.2, speed=1.0, force=true
            playAnimMethod.invoke(handler, animationId, 0.2, 0.2, 1.0, true)
            
            if (debugEnabled) {
                plugin.logger.info("[Debug] Successfully played animation using standard 5-param playAnimation in common methods")
            }
            
            return true
        } catch (e: Exception) {
            if (debugEnabled) {
                plugin.logger.info("[Debug] Error using standard playAnimation method in common methods: ${e.message}")
            }
        }
    
        // Try playState method with different parameter types
        val playStateMethods = handler.javaClass.methods.filter { it.name == "playState" }
        if (playStateMethods.isNotEmpty()) {
            for (method in playStateMethods) {
                // Handle enum parameters
                if (method.parameterCount == 1 && method.parameterTypes[0].isEnum) {
                    val enumClass = method.parameterTypes[0]
                    val enumValues = enumClass.enumConstants
                    val enumValue = enumValues?.find { it.toString().equals(animationId, ignoreCase = true) }
                    
                    if (enumValue != null) {
                        try {
                            method.invoke(handler, enumValue)
                            if (debugEnabled) {
                                plugin.logger.info("[Debug] Successfully played animation using enum playState: $animationId")
                            }
                            return true
                        } catch (e: Exception) {
                            if (debugEnabled) {
                                plugin.logger.info("[Debug] Failed to call playState with enum: ${e.message}")
                            }
                        }
                    } else if (debugEnabled && enumValues != null) {
                        plugin.logger.info("[Debug] No matching enum for '$animationId'. Available: ${enumValues.joinToString()}")
                    }
                } 
                // Handle String parameters
                else if (method.parameterCount == 1 && method.parameterTypes[0] == String::class.java) {
                    try {
                        method.invoke(handler, animationId)
                        if (debugEnabled) {
                            plugin.logger.info("[Debug] Successfully played animation using String playState: $animationId")
                        }
                        return true
                    } catch (e: Exception) {
                        if (debugEnabled) {
                            plugin.logger.info("[Debug] Failed to call playState with String: ${e.message}")
                        }
                    }
                }
            }
        }
        
        // Try playAnimation methods
        val playAnimationMethods = handler.javaClass.methods.filter { it.name == "playAnimation" }
        for (method in playAnimationMethods) {
            try {
                if (method.parameterCount == 1 && method.parameterTypes[0] == String::class.java) {
                    method.invoke(handler, animationId)
                    if (debugEnabled) {
                        plugin.logger.info("[Debug] Successfully played animation using playAnimation(String): $animationId")
                    }
                    return true
                } else if (method.parameterCount == 5) {
                    // Check if this is a different 5-param method than the one we tried above
                    var differentSignature = false
                    for (i in 0 until method.parameterTypes.size) {
                        val paramType = method.parameterTypes[i]
                        if ((i == 0 && paramType != String::class.java) ||
                            (i > 0 && i < 4 && paramType != Double::class.javaPrimitiveType) ||
                            (i == 4 && paramType != Boolean::class.javaPrimitiveType)) {
                            differentSignature = true
                            break
                        }
                    }
                    
                    if (differentSignature) {
                        // Try to adapt to the method's parameter types
                        val args = Array<Any?>(5) { null }
                        args[0] = animationId
                        
                        for (i in 1..4) {
                            val paramType = method.parameterTypes[i]
                            args[i] = when {
                                paramType == Double::class.javaPrimitiveType -> 1.0
                                paramType == Float::class.javaPrimitiveType -> 1.0f
                                paramType == Int::class.javaPrimitiveType -> 1
                                paramType == Boolean::class.javaPrimitiveType -> true
                                paramType == String::class.java -> "INSTANT"
                                else -> null
                            }
                        }
                        
                        method.invoke(handler, *args)
                        if (debugEnabled) {
                            plugin.logger.info("[Debug] Successfully played animation with adapted 5-param playAnimation")
                        }
                        return true
                    }
                }
            } catch (e: Exception) {
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Failed to call playAnimation: ${e.message}")
                }
            }
        }
        
        // Try other animation-related methods
        for (methodName in listOf("play", "setAnimation", "setState")) {
            try {
                val method = handler.javaClass.getMethod(methodName, String::class.java)
                method.invoke(handler, animationId)
                if (debugEnabled) {
                    plugin.logger.info("[Debug] Successfully played animation using $methodName: $animationId")
                }
                return true
            } catch (e: Exception) {
                // Try next method
            }
        }
        
        return false
    }
}
