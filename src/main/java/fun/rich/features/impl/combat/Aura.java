package fun.rich.features.impl.combat;

import fun.rich.features.impl.movement.Strafe;
import fun.rich.features.impl.movement.TargetStrafe;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.features.aura.point.MultiPoint;
import fun.rich.utils.features.aura.rotations.constructor.LinearConstructor;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.rotations.impl.*;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.TurnsConfig;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import fun.rich.utils.interactions.simulate.PlayerSimulation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.client.managers.event.types.EventType;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.*;
import fun.rich.utils.client.Instance;
import fun.rich.utils.math.task.TaskPriority;
import fun.rich.Rich;
import fun.rich.events.packet.PacketEvent;
import fun.rich.events.player.RotationUpdateEvent;
import fun.rich.display.hud.Notifications;
import fun.rich.utils.features.aura.striking.StrikeManager;
import fun.rich.utils.features.aura.striking.StrikerConstructor;
import fun.rich.utils.features.aura.target.TargetFinder;
import fun.rich.features.impl.render.Hud;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Objects;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Aura extends Module {

    private static final float RANGE_MARGIN = 0.253F;

    private static final Map<String, Integer> LEGIT_SPRINT_MAP = Map.of(
            "FunTime", 1,
            "Matrix", 1,
            "Snap", 1,
            "SpookyTime", 2,
            "HolyWorld", 2
    );

    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    @NonFinal
    LivingEntity target, lastTarget;

    @NonFinal
    long shiftTapEndTime = 0;

    public static boolean fakeRotate;

    @NonFinal
    @Getter
    public static float legitSprintNeed;

    SelectSetting aimMode = new SelectSetting("Наводка", "Выберите тип наводки")
            .value("Matrix", "Snap", "FunTime", "HolyWorld", "SpookyTime", "LonyGrief", "Trigger Bot")
            .selected("Matrix");

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует весь список целей по типу")
            .value("Players", "Mobs", "Animals", "Friends", "Armor Stand")
            .selected("Players", "Mobs", "Animals");

    SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    SliderSettings lookRange = new SliderSettings("Дистанция поиска", "Диапазон поиска до цели")
            .setValue(1.5f).range(0F, 10F);

    MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Позволяет настроить работу функции")
            .value("Only Critical", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls", "Elytra possibilities", "Combine with Blink")
            .selected("Only Critical", "Break Shield", "Elytra possibilities");

    SelectSetting correctionType = new SelectSetting("Коррекции движения", "Выбор коррекции движения игрока")
            .value("Free", "Focused", "Not visible").selected("Free");

    SelectSetting sprintReset = new SelectSetting("Сброс спринта", "Выбор сброса спринта перед ударом")
            .value("Legit", "Packet").selected("Legit");

    SliderSettings elytraFindRange = new SliderSettings("Дистанция элитры", "Дальность поиска цели во время полета на элитре")
            .setValue(16).range(6F, 32F).visible(() -> attackSetting.isSelected("Elytra possibilities"));

    private final BindSetting forward = new BindSetting("Кнопка перегона", "Кнопка для вкл или выкл перегона").visible(() -> attackSetting.isSelected("Elytra possibilities"));

    SliderSettings elytraForward = new SliderSettings("Значение перегона", "Дальность перегона цели во время полета на элитре")
            .setValue(3).range(0F, 6F).visible(() -> attackSetting.isSelected("Elytra possibilities"));

    BooleanSetting smartCrits = new BooleanSetting("Удары на земле", "Криты только при нажатии пробела")
            .setValue(true).visible(() -> attackSetting.isSelected("Only Critical"));

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(aimMode, targetType, attackRange, lookRange, attackSetting, correctionType, sprintReset, elytraFindRange, forward, elytraForward, smartCrits);
    }

    @Override
    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
        super.deactivate();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance().notificationSettings.isSelected("Break Shield")) {
                Notifications.getInstance().addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 2000);
            }
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        checkForwardToggle();

        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();
                if (target != null) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
            }
            case EventType.POST -> {
                if (target != null) {
                    Rich.getInstance().getAttackPerpetrator().performAttack(getConfig());
                }
            }
        }
    }

    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN + (mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities") ? elytraFindRange.getValue() : lookRange.getValue());
        targetSelector.searchTargets(mc.world.getEntities(), range, 360, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Turns.VecRotation rotation = new Turns.VecRotation(config.getAngle(), config.getAngle().toVector());
        TurnsConfig rotationConfig = getRotationConfig();

        boolean elytraMode = mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities");

        if (aimMode.isSelected("LonyGrief")) {
            fakeRotate = true;
        } else {
            fakeRotate = false;
        }

        if (fakeRotate && target != null) {
            FakeAngle fake = new FakeAngle();
            Turns fakeRot = fake.limitAngleChange(controller.getRotation(), rotation.getAngle(), rotation.getVec(), target);
            controller.setFakeRotation(fakeRot);
        }

        boolean shouldRotate = switch (aimMode.getSelected()) {
            case "Snap" -> attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(100);
            case "FunTime" -> attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(15);
            case "SpookyTime" -> true;
            case "LonyGrief" -> true;
            case "Matrix" -> true;
            case "HolyWorld" -> {
                PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(1);
                boolean isJumpPeakOrFalling = !simulated.onGround && simulated.velocity.getY() <= 0.04;
                yield isJumpPeakOrFalling || attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(100);
            }
            default -> false;
        };

        if (shouldRotate && !aimMode.isSelected("Trigger Bot")) {
            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }

        if (elytraMode && !aimMode.isSelected("Trigger Bot")) {
            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }
    }

    @NonFinal
    public boolean elytraStateForward = false;
    private boolean wasForwardPressed = false;

    private void toggleElytraStateForward() {
        if (!attackSetting.isSelected("Elytra possibilities")) {
            return;
        }
        elytraStateForward = !elytraStateForward;
        ChatMessage.brandmessage("Elytra forward state: " + elytraStateForward);
    }

    private void checkForwardToggle() {
        if (!attackSetting.isSelected("Elytra possibilities")) {
            return;
        }
        boolean isPressedNow = GLFW.glfwGetKey(mc.getWindow().getHandle(), forward.getKey()) == GLFW.GLFW_PRESS;

         if (isPressedNow && !wasForwardPressed) {
            toggleElytraStateForward();
        }

        wasForwardPressed = isPressedNow;
    }

    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackRange.getValue() + RANGE_MARGIN;

        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                TurnsConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                attackSetting.isSelected("Ignore The Walls")
        );

        Vec3d computedPoint = pointData.getLeft();

        if (mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities") && target.isGliding() && !aimMode.isSelected("Trigger Bot")) {
            Vec3d lookVec = target.getRotationVec(1.0F).normalize();
            if (elytraStateForward) {
                computedPoint = computedPoint.add(lookVec.multiply(elytraForward.getValue()));
            }
        }

        Turns angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));
        Box hitbox = pointData.getRight();

        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                attackSetting.getSelected(),
                aimMode,
                hitbox
        );
    }


    public TurnsConfig getRotationConfig() {
        boolean visibleCorrection = !correctionType.isSelected("Not visible");
        boolean freeCorrection = !aimMode.isSelected("Legit") && correctionType.isSelected("Free");
        if (TargetStrafe.getInstance().isState() && TargetStrafe.getInstance().mode.isSelected("Grim") && target !=null) { freeCorrection = false; }
        return new TurnsConfig(getSmoothMode(), visibleCorrection, freeCorrection);
    }

    public RotateConstructor getSmoothMode() {
        if (mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities") && !aimMode.isSelected("Trigger Bot")) {
            return new LinearConstructor();
        }
        return switch (aimMode.getSelected()) {
            case "FunTime" -> new FTAngle();
            case "HolyWorld" -> new HWAngle();
            case "LonyGrief" -> new LGAngle();
            case "SpookyTime" -> new SPAngle();
            case "Snap" -> new SnapAngle();
            case "Matrix" -> new MatrixAngle();
            default -> new LinearConstructor();
        };
    }
}
