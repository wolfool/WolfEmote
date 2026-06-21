package kr.wolfool.wolfemote.emote;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * BetterModel integration via reflection.
 * Plays model animations on players when BetterModel is available.
 *
 * Uses BetterModel API:
 *   BetterModel.model("model_name") -> Optional<ModelRenderer>
 *   renderer.getOrCreate(BukkitAdapter.adapt(entity)) -> EntityTracker
 *   tracker.animate("animation_name", AnimationModifier.DEFAULT)
 */
public class BetterModelHook {

    private final Logger logger;
    private boolean available = false;

    // Reflected classes and methods
    private Class<?> betterModelClass;
    private Class<?> bukkitAdapterClass;
    private Class<?> animationModifierClass;
    private Method modelMethod;        // BetterModel.model(String)
    private Method adaptMethod;        // BukkitAdapter.adapt(Entity)
    private Method getOrCreateMethod;  // ModelRenderer.getOrCreate(TrackerWrapper)
    private Method animateMethod;      // EntityTracker.animate(String, AnimationModifier)
    private Object defaultModifier;    // AnimationModifier.DEFAULT

    public BetterModelHook(Logger logger) {
        this.logger = logger;
        tryHook();
    }

    private void tryHook() {
        if (Bukkit.getPluginManager().getPlugin("BetterModel") == null) {
            logger.info("BetterModel 미감지 - 파티클 이모트만 사용합니다.");
            return;
        }

        try {
            betterModelClass = Class.forName("kr.toxicity.model.api.BetterModel");
            bukkitAdapterClass = Class.forName("kr.toxicity.model.api.bukkit.BukkitAdapter");
            animationModifierClass = Class.forName("kr.toxicity.model.api.data.animation.AnimationModifier");

            modelMethod = betterModelClass.getMethod("model", String.class);
            adaptMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.entity.Entity.class);

            // Get AnimationModifier.DEFAULT
            defaultModifier = animationModifierClass.getField("DEFAULT").get(null);

            available = true;
            logger.info("BetterModel 연동 성공! 3D 애니메이션 이모트를 사용할 수 있습니다.");
        } catch (Exception e) {
            logger.warning("BetterModel 연동 실패: " + e.getMessage());
            available = false;
        }
    }

    /**
     * Play a BetterModel animation on a player.
     *
     * @param player    The player
     * @param modelId   The model ID (e.g. "emote_dance")
     * @param animName  The animation name
     * @return true if animation was played via BetterModel
     */
    public boolean playAnimation(Player player, String modelId, String animName) {
        if (!available) return false;

        try {
            // Optional<ModelRenderer> = BetterModel.model(modelId)
            Object optionalRenderer = modelMethod.invoke(null, modelId);
            if (optionalRenderer instanceof Optional<?> opt && opt.isPresent()) {
                Object renderer = opt.get();

                // TrackerWrapper = BukkitAdapter.adapt(player)
                Object wrapper = adaptMethod.invoke(null, player);

                // EntityTracker = renderer.getOrCreate(wrapper)
                if (getOrCreateMethod == null) {
                    getOrCreateMethod = renderer.getClass().getMethod("getOrCreate", wrapper.getClass().getInterfaces()[0]);
                }
                Object tracker = getOrCreateMethod.invoke(renderer, wrapper);

                if (tracker != null) {
                    // tracker.animate(animName, AnimationModifier.DEFAULT)
                    if (animateMethod == null) {
                        animateMethod = tracker.getClass().getMethod("animate", String.class, animationModifierClass);
                    }
                    animateMethod.invoke(tracker, animName, defaultModifier);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.fine("BetterModel 애니메이션 재생 실패: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check if BetterModel is available.
     */
    public boolean isAvailable() {
        return available;
    }
}
