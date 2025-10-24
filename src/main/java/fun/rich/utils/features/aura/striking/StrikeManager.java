package fun.rich.utils.features.aura.striking;

import fun.rich.features.impl.movement.Blink;
import fun.rich.utils.client.Instance;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.display.interfaces.QuickImports;
import fun.rich.utils.client.managers.event.types.EventType;
import fun.rich.features.impl.combat.Aura;
import fun.rich.utils.features.aura.warp.Turns;
import fun.rich.utils.features.aura.utils.MathAngle;
import fun.rich.utils.features.aura.utils.RaycastAngle;
import fun.rich.utils.features.aura.warp.TurnsConnection;
import fun.rich.utils.features.aura.utils.Pressing;
import fun.rich.features.impl.movement.AutoSprint;
import fun.rich.events.item.UsingItemEvent;
import fun.rich.events.packet.PacketEvent;
import fun.rich.main.listener.impl.EventListener;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.inv.InventoryFlowManager;
import fun.rich.utils.interactions.inv.InventoryTask;
import fun.rich.utils.interactions.simulate.PlayerSimulation;
import fun.rich.utils.math.calc.Calculate;
import fun.rich.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StrikeManager implements QuickImports {
    private final StopWatch attackTimer = new StopWatch(), shieldWatch = new StopWatch(), sprintCooldown = new StopWatch();;
    private final Pressing clickScheduler = new Pressing();
    private int count = 0;
    private boolean prevSprinting;

    void tick() {}

    void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            clickScheduler.recalculate();
        }
    }

    void onUsingItem(UsingItemEvent e) {
        if (e.getType() == EventType.START && !shieldWatch.finished(50)) {
            e.cancel();
        }
    }
    private ClientCommandC2SPacket.Mode lastSprintCommand = null;
    private boolean pendingStartSprint = false;
    private boolean pendingStopSprint = false;
    private boolean didStopSprint = false;
    private static final long SPRINT_COOLDOWN_MS = 200;
    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (canAttack(config, 1)) preAttackEntity(config);
        if (Aura.getInstance().getTarget() !=null && Aura.getInstance().getTarget().distanceTo(mc.player) <= Aura.getInstance().getAttackRange().getValue() && Aura.getInstance().getTarget().isGliding() && mc.player.isGliding() && Aura.getInstance().getAttackSetting().isSelected("Elytra possibilities")) {
            if (!canAttack(config, 1)) return;
        } else {
            if (!RaycastAngle.rayTrace(config) || !canAttack(config, 1)) return;
        }

        String sprintMode = Aura.getInstance().getSprintReset().getSelected();

        if (sprintMode.equals("Legit") && !isSprinting()) {
            attackEntity(config);
        }

        if (sprintMode.equals("Packet")) {
            mc.player.setSprinting(false);
            mc.player.sendSprintingPacket();
            attackEntity(config);
        }

    }

    void preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }
        String sprintMode = Aura.getInstance().getSprintReset().getSelected();
        if (sprintMode.equals("Legit")) {
            if (mc.player.isSprinting() && mc.player.distanceTo(Aura.getInstance().getTarget()) <= Aura.getInstance().getAttackRange().getValue()) {
                AutoSprint.tickStop = 2;
                mc.options.sprintKey.setPressed(false);
                mc.player.setSprinting(false);
                return;
            }
            return;
        }


    }

    void postAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
    }

    void attackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (Aura.getInstance().getAttackSetting().isSelected("Fake Lag")) {
            Aura.getInstance().tickStop = 1;
        }
        attack(config);
        breakShield(config);
        attackTimer.reset();
        count++;
    }

    private void breakShield(StrikerConstructor.AttackPerpetratorConfigurable config) {
        LivingEntity target = config.getTarget();
        Turns angleToPlayer = MathAngle.fromVec3d(mc.player.getBoundingBox().getCenter().subtract(target.getEyePos()));
        boolean targetOnShield = target.isUsingItem() && target.getActiveItem().getItem().equals(Items.SHIELD);
        boolean angle = Math.abs(TurnsConnection.computeAngleDifference(target.getYaw(), angleToPlayer.getYaw())) < 90;
        Slot axe = InventoryTask.getSlot(s -> s.getStack().getItem() instanceof AxeItem);

        if (config.isShouldBreakShield() && targetOnShield && axe != null && angle && InventoryFlowManager.script.isFinished()) {
            InventoryTask.swapHand(axe, Hand.MAIN_HAND, false);
            InventoryTask.closeScreen(true);
            attack(config);
            InventoryTask.swapHand(axe, Hand.MAIN_HAND, false, true);
            InventoryTask.closeScreen(true);
        }
    }

    private void attack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        mc.interactionManager.attackEntity(mc.player, config.getTarget());
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isSprinting() {
        return EventListener.serverSprint && !mc.player.isGliding() && !mc.player.isTouchingWater();
    }

    public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        for (int i = 0;i <= (int) .1;i++) {
            if (canCrit(config, (int) .1)) {
                return true;
            }
        }
        return false;
    }

    public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (mc.player.isUsingItem() && !mc.player.getActiveItem().getItem().equals(Items.SHIELD) && config.isEatAndAttack()) {
            return false;
        }

        if (!clickScheduler.isCooldownComplete(false, 1)) {
            return false;
        }

        PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
        if (config.isOnlyCritical() && !hasMovementRestrictions(simulated)) {
            return isPlayerInCriticalState(simulated, ticks);
        }

        return true;
    }

    private boolean hasMovementRestrictions(PlayerSimulation simulated) {
        return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
                || simulated.hasStatusEffect(StatusEffects.LEVITATION)
                || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-1e-3), Blocks.COBWEB)
                || simulated.isSubmergedInWater()
                || simulated.isInLava()
                || simulated.isClimbing()
                || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
                || simulated.player.getAbilities().flying;
    }

    private boolean isPlayerInCriticalState(PlayerSimulation simulated, int ticks) {
        boolean fall = simulated.fallDistance > 0 && (simulated.fallDistance < 0.08 || !PlayerSimulation.simulateLocalPlayer(ticks + 1).onGround);;
        if (Aura.getInstance().getSmartCrits().isValue() && !mc.options.jumpKey.isPressed() && mc.player.distanceTo(Aura.getInstance().getTarget()) <= Aura.getInstance().getAttackRange().getValue()) {
            return simulated.onGround || (!simulated.onGround && fall);
        }

        return !simulated.onGround && (fall);
    }
}
