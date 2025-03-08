package com.willfp.ecopets.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecopets.pets.activePet
import com.willfp.ecopets.pets.getPetDisplay
import com.willfp.ecopets.skins.PetSkins
import com.willfp.ecopets.skins.removeActiveSkin
import com.willfp.ecopets.skins.setActiveSkin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil
import java.util.UUID

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
        
        // Allow setting skin for other players (console or operators)
        if (args.size > 1 && (sender.isOp || !(sender is Player))) {
            val targetPlayerName = args[1]
            val targetPlayer = Bukkit.getPlayer(targetPlayerName)
            
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer $targetPlayerName not found or offline!")
                return
            }
            
            // Set the skin for the target player
            PetSkins.setActiveSkin(targetPlayer.uniqueId, skin)
            
            // Force refresh pet entity if the player has an active pet
            val activePet = targetPlayer.activePet
            if (activePet != null) {
                // Remove and recreate pet entity in next tick to refresh appearance
                plugin.scheduler.runLater(1) {
                    val petDisplay = targetPlayer.getPetDisplay()
                    petDisplay?.refresh()
                }
            }
            
            sender.sendMessage("§aSet skin §6${skin.displayName} §afor player §6${targetPlayer.name}§a!")
            targetPlayer.sendMessage(plugin.langYml.getMessage("set-skin")
                .replace("%skin%", skin.displayName))
                
            return
        }
        
        // Original code for player self-setting skin
        if (sender !is Player) {
            sender.sendMessage("§cUsage from console: /pets skin set <skinID> <playerName>")
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
        } else if (args.size == 2 && (sender.isOp || !(sender is Player))) {
            val completions = mutableListOf<String>()
            StringUtil.copyPartialMatches(
                args[1],
                Bukkit.getOnlinePlayers().map { it.name },
                completions
            )
            return completions
        }
        return emptyList()
    }
}

class CommandSkinRemove(plugin: EcoPlugin) : Subcommand(plugin, "remove", "ecopets.command.skin.remove", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        // Allow removing skin for other players (console or operators)
        if (args.isNotEmpty() && (sender.isOp || !(sender is Player))) {
            val targetPlayerName = args[0]
            val targetPlayer = Bukkit.getPlayer(targetPlayerName)
            
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer $targetPlayerName not found or offline!")
                return
            }
            
            val currentSkin = PetSkins.getActiveSkin(targetPlayer.uniqueId)
            if (currentSkin == null) {
                sender.sendMessage("§cPlayer §6${targetPlayer.name} §cdoesn't have an active skin!")
                return
            }
            
            // Remove the skin
            PetSkins.removeSkin(targetPlayer.uniqueId)
            
            // Force refresh pet entity if the player has an active pet
            val activePet = targetPlayer.activePet
            if (activePet != null) {
                // Remove and recreate pet entity in next tick to refresh appearance
                plugin.scheduler.runLater(1) {
                    val petDisplay = targetPlayer.getPetDisplay()
                    petDisplay?.refresh()
                }
            }
            
            sender.sendMessage("§aRemoved skin §6${currentSkin.displayName} §afrom player §6${targetPlayer.name}§a!")
            targetPlayer.sendMessage(plugin.langYml.getMessage("removed-skin")
                .replace("%skin%", currentSkin.displayName))
                
            return
        }
        
        // Original code for player self-removing skin
        if (sender !is Player) {
            sender.sendMessage("§cUsage from console: /pets skin remove <playerName>")
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
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1 && (sender.isOp || !(sender is Player))) {
            val completions = mutableListOf<String>()
            StringUtil.copyPartialMatches(
                args[0],
                Bukkit.getOnlinePlayers().map { it.name },
                completions
            )
            return completions
        }
        return emptyList()
    }
} 