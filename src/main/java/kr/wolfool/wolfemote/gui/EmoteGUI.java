package kr.wolfool.wolfemote.gui;

import kr.wolfool.wolfemote.WolfEmote;
import kr.wolfool.wolfemote.model.Emote;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class EmoteGUI implements Listener {

    private final WolfEmote plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public EmoteGUI(WolfEmote plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        int rows = plugin.getConfig().getInt("gui.rows", 2);
        String title = plugin.getConfig().getString("gui.title", "이모트 선택");
        Inventory gui = Bukkit.createInventory(null, rows * 9, Component.text(title));

        int slot = 0;
        for (Emote emote : plugin.getEmotes().values()) {
            if (slot >= rows * 9) break;

            boolean hasPerm = player.hasPermission(emote.getPermission());
            ItemStack item = new ItemStack(hasPerm ? emote.getMaterial() : org.bukkit.Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(Component.text(emote.getDisplay(),
                    hasPerm ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(List.of(
                    Component.empty(),
                    Component.text(emote.getDescription(), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    hasPerm
                            ? Component.text("▶ 클릭하여 사용", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)
                            : Component.text("✖ 권한 없음", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)
            ));

            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = plugin.getConfig().getString("gui.title", "이모트 선택");
        if (!event.getView().title().equals(Component.text(title))) return;

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;
        if (item.getType() == org.bukkit.Material.GRAY_DYE) {
            player.sendMessage(miniMessage.deserialize(plugin.getMessage("no-permission")));
            return;
        }

        // Find emote by slot
        int slot = event.getSlot();
        int index = 0;
        for (Emote emote : plugin.getEmotes().values()) {
            if (index == slot) {
                player.closeInventory();
                plugin.playEmote(player, emote);
                return;
            }
            index++;
        }
    }
}
