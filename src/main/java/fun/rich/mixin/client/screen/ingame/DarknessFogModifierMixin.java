package fun.rich.mixin.client.screen.ingame;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.rich.features.impl.render.NoRender;

@Mixin(BackgroundRenderer.DarknessFogModifier.class)
public class DarknessFogModifierMixin {

    @Inject(method = "applyColorModifier", at = @At("HEAD"), cancellable = true)
    private void onApplyColorModifier(LivingEntity entity, StatusEffectInstance effect, float defaultModifier, float tickDelta, CallbackInfoReturnable<Float> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "applyStartEndModifier", at = @At("HEAD"), cancellable = true)
    private void onApplyStartEndModifier(BackgroundRenderer.FogData fogData, LivingEntity entity, StatusEffectInstance effect, float viewDistance, float tickDelta, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.isState() && noRender.modeSetting.isSelected("Darkness")) {
            ci.cancel();
        }
    }
}