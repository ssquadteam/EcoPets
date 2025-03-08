package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.getPetDisplay
import com.willfp.ecopets.skins.PetSkins
import com.willfp.ecopets.skins.removeActiveSkin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandSkin(plugin: EcoPlugin) : Subcommand(plugin, "skin", "ecopets.command.skin", false) {
    init {
        this.addSubcommand(CommandSkinSet(plugin))
        this.addSubcommand(CommandSkinRemove(plugin))
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(plugin.langYml.getMessage("must-specify-skin"))
    }
}

class CommandSkinSet(plugin: EcoPlugin) : Subcommand(plugin, "set", "ecopets.command.skin.set", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (sender !is Player) {
            sender.sendMessage(plugin.langYml.getMessage("not-player"))
            return
        }
        
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("specify-skin-id"))
            return
        }
        
        val skinID = args[0]
        val skin = PetSkins.get(skinID)
        
        if (skin == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-skin")
                .replace("%skin%", skinID))
            return
        }
        
        PetSkins.setActiveSkin(sender.uniqueId, skin)
        
        // Force refresh pet entity if the player has an active pet
        val activePet = sender.activePet
        if (activePet != null) {
            // Remove and recreate pet entity in next tick to refresh appearance
            plugin.scheduler.runLater(1) {
                val petDisplay = sender.getPetDisplay()
                petDisplay?.refresh()
            }
        }
        
        sender.sendMessage(plugin.langYml.getMessage("set-skin")
            .replace("%skin%", skin.displayName))
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1) {
            val completions = mutableListOf<String>()
            StringUtil.copyPartialMatches(
                args[0],
                PetSkins.values().map { it.id },
                completions
            )
            return completions
        }
        return emptyList()
    }
}

class CommandSkinRemove(plugin: EcoPlugin) : Subcommand(plugin, "remove", "ecopets.command.skin.remove", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (sender !is Player) {
            sender.sendMessage(plugin.langYml.getMessage("not-player"))
            return
        }
        
        val currentSkin = PetSkins.getActiveSkin(sender.uniqueId)
        if (currentSkin == null) {
            sender.sendMessage(plugin.langYml.getMessage("no-skin-active"))
            return
        }
        
        sender.removeActiveSkin()
        
        // Force refresh pet entity if the player has an active pet
        val activePet = sender.activePet
        if (activePet != null) {
            // Remove and recreate pet entity in next tick to refresh appearance
            plugin.scheduler.runLater(1) {
                val petDisplay = sender.getPetDisplay()
                petDisplay?.refresh()
            }
        }
        
        sender.sendMessage(plugin.langYml.getMessage("removed-skin")
            .replace("%skin%", currentSkin.displayName))
    }
} 