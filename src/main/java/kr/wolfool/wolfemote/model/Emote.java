package kr.wolfool.wolfemote.model;

import org.bukkit.Material;
import org.bukkit.Particle;

/**
 * Represents an emote with its properties.
 */
public class Emote {

    public enum EmoteType {
        SIT, WAVE, DANCE, BOW, CRY, LAUGH, POINT, SLEEP, CLAP, FACEPALM
    }

    private final String id;
    private final String display;
    private final EmoteType type;
    private final String permission;
    private final Material material;
    private final String description;
    private final Particle particle;
    private final String sound;
    private final int duration; // ticks

    public Emote(String id, String display, EmoteType type, String permission,
                 Material material, String description, Particle particle,
                 String sound, int duration) {
        this.id = id;
        this.display = display;
        this.type = type;
        this.permission = permission.isEmpty() ? "wolfemote.use" : permission;
        this.material = material;
        this.description = description;
        this.particle = particle;
        this.sound = sound;
        this.duration = duration;
    }

    public String getId() { return id; }
    public String getDisplay() { return display; }
    public EmoteType getType() { return type; }
    public String getPermission() { return permission; }
    public Material getMaterial() { return material; }
    public String getDescription() { return description; }
    public Particle getParticle() { return particle; }
    public String getSound() { return sound; }
    public int getDuration() { return duration; }
    public boolean hasParticle() { return particle != null; }
    public boolean hasSound() { return sound != null && !sound.isEmpty(); }
}
