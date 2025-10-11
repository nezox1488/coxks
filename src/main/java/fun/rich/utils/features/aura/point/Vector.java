package fun.rich.utils.features.aura.point;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static fun.rich.utils.display.interfaces.QuickImports.mc;

public class Vector {

    public static Vec3d hitbox(Entity entity, float X, float Y, float Z, float WIDTH) {
        double wHalf = entity.getWidth() / WIDTH;
        double yExpand = MathHelper.clamp(entity.getEyeY() - entity.getY(), 0, entity.getHeight());
        double xExpand = MathHelper.clamp(mc.player.getX() - entity.getX(), -wHalf, wHalf);
        double zExpand = MathHelper.clamp(mc.player.getZ() - entity.getZ(), -wHalf, wHalf);

        return new Vec3d(
                entity.getX() + xExpand / X,
                entity.getY() + yExpand / Y,
                entity.getZ() + zExpand / Z
        );
    }


}
