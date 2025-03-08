package com.willfp.ecopets.skins

import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.Registry
import com.willfp.ecopets.EcoPetsPlugin
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.loader.configs.LegacyLocation
import java.io.File
import java.util.UUID

object PetSkins : ConfigCategory("skin", "skins") {
    private val registry = Registry<PetSkin>()
    private val activeSkins = mutableMapOf<UUID, String>()
    
    override val legacyLocation = LegacyLocation(
        "skins.yml",
        "skins"
    )
    
    /**
     * Get all registered [PetSkin]s.
     *
     * @return A list of all [PetSkin]s.
     */
    @JvmStatic
    fun values(): List<PetSkin> {
        return ImmutableList.copyOf(registry.values())
    }
    
    /**
     * Get [PetSkin] matching ID.
     *
     * @param name The name to search for.
     * @return The matching [PetSkin], or null if not found.
     */
    @JvmStatic
    fun getByID(name: String): PetSkin? {
        return registry[name]
    }
    
    /**
     * Get [PetSkin] matching ID.
     *
     * @param name The name to search for.
     * @return The matching [PetSkin], or null if not found.
     */
    @JvmStatic
    fun get(name: String): PetSkin? {
        return registry[name]
    }
    
    fun getActiveSkin(uuid: UUID): PetSkin? {
        val skinID = activeSkins[uuid] ?: return null
        return this.get(skinID)
    }
    
    fun setActiveSkin(uuid: UUID, skin: PetSkin) {
        activeSkins[uuid] = skin.id
        saveSkinData()
    }
    
    fun removeSkin(uuid: UUID) {
        activeSkins.remove(uuid)
        saveSkinData()
    }
    
    fun isSkinActive(uuid: UUID, skinID: String): Boolean {
        return activeSkins[uuid] == skinID
    }
    
    fun saveSkinData() {
        val plugin = EcoPetsPlugin.instance
        val file = File(plugin.dataFolder, "skindata.yml")
        
        try {
            // Create a simple YAML file
            val yaml = java.util.Properties()
            
            // Save all active skins
            for ((uuid, skinID) in activeSkins) {
                yaml.setProperty(uuid.toString(), skinID)
            }
            
            // Save the file
            file.outputStream().use { 
                yaml.store(it, "EcoPets Skin Data") 
            }
            
            plugin.logger.info("Saved skin data for ${activeSkins.size} players")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save skin data: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun loadSkinData() {
        val plugin = EcoPetsPlugin.instance
        val file = File(plugin.dataFolder, "skindata.yml")
        
        try {
            if (!file.exists()) {
                plugin.logger.info("No skin data file found, creating new one")
                saveSkinData() // Create the file if it doesn't exist
                return
            }
            
            // Load properties file
            val yaml = java.util.Properties()
            file.inputStream().use { yaml.load(it) }
            
            activeSkins.clear() // Clear existing data before loading
            
            var loadedCount = 0
            for (key in yaml.stringPropertyNames()) {
                try {
                    val uuid = UUID.fromString(key)
                    val skinID = yaml.getProperty(key)
                    
                    // Only load valid skins
                    if (get(skinID) != null) {
                        activeSkins[uuid] = skinID
                        loadedCount++
                    } else {
                        plugin.logger.warning("Skipping invalid skin ID in skindata.yml: $skinID")
                    }
                } catch (e: IllegalArgumentException) {
                    plugin.logger.warning("Invalid UUID in skindata.yml: $key")
                }
            }
            
            plugin.logger.info("Loaded skin data for $loadedCount players")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load skin data: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }
    
    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        registry.register(PetSkin(id, config))
        
        // Load skin data only once after all skins are registered
        if (registry.values().size == 1) {
            plugin.scheduler.runLater(1) {
                loadSkinData()
            }
        }
    }
    
    override fun afterReload(plugin: LibreforgePlugin) {
        val skinsDirectory = File(plugin.dataFolder, "skins")
        if (!skinsDirectory.exists()) {
            skinsDirectory.mkdirs()
            
            try {
                // Create example skin
                val exampleFile = File(skinsDirectory, "example.yml")
                if (!exampleFile.exists()) {
                    // Create a simple example skin file
                    val exampleContent = """
                        # Example skin configuration
                        display-name: "&aExample Skin"
                        texture-head-id: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjVjNGQyNGFmZmRkNDgxMjI1ZTNlM2MxMmNiOWU1YzYzNWJlN2Y1YzUzYTA5YzY4OWNkNzU5ZjRhODZkYjEifX19"
                        modelengine-id: "example_model"
                        use-modelengine: false
                    """.trimIndent()
                    
                    exampleFile.writeText(exampleContent)
                    plugin.logger.info("Created example skin file")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to create example skin: ${e.message}")
            }
        }
        
        // Load skin data when plugin reloads
        loadSkinData()
    }
} 