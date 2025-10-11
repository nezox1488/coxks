package fun.rich.features.impl.movement;

import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.inv.InventoryFlowManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.interactions.simulate.Simulations;

import fun.rich.utils.interactions.inv.InventoryTask;
import fun.rich.events.container.CloseScreenEvent;
import fun.rich.events.item.ClickSlotEvent;
import fun.rich.events.packet.PacketEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryMove extends Module {
    private final List<Packet<?>> packets = new ArrayList<>();
    private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения в инвентаре")
            .value("Normal", "Bypass")
            .selected("Bypass");

    public InventoryMove() {
        super("InventoryMove", "Inventory Move", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mode.isSelected("Bypass")) {
            switch (e.getPacket()) {
                case ClickSlotC2SPacket slot when (!packets.isEmpty() || Simulations.hasPlayerMovement()) && InventoryFlowManager.shouldSkipExecution() -> {
                    packets.add(slot);
                    e.cancel();
                }
                case CloseScreenS2CPacket screen when screen.getSyncId() == 0 -> e.cancel();
                default -> {
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!InventoryTask.isServerScreen() && InventoryFlowManager.shouldSkipExecution() && (mode.isSelected("Normal") || !packets.isEmpty() || mc.player.currentScreenHandler.getCursorStack().isEmpty())) {
            InventoryFlowManager.updateMoveKeys();
        }
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        if (mode.isSelected("Bypass")) {
            SlotActionType actionType = e.getActionType();
            if ((!packets.isEmpty() || Simulations.hasPlayerMovement()) && ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW)) || actionType.equals(SlotActionType.PICKUP_ALL))) {
                e.cancel();
            }
        }
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent e) {
        if (mode.isSelected("Bypass") && !packets.isEmpty()) {
            InventoryFlowManager.addTask(() -> {
                packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
                packets.clear();
                InventoryTask.updateSlots();
            });
        }
    }
}