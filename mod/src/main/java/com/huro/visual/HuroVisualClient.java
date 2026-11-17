package com.huro.visual;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class HuroVisualClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("huro_visual");

    public static final ParticleEffect[] PARTICLES = {
        ParticleTypes.CRIT, ParticleTypes.ENCHANTED_HIT, ParticleTypes.HEART, ParticleTypes.FLAME,
        ParticleTypes.TOTEM_OF_UNDYING, ParticleTypes.END_ROD, ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.ELECTRIC_SPARK
    };
    public static final String[] PARTICLE_NAMES = {
        "Криты", "Магия", "Сердца", "Огонь", "Тотем", "Искры End", "Синий огонь", "Молния"
    };
    public static final SoundEvent[] SOUNDS = {
        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundEvents.ENTITY_ARROW_HIT_PLAYER,
        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundEvents.ENTITY_ITEM_PICKUP
    };
    public static final String[] SOUND_NAMES = {"Динь (орб)", "Попадание", "Крит", "Левел-ап", "Подбор"};

    public static final String[] WEATHER_NAMES = {"Серверная", "Ясно", "Дождь", "Гроза"};

    public static KeyBinding menuKey;
    public static KeyBinding markerKey;

    public static int flashTicks = 0;
    public static int combo = 0;
    public static long lastHitMs = 0;

    private static final Random RND = new Random();
    private static LivingEntity lastTarget;
    private static boolean nightVisionAdded = false;

    @Override
    public void onInitializeClient() {
        HuroConfig.load();
        HuroData.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.huro_visual.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.huro_visual"));
        markerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.huro_visual.marker", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.huro_visual"));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && entity instanceof LivingEntity living) {
                onHit(MinecraftClient.getInstance(), living);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(HuroVisualClient::tick);
        HudRenderCallback.EVENT.register(HuroHud::render);
        LOG.info("HURO VISUAL loaded. Press Right Shift in-game to open the menu.");
    }

    // ------------------------------------------------------------------ hits

    private static boolean isFriend(Entity e) {
        return e instanceof PlayerEntity p && HuroData.get().isFriend(p.getName().getString());
    }

    private static void onHit(MinecraftClient mc, LivingEntity target) {
        if (mc.player == null || mc.world == null) return;
        HuroConfig c = HuroConfig.get();

        long now = System.currentTimeMillis();
        if (now - lastHitMs > 3000) combo = 0;
        combo++;
        lastHitMs = now;
        lastTarget = target;

        if (c.noEffectsOnFriends && isFriend(target)) return;

        if (c.hitParticles) {
            ParticleEffect type = PARTICLES[Math.floorMod(c.hitParticleType, PARTICLES.length)];
            spawnBurst(target, type, c.hitParticleCount, 0.15);
        }
        if (c.hitSound) {
            SoundEvent s = SOUNDS[Math.floorMod(c.hitSoundType, SOUNDS.length)];
            mc.getSoundManager().play(PositionedSoundInstance.master(s, c.hitSoundPitch, c.hitSoundVolume));
        }
        if (c.hitFlash) flashTicks = 6;
        if (c.hitKick) {
            mc.player.setPitch(mc.player.getPitch() - c.hitKickAmount);
        }
    }

    private static void spawnBurst(Entity e, ParticleEffect type, int count, double speed) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        Vec3d p = e.getPos().add(0, e.getHeight() * 0.5, 0);
        double w = e.getWidth() * 0.5;
        for (int i = 0; i < count; i++) {
            mc.world.addParticle(type,
                p.x + (RND.nextDouble() - 0.5) * w * 2, p.y + (RND.nextDouble() - 0.5) * e.getHeight() * 0.8,
                p.z + (RND.nextDouble() - 0.5) * w * 2,
                (RND.nextDouble() - 0.5) * speed * 2, RND.nextDouble() * speed, (RND.nextDouble() - 0.5) * speed * 2);
        }
    }

    // ------------------------------------------------------------------ tick

    private static void tick(MinecraftClient mc) {
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new HuroScreen());
        }
        while (markerKey.wasPressed()) {
            if (mc.player != null && mc.world != null) {
                HuroData.get().addMarker("", mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.world.getRegistryKey().getValue().toString());
            }
        }
        if (mc.player == null || mc.world == null) {
            nightVisionAdded = false;
            lastTarget = null;
            return;
        }
        HuroConfig c = HuroConfig.get();
        if (flashTicks > 0) flashTicks--;

        // Fullbright through a client-side hidden night vision effect
        if (c.fullbright) {
            if (mc.player.getStatusEffect(StatusEffects.NIGHT_VISION) == null) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1_000_000, 0, true, false, false));
                nightVisionAdded = true;
            }
        } else if (nightVisionAdded) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            nightVisionAdded = false;
        }

        // Client-side time and weather
        if (c.timeOverride) mc.world.setTimeOfDay(c.customTime);
        switch (c.weatherMode) {
            case 1 -> { mc.world.setRainGradient(0f); mc.world.setThunderGradient(0f); }
            case 2 -> { mc.world.setRainGradient(1f); mc.world.setThunderGradient(0f); }
            case 3 -> { mc.world.setRainGradient(1f); mc.world.setThunderGradient(1f); }
            default -> { }
        }

        // Kill effect
        if (lastTarget != null) {
            if (!lastTarget.isAlive() || lastTarget.isRemoved()) {
                if (c.killEffect && !(c.noEffectsOnFriends && isFriend(lastTarget))) {
                    spawnBurst(lastTarget, ParticleTypes.TOTEM_OF_UNDYING, 30, 0.35);
                    mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2f, 0.8f));
                }
                lastTarget = null;
            } else if (System.currentTimeMillis() - lastHitMs > 4000) {
                lastTarget = null;
            }
        }
    }

    // ------------------------------------------------------------------ presets

    /** Lowers heavy vanilla options for more FPS. */
    public static void applyPerformancePreset(MinecraftClient mc) {
        GameOptions o = mc.options;
        o.getParticles().setValue(ParticlesMode.MINIMAL);
        o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        o.getEntityShadows().setValue(false);
        o.getBiomeBlendRadius().setValue(1);
        o.getViewDistance().setValue(8);
        o.getSimulationDistance().setValue(6);
        o.getEnableVsync().setValue(false);
        o.getMaxFps().setValue(260);
        o.write();
    }

    public static void applyUltimatePreset() {
        HuroConfig c = HuroConfig.get();
        c.hitParticles = true; c.hitParticleType = 4; c.hitParticleCount = 20;
        c.hitSound = true; c.hitSoundType = 0; c.hitSoundPitch = 1.2f;
        c.hitFlash = true; c.hitKick = true; c.killEffect = true;
        c.showCombo = true; c.customCrosshair = true;
        HuroConfig.save();
    }
}
