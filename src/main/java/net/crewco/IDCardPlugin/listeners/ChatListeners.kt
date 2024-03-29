package net.crewco.IDCardPlugin.listeners

import com.google.inject.Inject
import net.crewco.IDCardPlugin.IDCardPlugin
import net.crewco.IDCardPlugin.IDCardPlugin.Companion.SynidMsg
import net.crewco.IDCardPlugin.IDCardPlugin.Companion.playerData
import net.crewco.IDCardPlugin.IDCardPlugin.Companion.promptCheck
import net.crewco.IDCardPlugin.IDCardPlugin.Companion.promptManager
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack


class ChatListeners  @Inject constructor(private val plugin: IDCardPlugin) : Listener {
	@EventHandler
	fun onPlayerChat(event: AsyncPlayerChatEvent) {
		val player = event.player
		val prompts = playerData[player]

		if (!prompts.isNullOrEmpty()) {
			event.isCancelled = true // Cancel the normal chat behavior

			val inputList = playerData.computeIfAbsent(player) { mutableListOf() }

			// Save the player's response (limit to a reasonable length)
			val maxResponseLength = plugin.config.getInt("max-length") // Adjust the maximum allowed length as needed
			val response = event.message.substring(0, minOf(event.message.length, maxResponseLength))
			inputList.add(response)

			// Move to the next prompt or generate the ID
			prompts.removeAt(0)
			if (prompts.isNotEmpty() && promptManager.countPromptsSent() != 0 && promptManager.isDone(player)) {
				promptManager.askForInput(player)
			} else {
				// All information collected, generate ID
				generateAndGiveID(player)

				// Removes the player from the promptCheck
				promptCheck.remove(player)

				// Clean up the input map
				playerData.remove(player)
			}
		}
	}

	private fun generateAndGiveID(player: Player) {
		val idPaper = ItemStack(Material.PAPER)
		val meta = idPaper.itemMeta

		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a&lID-Card"))

		// Add player information to the lore of the paper
		val lore: MutableList<String> = ArrayList()
		playerData[player]?.forEachIndexed { index, response ->
			lore.add("${ChatColor.DARK_PURPLE}${promptManager.getPrompt()}: ${ChatColor.DARK_GRAY}$response")
		}

		meta.lore = lore
		meta.addEnchant(Enchantment.DURABILITY, 5, true)
		meta.itemFlags.add(ItemFlag.HIDE_ENCHANTS)
		idPaper.itemMeta = meta

		// Give the paper to the player
		player.inventory.addItem(idPaper)
		player.sendMessage("$SynidMsg Your ID paper has been created and added to your inventory.")
	}
}