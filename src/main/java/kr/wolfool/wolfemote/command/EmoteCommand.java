package kr.wolfool.wolfemote.command;

import kr.wolfool.wolfemote.WolfEmote;
import kr.wolfool.wolfemote.gui.EmoteGUI;
import kr.wolfool.wolfemote.model.Emote;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EmoteCommand implements CommandExecutor, TabCompleter {

    private final WolfEmote plugin;
    private final EmoteGUI gui;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public EmoteCommand(WolfEmote plugin) {
        this.plugin = plugin;
        this.gui = new EmoteGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        // /emote reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("wolfemote.admin")) {
                player.sendMessage(miniMessage.deserialize(plugin.getMessage("no-permission")));
                return true;
            }
            plugin.reload();
            player.sendMessage(miniMessage.deserialize(plugin.getMessage("reloaded")));
            return true;
        }

        // /emote <name> - play emote directly
        if (args.length > 0) {
            Emote emote = plugin.getEmotes().get(args[0].toLowerCase());
            if (emote != null) {
                plugin.playEmote(player, emote);
                return true;
            }
        }

        // /emote - open GUI
        gui.open(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(plugin.getEmotes().keySet());
            if (sender.hasPermission("wolfemote.admin")) {
                completions.add("reload");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
