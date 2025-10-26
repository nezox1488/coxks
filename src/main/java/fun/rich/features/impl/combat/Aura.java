package fun.rich.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.rich.events.player.MotionEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.events.render.DrawEvent;
import fun.rich.events.render.WorldRenderEvent;
import fun.rich.features.impl.movement.Strafe;
import fun.rich.features.impl.movement.TargetStrafe;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.display.color.ColorAssist;
import fun.rich.utils.display.font.Fonts;
import fun.rich.utils.display.geometry.Render3D;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.display.shape.ShapeProperties;
import fun.rich.utils.display.shape.implement.Arc;
import fun.rich.utils.features.aura.point.MultiPoint;
import fun.rich.utils.features.aura.rotations.constructor.LinearConstructor;
import fun.rich.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.rich.utils.features.aura.rotations.impl.*;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.warp.TurnsConfig;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.simulate.PlayerSimulation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
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
import fun.rich.utils.math.calc.Calculate;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Aura extends Module {

    private static final float RANGE_MARGIN = 0.253F;

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
            .value("FunTime", "Fov Legit", "ReallyWorld", "HolyWorld", "SpookyTime", "CakeWorld")
            .selected("FunTime");

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует весь список целей по типу")
            .value("Players", "Mobs", "Animals", "Friends", "Armor Stand")
            .selected("Players", "Mobs", "Animals");

    SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    SliderSettings lookRange = new SliderSettings("Дополнительная дистанция поиска", "Диапазон поиска до цели")
            .setValue(1.5f).range(0F, 2F);

    MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Позволяет настроить работу функции")
            .value("Only Critical", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls", "Elytra possibilities", "Fake Lag", "Hit Chance")
            .selected("Only Critical", "Break Shield", "Elytra possibilities");

    SliderSettings hitChance = new SliderSettings("Шанс удара в %", "Шанс удара по цели")
            .setValue(100).range(1F, 100F).visible(() -> attackSetting.isSelected("Hit Chance"));

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
        setup(aimMode, correctionType, sprintReset, targetType, attackRange, lookRange, hitChance, attackSetting, smartCrits, elytraFindRange, forward, elytraForward);
        Rich.getInstance().getEventManager().register(new FovCircleRenderer());
    }

    @Override
    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
        packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
        packets.clear();
        super.deactivate();
    }

    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private Box box;
    public static int tickStop = -1;

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance().notificationSettings.isSelected("Break Shield")) {
                Notifications.getInstance().addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 5000);
            }
        }

        if (attackSetting.isSelected("Fake Lag") && target !=null) {
            if (PlayerInteractionHelper.nullCheck()) return;
            switch (e.getPacket()) {
                case PlayerRespawnS2CPacket respawn -> setState(false);
                case GameJoinS2CPacket join -> setState(false);
                case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) ->
                        setState(false);
                default -> {
                    if (e.isSend() && tickStop < 0) {
                        packets.add(e.getPacket());
                        e.cancel();
                    }
                }
            }
        }
    }

    public class FovCircleRenderer implements QuickImports {
        private float currentScale = 1.0f;
        private float targetScale = 1.0f;

        @EventHandler
        public void drawEvent(DrawEvent e){
            if (mc.player == null || !aimMode.isSelected("Fov Legit") || !Aura.getInstance().isState()) return;

            if (mc.options.getPerspective().isFirstPerson()) {
                MatrixStack matrix = e.getDrawContext().getMatrices();
                float middleW = mc.getWindow().getScaledWidth() / 2f;
                float middleH = mc.getWindow().getScaledHeight() / 2f;

                double fov = mc.options.getFov().getValue();
                fov = MathHelper.clamp(fov, 30, 110);

                float baseRadius = (float) MathHelper.lerp((fov - 30.0) / 80.0, 65.0, 90.0);
                float fovScale = (float) (450.0 / fov);
                float dynamicRadius = baseRadius * fovScale;

                targetScale = mc.player.isSprinting() ? 0.9f : 1.0f;
                currentScale = Calculate.interpolateSmooth(2.5, currentScale, targetScale);

                float finalRadius = dynamicRadius * currentScale;

                float baseThickness = (float) MathHelper.lerp((fov - 30.0) / 80.0, 0.005, 0.017);

                arc.render(ShapeProperties.create(matrix, middleW - finalRadius / 2f, middleH - finalRadius / 2f, finalRadius, finalRadius)
                        .round(0.3F)
                        .thickness(baseThickness)
                        .end(360)
                        .color(ColorAssist.getColor(255, 255, 255, 255))
                        .build());
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (box != null && attackSetting.isSelected("Fake Lag") && target !=null) {
            Render3D.drawBox(box, ColorAssist.getClientColor(), 1);
        }
    }

    @EventHandler
    public void tick(TickEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        if (target == null) return;

        tickStop--;
        if (tickStop >= 0 && !packets.isEmpty() && attackSetting.isSelected("Fake Lag")) {
            box = mc.player.getBoundingBox();
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
        }
        if (mc.player.distanceTo(target) > attackRange.getValue() && attackSetting.isSelected("Fake Lag")) {
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        checkForwardToggle();

        try {
            if (aimMode.isSelected("FunTime") && Rich.getInstance().getFtCheckClient() != null) {
                Rich.getInstance().getFtCheckClient().checkAndWarnFunTime();
            }
        } catch (Exception ex) {
        }

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

    public static boolean shouldRotate;

    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN + (mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities") ? elytraFindRange.getValue() : lookRange.getValue());
        targetSelector.searchTargets(mc.world.getEntities(), range, aimMode.isSelected("Fov Legit") ? 35 : 360, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        StrikeManager attackHandler = Rich.getInstance().getAttackPerpetrator().getAttackHandler();
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Turns.VecRotation rotation = new Turns.VecRotation(config.getAngle(), config.getAngle().toVector());
        TurnsConfig rotationConfig = getRotationConfig();

        boolean elytraMode = mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities");

        if (fakeRotate && target != null) {
            FakeAngle fake = new FakeAngle();
            Turns fakeRot = fake.limitAngleChange(controller.getRotation(), rotation.getAngle(), rotation.getVec(), target);
            controller.setFakeRotation(fakeRot);
        }
        fakeRotate = false;
        shouldRotate = switch (aimMode.getSelected()) {
            case "Snap" -> attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(100);
            case "Fov Legit" -> attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(15);
            case "d" -> {
                PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(1);
                boolean isJumpPeakOrFalling = !simulated.onGround && simulated.velocity.getY() <= 0.2 && attackHandler.getAttackTimer().finished(300);
                yield isJumpPeakOrFalling || attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(25);
            }
            case "FunTime" -> attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(15);
            case "SpookyTime" -> true;
            case "CakeWorld" -> true;
            case "ds" -> true;
            case "ReallyWorld" -> true;
            case "HvH" -> true;
            case "Matrix" -> true;
            case "HolyWorld" -> {
                PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(1);
                boolean isJumpPeakOrFalling = !simulated.onGround && simulated.velocity.getY() <= 0.06;
                yield isJumpPeakOrFalling && mc.player.distanceTo(Aura.getInstance().getTarget()) <= Aura.getInstance().getAttackRange().getValue() || attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(100) ;
            }
            default -> false;
        };

        if (shouldRotate && !aimMode.isSelected("TriggerBot")) {
            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }

        if (elytraMode && !aimMode.isSelected("TriggerBot")) {
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
        if (!attackSetting.isSelected("Elytra possibilities") || mc.currentScreen != null) {
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

    @EventHandler
    public void onmotion(MotionEvent event) {

    }

    public RotateConstructor getSmoothMode() {
        if (mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities") && !aimMode.isSelected("Trigger Bot")) {
            return new LinearConstructor();
        }
        return switch (aimMode.getSelected()) {
            case "FunTime" -> new FTAngle();
            case "HolyWorld" -> new HWAngle();
            case "HvH" -> new HAngle();
            case "CakeWorld" -> new LGAngle();
            case "SpookyTime" -> new SPAngle();
            case "ReallyWorld" -> new RWAngle();
            case "Snap" -> new SnapAngle();
            case "Fov Legit" -> new SnapAngle();
            case "Matrix" -> new MatrixAngle();
            default -> new LinearConstructor();
        };
    }
}