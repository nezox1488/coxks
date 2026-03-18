package fun.rich.features.impl.combat;
/**
 * @author Sitoku
 * @since 3/3/2026
 */
import fun.rich.common.repository.friend.FriendUtils;
import fun.rich.events.player.AttackEvent;
import fun.rich.events.player.BoundingBoxControlEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.GroupSetting;
import fun.rich.features.module.setting.implement.MultiSelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import fun.rich.events.render.WorldRenderEvent;
import fun.rich.utils.display.geometry.Render3D;
import fun.rich.utils.display.color.ColorAssist;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HitBoxModule extends Module {

    static final String TARGET_PLAYERS = "Игроки";
    static final String TARGET_MOBS = "Мобы";
    static final String TARGET_ALL = "Все";

    MultiSelectSetting targetsSetting = new MultiSelectSetting("Цели", "Какие сущности увеличивать")
            .value(TARGET_ALL, TARGET_PLAYERS, TARGET_MOBS)
            .selected(TARGET_PLAYERS);

    BooleanSetting onlyWithWeaponSetting = new BooleanSetting("Только с оружием", "Работает только с оружием в руке");
    BooleanSetting onlyArmoredSetting = new BooleanSetting("Только в броне", "Только игроки в броне");
    BooleanSetting onlyTargetSetting = new BooleanSetting("Только на противнике", "Увеличивать хитбокс только у последней атакованной цели");
    SliderSettings resetTimeSetting = new SliderSettings("Время сброса цели", "Секунды до сброса цели").range(1f, 7f).setValue(3f);
    BooleanSetting invisibleHitboxSetting = new BooleanSetting("Невидимый хитбокс", "Не расширять визуально (в игре всё равно бьёт по расширенному)");

    GroupSetting sizeGroup = new GroupSetting("Настройки размера", "Параметры хитбокса").setValue(true);
    BooleanSetting fixedSizeSetting = new BooleanSetting("Фиксированный размер", "Один размер для всех").setValue(true);
    SliderSettings fixedSizeValueSetting = new SliderSettings("Фикс. размер", "Половина ширины хитбокса (0.3 = ванилла)").range(0.3f, 1.2f).setValue(0.7f);
    SliderSettings minSizeSetting = new SliderSettings("Мин. размер", "Минимальная половина ширины").range(0.2f, 1f).setValue(0.3f);
    SliderSettings maxSizeSetting = new SliderSettings("Макс. размер", "Максимальная половина ширины").range(0.5f, 5f).setValue(2f);
    SliderSettings stepSetting = new SliderSettings("Шаг", "Шаг изменения размера").range(0.05f, 0.5f).setValue(0.1f);
    SliderSettings sizeSetting = new SliderSettings("Размер", "Текущий размер (если не фикс.)").range(0.3f, 2f).setValue(0.5f);
    BooleanSetting animationSetting = new BooleanSetting("Анимация", "Плавное изменение размера").setValue(true);
    SliderSettings animationSpeedSetting = new SliderSettings("Скорость анимации", "Скорость плавного изменения").range(1f, 15f).setValue(6f);

    SliderSettings yExpandSetting = new SliderSettings("Расширение Y", "Расширение по вертикали").range(0f, 3f).setValue(0f);

    BooleanSetting showHitboxSetting = new BooleanSetting("Показывать хитбокс", "Рисовать контур увеличенного хитбокса в мире").setValue(true);

    LivingEntity lastTarget;
    long lastTargetTime;
    double savedVariableSize = 0.5;
    float animatedSize = 0.3f;
    long lastAnimTimeNanos;

    static final double NANOS_PER_SECOND = 1_000_000_000.0;

    public HitBoxModule() {
        super("HitBox", "Hit Box", ModuleCategory.COMBAT);

        fixedSizeValueSetting.visible(fixedSizeSetting::isValue);
        resetTimeSetting.visible(onlyTargetSetting::isValue);
        minSizeSetting.visible(() -> !fixedSizeSetting.isValue());
        maxSizeSetting.visible(() -> !fixedSizeSetting.isValue());
        stepSetting.visible(() -> !fixedSizeSetting.isValue());
        sizeSetting.visible(() -> !fixedSizeSetting.isValue());
        animationSpeedSetting.visible(animationSetting::isValue);

        sizeGroup.settings(minSizeSetting, maxSizeSetting, stepSetting, sizeSetting, fixedSizeSetting, fixedSizeValueSetting, animationSetting, animationSpeedSetting);

        setup(
                targetsSetting,
                onlyWithWeaponSetting,
                onlyArmoredSetting,
                onlyTargetSetting,
                resetTimeSetting,
                invisibleHitboxSetting,
                showHitboxSetting,
                sizeGroup,
                yExpandSetting
        );
    }

    @Override
    public void activate() {
        if (!fixedSizeSetting.isValue()) {
            sizeSetting.setValue((float) savedVariableSize);
        }
        float target = fixedSizeSetting.isValue() ? fixedSizeValueSetting.getValue() : sizeSetting.getValue();
        animatedSize = target;
        lastAnimTimeNanos = System.nanoTime();
    }

    @Override
    public void deactivate() {
        if (!fixedSizeSetting.isValue()) {
            savedVariableSize = sizeSetting.getValue();
        }
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!isState() || !onlyTargetSetting.isValue()) return;
        Entity entity = e.getEntity();
        if (entity instanceof LivingEntity living && matchesTargetFilter(living)) {
            lastTarget = living;
            lastTargetTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onBoundingBoxControl(BoundingBoxControlEvent event) {
        if (!isState()) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;
        if (living == mc.player) return;
        if (FriendUtils.isFriend(living)) return;

        if (onlyWithWeaponSetting.isValue() && !hasWeapon()) return;
        if (onlyTargetSetting.isValue()) {
            if (lastTarget == null || lastTarget != living) return;
            long elapsed = System.currentTimeMillis() - lastTargetTime;
            if (elapsed >= (long) (resetTimeSetting.getValue() * 1000)) {
                lastTarget = null;
                return;
            }
        }
        if (living instanceof PlayerEntity player && onlyArmoredSetting.isValue() && !hasArmor(player)) return;
        if (!matchesTargetFilter(living)) return;

        Box box = event.getBox();
        float targetSize = fixedSizeSetting.isValue() ? fixedSizeValueSetting.getValue() : sizeSetting.getValue();

        if (animationSetting.isValue()) {
            long now = System.nanoTime();
            if (lastAnimTimeNanos > 0 && now - lastAnimTimeNanos <= NANOS_PER_SECOND) {
                float dt = (float) ((now - lastAnimTimeNanos) / NANOS_PER_SECOND);
                float diff = targetSize - animatedSize;
                float speed = animationSpeedSetting.getValue();
                if (Math.abs(diff) > 0.001f) {
                    float step = 1f - (float) Math.exp(-dt * speed);
                    animatedSize += diff * step;
                } else {
                    animatedSize = targetSize;
                }
            } else {
                animatedSize = targetSize;
            }
            lastAnimTimeNanos = now;
        } else {
            animatedSize = targetSize;
        }

        float halfWidth = Math.max(0.3f, animatedSize);
        double x = entity.getX();
        double z = entity.getZ();
        float yExpand = yExpandSetting.getValue();

        Box newBox = new Box(
                x - halfWidth,
                box.minY - yExpand * 0.5,
                z - halfWidth,
                x + halfWidth,
                box.maxY + yExpand * 0.5,
                z + halfWidth
        );
        event.setBox(newBox);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!isState() || !showHitboxSetting.isValue() || invisibleHitboxSetting.isValue()) return;
        if (mc.world == null || mc.player == null) return;

        float halfWidth = getCurrentHalfWidth();
        float yExpand = yExpandSetting.getValue();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!shouldExpandEntity(living, true)) continue;

            double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
            Box vanillaBox = entity.getType().getDimensions().getBoxAt(x, y, z);
            Box drawBox = buildExpandedBox(entity, x, vanillaBox.minY, vanillaBox.maxY, z, halfWidth, yExpand);

            int color = ColorAssist.getClientColor();
            int outline = 0xFF000000 | (color & 0x00FFFFFF);
            Render3D.drawBox(drawBox, color & 0x33FFFFFF, 1.5f, true, true, false);
            Render3D.drawBox(drawBox, outline, 2f, true, false, false);
        }
    }

    /**
     * @author Sitoku
     * @since 3/3/2026
     */
    float getCurrentHalfWidth() {
        if (!animationSetting.isValue()) {
            return fixedSizeSetting.isValue() ? fixedSizeValueSetting.getValue() : sizeSetting.getValue();
        }
        return animatedSize;
    }

    boolean shouldExpandEntity(LivingEntity living, boolean requireWeapon) {
        if (living == mc.player || FriendUtils.isFriend(living)) return false;
        if (requireWeapon && onlyWithWeaponSetting.isValue() && !hasWeapon()) return false;
        if (onlyTargetSetting.isValue()) {
            if (lastTarget == null || lastTarget != living) return false;
            if (System.currentTimeMillis() - lastTargetTime >= (long) (resetTimeSetting.getValue() * 1000)) return false;
        }
        if (living instanceof PlayerEntity player && onlyArmoredSetting.isValue() && !hasArmor(player)) return false;
        return matchesTargetFilter(living);
    }

    Box buildExpandedBox(Entity entity, double x, double yMin, double yMax, double z, float halfWidth, float yExpand) {
        return new Box(
                x - halfWidth,
                yMin - yExpand * 0.5,
                z - halfWidth,
                x + halfWidth,
                yMax + yExpand * 0.5,
                z + halfWidth
        );
    }

    boolean matchesTargetFilter(Entity entity) {
        if (targetsSetting.isSelected(TARGET_ALL)) return entity instanceof LivingEntity;
        if (targetsSetting.isSelected(TARGET_PLAYERS)) return entity instanceof PlayerEntity;
        if (targetsSetting.isSelected(TARGET_MOBS)) return entity instanceof MobEntity;
        return entity instanceof PlayerEntity;
    }

    boolean hasWeapon() {
        if (mc.player == null) return false;
        ItemStack main = mc.player.getMainHandStack();
        ItemStack off = mc.player.getOffHandStack();
        return isWeapon(main) || isWeapon(off);
    }

    static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
    }

    boolean hasArmor(PlayerEntity player) {
        var armor = player.getInventory().armor;
        for (int i = 0; i < armor.size(); i++) {
            if (!armor.get(i).isEmpty()) return true;
        }
        return false;
    }
}
