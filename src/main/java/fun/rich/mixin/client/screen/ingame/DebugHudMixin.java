package fun.rich.mixin.client.screen.ingame;

import fun.rich.features.impl.misc.SelfDestruct;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    @ModifyVariable(
            method = "getLeftText",
            at = @At("RETURN"),
            ordinal = 0
    )
    private List<String> modifyLeftText(List<String> list) {
        if (SelfDestruct.unhooked) return list;

        list.removeIf(s ->
                s.contains("fps") ||
                        s.startsWith("Chunks") ||
                        s.startsWith("SC:") ||
                        s.startsWith("Post:") ||
                        s.contains("[ViaFabricPlus]") ||
                        s.contains("Local Difficulty") ||
                        s.contains("For") ||
                        s.contains("Sounds:") ||
                        s.contains("Renderer") ||
                        s.contains("Debug charts") ||
                        s.contains("Debug charts:") ||
                        s.contains("For help:"));
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).startsWith("SC:")) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            list.subList(index, list.size()).clear();
        }
        return list;
    }

    @ModifyVariable(
            method = "getRightText",
            at = @At("RETURN"),
            ordinal = 0
    )
    private List<String> modifyRightText(List<String> list) {
        if (SelfDestruct.unhooked) return list;

        list.removeIf(s ->
                s.contains("Targeted Block") ||
                        s.contains("Targeted Fluid") ||
                        s.contains("Targeted Entity") ||
                        s.startsWith("minecraft:") ||
                        s.startsWith("#minecraft:"));
        return list;
    }

    @Redirect(
            method = "drawText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            )
    )
    private void redirectFill(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
        if (SelfDestruct.unhooked) {
            instance.fill(x1, y1, x2, y2, color);
            return;
        }

        int newColor = (200 << 24);
        instance.fill(x1, y1, x2, y2, newColor);
    }

    @Redirect(
            method = "getLeftText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getYaw()F"
            )
    )
    private float redirectYaw(Entity entity) {
        if (SelfDestruct.unhooked) return entity.getYaw();
        return TurnsConnection.INSTANCE.getRotation().getYaw();
    }

    @Redirect(
            method = "getLeftText",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getPitch()F"
            )
    )
    private float redirectPitch(Entity entity) {
        if (SelfDestruct.unhooked) return entity.getPitch();
        return TurnsConnection.INSTANCE.getRotation().getPitch();
    }
}
