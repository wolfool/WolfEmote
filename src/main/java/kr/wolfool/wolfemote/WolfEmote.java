package kr.wolfool.wolfemote;

import kr.wolfool.wolfemote.command.EmoteCommand;
import kr.wolfool.wolfemote.emote.BetterModelHook;
import kr.wolfool.wolfemote.emote.EmoteExecutor;
import kr.wolfool.wolfemote.model.Emote;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class WolfEmote extends JavaPlugin implements Listener {

    private static WolfEmote instance;
    private final Map<String, Emote> emotes = new LinkedHashMap<>();
    private EmoteExecutor executor;
    private BetterModelHook betterModelHook;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadEmotes();

        executor = new EmoteExecutor(this);
        betterModelHook = new BetterModelHook(getLogger());

        var cmd = getCommand("emote");
        if (cmd != null) {
            EmoteCommand emoteCmd = new EmoteCommand(this);
            cmd.setExecutor(emoteCmd);
            cmd.setTabCompleter(emoteCmd);
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WolfEmote v" + getDescription().getVersion() + " 활성화!");
    }

    @Override
    public void onDisable() {
        if (executor != null) executor.cleanup();
        getLogger().info("WolfEmote 비활성화!");
    }

    private void loadEmotes() {
        emotes.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("emotes");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            ConfigurationSection e = sec.getConfigurationSection(key);
            if (e == null) continue;

            try {
                String display = e.getString("display", key);
                Emote.EmoteType type;
                try {
                    type = Emote.EmoteType.valueOf(e.getString("type", "WAVE").toUpperCase());
                } catch (IllegalArgumentException ex) {
                    type = Emote.EmoteType.WAVE;
                }

                String permission = e.getString("permission", "");
                Material material;
                try {
                    material = Material.valueOf(e.getString("material", "NAME_TAG").toUpperCase());
                } catch (IllegalArgumentException ex) {
                    material = Material.NAME_TAG;
                }

                String description = e.getString("description", "");
                Particle particle = null;
                String particleStr = e.getString("particle", "NONE");
                if (!particleStr.equalsIgnoreCase("NONE")) {
                    try {
                        particle = Particle.valueOf(particleStr.toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                String sound = e.getString("sound", "");
                int duration = e.getInt("duration", 30);
                String modelId = e.getString("model-id", "");
                String animation = e.getString("animation", "");

                emotes.put(key, new Emote(key, display, type, permission, material,
                        description, particle, sound, duration, modelId, animation));
            } catch (Exception ex) {
                getLogger().warning("이모트 로드 실패: " + key);
            }
        }

        getLogger().info(emotes.size() + "개의 이모트가 로드되었습니다.");
    }

    /**
     * Play an emote for a player with message broadcast.
     */
    public void playEmote(Player player, Emote emote) {
        if (!player.hasPermission(emote.getPermission())) {
            player.sendMessage(miniMessage.deserialize(getMessage("no-permission")));
            return;
        }

        int remaining = executor.getRemainingCooldown(player.getUniqueId());
        if (remaining > 0) {
            player.sendMessage(miniMessage.deserialize(
                    getMessage("cooldown").replace("{time}", String.valueOf(remaining))));
            return;
        }

        boolean played = executor.execute(player, emote);
        if (!played) return;

        // Send message
        if (emote.getType() == Emote.EmoteType.SIT) {
            player.sendMessage(miniMessage.deserialize(getMessage("sitting")));
        } else if (emote.getType() == Emote.EmoteType.SLEEP) {
            player.sendMessage(miniMessage.deserialize(getMessage("sleeping")));
        } else {
            // Broadcast to nearby players
            String msg = getMessage("playing")
                    .replace("{player}", player.getName())
                    .replace("{emote}", emote.getDisplay());
            for (Player nearby : player.getWorld().getPlayers()) {
                if (nearby.getLocation().distance(player.getLocation()) <= 30) {
                    nearby.sendMessage(miniMessage.deserialize(msg));
                }
            }
        }
    }

    /**
     * Stand up when player moves (if sitting/sleeping).
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!executor.isSitting(event.getPlayer().getUniqueId())) return;

        // Only trigger on actual movement, not head rotation
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            executor.standUp(event.getPlayer());
            event.getPlayer().sendMessage(miniMessage.deserialize(getMessage("stand-up")));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (executor.isSitting(event.getPlayer().getUniqueId())) {
            executor.standUp(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (executor.isSitting(event.getPlayer().getUniqueId())) {
            executor.standUp(event.getPlayer());
        }
    }

    public void reload() {
        reloadConfig();
        loadEmotes();
    }

    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "<red>메시지 없음: " + key + "</red>");
    }

    public static WolfEmote getInstance() { return instance; }
    public Map<String, Emote> getEmotes() { return emotes; }
    public EmoteExecutor getExecutor() { return executor; }
    public BetterModelHook getBetterModelHook() { return betterModelHook; }
}
