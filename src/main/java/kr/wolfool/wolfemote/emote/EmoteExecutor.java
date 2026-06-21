package kr.wolfool.wolfemote.emote;

import kr.wolfool.wolfemote.WolfEmote;
import kr.wolfool.wolfemote.model.Emote;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Handles emote execution: sitting, particles, sounds, and animations.
 */
public class EmoteExecutor {

    private final WolfEmote plugin;

    // Track sitting/sleeping players and their armor stands
    private final Map<UUID, ArmorStand> sittingPlayers = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> animatingPlayers = new HashSet<>();

    public EmoteExecutor(WolfEmote plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute an emote for a player.
     * @return true if emote was played
     */
    public boolean execute(Player player, Emote emote) {
        UUID uuid = player.getUniqueId();

        // Check cooldown
        int cdSec = plugin.getConfig().getInt("cooldown", 3);
        Long lastUse = cooldowns.get(uuid);
        if (lastUse != null) {
            long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
            if (elapsed < cdSec) {
                return false; // cooldown active
            }
        }

        // If currently sitting/sleeping, stand up first
        if (sittingPlayers.containsKey(uuid)) {
            standUp(player);
        }

        cooldowns.put(uuid, System.currentTimeMillis());

        switch (emote.getType()) {
            case SIT -> sit(player);
            case SLEEP -> sleep(player);
            default -> playAnimatedEmote(player, emote);
        }

        return true;
    }

    /**
     * Make player sit down using invisible armor stand.
     */
    private void sit(Player player) {
        Location loc = player.getLocation().clone();
        // Spawn invisible armor stand slightly below
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(
                loc.subtract(0, 1.2, 0), EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.addPassenger(player);

        sittingPlayers.put(player.getUniqueId(), stand);
    }

    /**
     * Make player sleep (lay down) using armor stand rotation.
     */
    private void sleep(Player player) {
        Location loc = player.getLocation().clone();
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(
                loc.subtract(0, 1.2, 0), EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.addPassenger(player);

        sittingPlayers.put(player.getUniqueId(), stand);

        // Sleep pose indication with particles
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!sittingPlayers.containsKey(player.getUniqueId()) || !player.isOnline()) {
                    cancel();
                    return;
                }
                // Z particles above head
                Location headLoc = player.getLocation().add(0, 1.5, 0);
                double offsetX = Math.random() * 0.5 - 0.25;
                double offsetY = Math.random() * 0.3;
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        headLoc.add(offsetX, offsetY, offsetX), 1, 0, 0, 0, 0);
                ticks += 20;
                if (ticks > 6000) cancel(); // Max 5 min
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Play an animated emote with particles and sound.
     */
    private void playAnimatedEmote(Player player, Emote emote) {
        UUID uuid = player.getUniqueId();
        if (animatingPlayers.contains(uuid)) return;
        animatingPlayers.add(uuid);

        // Play sound
        if (emote.hasSound()) {
            Sound sound = Sound.sound(
                    Key.key(emote.getSound()),
                    Sound.Source.PLAYER,
                    1.0f, 1.0f
            );
            player.getWorld().playSound(sound, player.getX(), player.getY(), player.getZ());
        }

        // Swing arm
        player.swingMainHand();

        // Particle animation
        if (emote.hasParticle()) {
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = emote.getDuration();

                @Override
                public void run() {
                    if (ticks >= maxTicks || !player.isOnline()) {
                        animatingPlayers.remove(uuid);
                        cancel();
                        return;
                    }

                    Location loc = player.getLocation().add(0, 1.0, 0);

                    switch (emote.getType()) {
                        case DANCE -> {
                            // Circle particles around player
                            double angle = (ticks * 18) * Math.PI / 180;
                            double x = Math.cos(angle) * 1.0;
                            double z = Math.sin(angle) * 1.0;
                            player.getWorld().spawnParticle(emote.getParticle(),
                                    loc.add(x, Math.sin(ticks * 0.2) * 0.5, z),
                                    2, 0.1, 0.1, 0.1, 0);
                            // Head bobbing effect by periodic sneaking
                            if (ticks % 10 == 0) player.swingMainHand();
                        }
                        case WAVE -> {
                            // Particles above head
                            player.getWorld().spawnParticle(emote.getParticle(),
                                    loc.add(0, 1.0, 0), 1, 0.3, 0.1, 0.3, 0);
                            if (ticks % 5 == 0) player.swingMainHand();
                        }
                        case BOW -> {
                            // Subtle particles at feet
                            if (ticks < 10) {
                                player.getWorld().spawnParticle(Particle.CLOUD,
                                        player.getLocation().add(0, 0.1, 0),
                                        1, 0.2, 0, 0.2, 0.01);
                            }
                        }
                        case CRY -> {
                            // Dripping water from eyes
                            Location eyeLoc = player.getEyeLocation();
                            player.getWorld().spawnParticle(emote.getParticle(),
                                    eyeLoc.add(0.15, -0.1, 0), 1, 0, 0, 0, 0);
                            player.getWorld().spawnParticle(emote.getParticle(),
                                    eyeLoc.add(-0.3, 0, 0), 1, 0, 0, 0, 0);
                        }
                        case LAUGH -> {
                            // Happy particles
                            player.getWorld().spawnParticle(emote.getParticle(),
                                    loc.add(0, 0.5, 0), 2, 0.3, 0.3, 0.3, 0);
                        }
                        case POINT -> {
                            // Line of particles in look direction
                            Location start = player.getEyeLocation();
                            var dir = start.getDirection().normalize().multiply(0.5);
                            for (int i = 1; i <= 4; i++) {
                                Location point = start.clone().add(dir.clone().multiply(i));
                                player.getWorld().spawnParticle(emote.getParticle(),
                                        point, 1, 0, 0, 0, 0);
                            }
                        }
                        case CLAP -> {
                            // Firework particles
                            if (ticks % 5 == 0) {
                                player.getWorld().spawnParticle(emote.getParticle(),
                                        loc.add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0);
                                player.swingMainHand();
                            }
                        }
                        case FACEPALM -> {
                            // Smoke from head
                            player.getWorld().spawnParticle(emote.getParticle(),
                                    player.getEyeLocation().add(0, 0.3, 0),
                                    1, 0.1, 0.1, 0.1, 0.01);
                        }
                        default -> {
                            player.getWorld().spawnParticle(emote.getParticle(),
                                    loc, 3, 0.3, 0.3, 0.3, 0);
                        }
                    }

                    ticks += 2;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        } else {
            animatingPlayers.remove(uuid);
        }
    }

    /**
     * Stand up from sitting/sleeping.
     */
    public void standUp(Player player) {
        ArmorStand stand = sittingPlayers.remove(player.getUniqueId());
        if (stand != null) {
            stand.removePassenger(player);
            stand.remove();
        }
    }

    /**
     * Check if a player is sitting or sleeping.
     */
    public boolean isSitting(UUID uuid) {
        return sittingPlayers.containsKey(uuid);
    }

    /**
     * Get remaining cooldown in seconds.
     */
    public int getRemainingCooldown(UUID uuid) {
        Long lastUse = cooldowns.get(uuid);
        if (lastUse == null) return 0;
        int cdSec = plugin.getConfig().getInt("cooldown", 3);
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
        return Math.max(0, cdSec - (int) elapsed);
    }

    /**
     * Cleanup on disable.
     */
    public void cleanup() {
        for (var entry : sittingPlayers.entrySet()) {
            ArmorStand stand = entry.getValue();
            if (stand != null && !stand.isDead()) {
                stand.getPassengers().forEach(stand::removePassenger);
                stand.remove();
            }
        }
        sittingPlayers.clear();
        animatingPlayers.clear();
    }
}
