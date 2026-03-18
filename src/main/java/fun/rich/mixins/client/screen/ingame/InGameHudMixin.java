package fun.rich.mixins.client.screen.ingame;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.geometry.Render2D;
import fun.rich.Rich;
import fun.rich.utils.client.managers.event.EventManager;
import fun.rich.events.render.DrawEvent;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.features.impl.render.CrossHair;
import fun.rich.features.impl.render.Hud;

import java.util.ConcurrentModificationException;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements QuickImports {
    @Unique private final Hud hud = Hud.getInstance();

    @Final @Shadow private MinecraftClient client;

    @Shadow protected abstract void renderStatusBars(DrawContext context);

    @Shadow protected abstract void renderMountHealth(DrawContext context);

    @Inject(method = "renderStatusBars", at = @At("RETURN"))
    private void renderSaturationBars(DrawContext context, CallbackInfo ci) {
        if (client.player == null || !client.interactionManager.hasStatusBars()) return;
        float saturation = client.player.getHungerManager().getSaturationLevel();
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        int x = w / 2 + 91 - 80;
        int y = h - 39 + 10;
        int barW = 80;
        int barH = 2;
        context.fill(x, y, x + barW, y + barH, 0xFF404040);
        int fillW = (int) (barW * Math.min(1f, saturation / 20f));
        if (fillW > 0) {
            context.fill(x, y, x + fillW, y + barH, 0xFFE0C000);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/LayeredDrawer;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            shift = At.Shift.AFTER))
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        blur.prepareFrame();
        DrawEvent event = new DrawEvent(context, drawEngine, tickCounter.getTickDelta(false));
        EventManager.callEvent(event);
        Render2D.onRender(context);

        boolean debugHudVisible = client.getDebugHud().shouldShowDebugHud();
        boolean tabVisible = client.options.playerListKey.isPressed();

        if (!client.options.hudHidden && !debugHudVisible) {
            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 900.0F);

            var draggables = Rich.getInstance().getDraggableRepository().draggable();
            for (var draggable : draggables) {
                if (draggable.canDraw(hud, draggable)) draggable.startAnimation();
                else draggable.stopAnimation();

                float scale = draggable.getScaleAnimation().getOutput().floatValue();
                if (!draggable.isCloseAnimationFinished()) {
                    draggable.validPosition();
                    try {
                        float visualScale = 0.88f + 0.12f * scale;
                        float cx = draggable.getX() + draggable.getWidth() / 2f;
                        float cy = draggable.getY() + draggable.getHeight() / 2f;
                        var matrix = context.getMatrices();
                        matrix.push();
                        matrix.translate(cx, cy, 0);
                        matrix.scale(visualScale, visualScale, 1f);
                        matrix.translate(-cx, -cy, 0);
                        Calculate.setAlpha(scale, () -> {
                            if (draggable.isDragging()) draggable.renderDragOutline(context);
                            draggable.drawDraggable(context);
                        });
                        matrix.pop();
                    } catch (ConcurrentModificationException ignored) {}
                }
            }

            context.getMatrices().pop();
        }
    }

    @Inject(method = "renderCrosshair", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;"), cancellable = true)
    public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrossHair crossHair = CrossHair.getInstance();
        if (crossHair.isState()) {
            crossHair.onRenderCrossHair();
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
    public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Potions")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Score Board")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("HotBar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At(value = "HEAD"), cancellable = true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("HotBar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMainHud", at = @At(value = "HEAD"), cancellable = true)
    private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("HotBar")) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, InGameHud.HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 1, 1);
            if (client.interactionManager.hasStatusBars()) renderStatusBars(context);
            this.renderMountHealth(context);
            ci.cancel();
        }
    }
}