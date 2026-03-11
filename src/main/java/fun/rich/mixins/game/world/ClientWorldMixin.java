package fun.rich.mixins.game.world;

import fun.rich.features.impl.render.Optimization;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.rich.utils.client.managers.event.EventManager;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.events.player.EntitySpawnEvent;
import fun.rich.events.render.WorldLoadEvent;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements QuickImports {

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(CallbackInfo info) {
        EventManager.callEvent(new WorldLoadEvent());
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    public void addEntityHook(Entity entity, CallbackInfo ci) {
        if (PlayerInteractionHelper.nullCheck()) return;
        EntitySpawnEvent event = new EntitySpawnEvent(entity);
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void optimizationParticles(ParticleEffect parameters, boolean force, boolean canSpawnOnMinimal,
                                        double x, double y, double z, double velocityX, double velocityY, double velocityZ,
                                        CallbackInfo ci) {
        Optimization opt = Optimization.getInstance();
        if (opt == null || !opt.isState()) return;

        var type = parameters.getType();

        if (opt.shouldOptimizeSandDust()) {
            if (type == ParticleTypes.FALLING_DUST || type == ParticleTypes.DUST_PILLAR || type == ParticleTypes.DUST_PLUME
                    || type == ParticleTypes.BLOCK_CRUMBLE) {
                ci.cancel();
                return;
            }
        }
        if (opt.shouldOptimizeBubbles()) {
            if (type == ParticleTypes.BUBBLE || type == ParticleTypes.BUBBLE_POP || type == ParticleTypes.BUBBLE_COLUMN_UP
                    || type == ParticleTypes.CURRENT_DOWN || type == ParticleTypes.DRIPPING_WATER
                    || type == ParticleTypes.FALLING_WATER || type == ParticleTypes.DRIPPING_DRIPSTONE_WATER
                    || type == ParticleTypes.FALLING_DRIPSTONE_WATER || type == ParticleTypes.UNDERWATER) {
                ci.cancel();
                return;
            }
        }
        if (opt.shouldOptimizeSmokeFire()) {
            if (type == ParticleTypes.SMOKE || type == ParticleTypes.LARGE_SMOKE || type == ParticleTypes.WHITE_SMOKE
                    || type == ParticleTypes.CAMPFIRE_COSY_SMOKE || type == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
                    || type == ParticleTypes.FLAME || type == ParticleTypes.SMALL_FLAME || type == ParticleTypes.SOUL_FIRE_FLAME
                    || type == ParticleTypes.LAVA || type == ParticleTypes.DRIPPING_LAVA || type == ParticleTypes.FALLING_LAVA
                    || type == ParticleTypes.LANDING_LAVA || type == ParticleTypes.DRIPPING_DRIPSTONE_LAVA
                    || type == ParticleTypes.FALLING_DRIPSTONE_LAVA) {
                ci.cancel();
            }
        }
    }
}