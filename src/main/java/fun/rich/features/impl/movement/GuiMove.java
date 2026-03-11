package fun.rich.features.impl.movement;

import fun.rich.events.item.ClickSlotEvent;
import fun.rich.features.impl.render.TargetESP;
import fun.rich.utils.client.Instance;
import fun.rich.utils.client.packet.network.Network;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.inv.InventoryFlowManager;
import lombok.Getter;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.interactions.simulate.Simulations;

import fun.rich.utils.interactions.inv.InventoryTask;
import fun.rich.events.container.CloseScreenEvent;
import fun.rich.events.packet.PacketEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

@Getter
public class GuiMove extends Module {
    public static GuiMove getInstance() {
        return Instance.get(GuiMove.class);
    }
    private final List<Packet<?>> packets = new ArrayList<>();
    public static final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения в инвентаре")
            .value("Обычный", "ФанТайм", "СпукиТайм", "ХолиВорлд","РиллиВорлд","КопиТайм")
            .selected("СпукиТайм");

    public GuiMove() {
        super("GuiMove", "GuiMove", ModuleCategory.MOVEMENT);
        setup(mode);
    }



    @EventHandler
    public void onPacket(PacketEvent e) {
        if (!mode.isSelected("Обычный")) {
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
        if (!InventoryTask.isServerScreen() && InventoryFlowManager.shouldSkipExecution() && (mode.isSelected("Обычный") || !packets.isEmpty() || mc.player.currentScreenHandler.getCursorStack().isEmpty())) {
            InventoryFlowManager.updateMoveKeys();
        }
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        if (!mode.isSelected("Обычный")) {
            SlotActionType actionType = e.getActionType();
            if ((!packets.isEmpty() || Simulations.hasPlayerMovement()) && ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW)) || actionType.equals(SlotActionType.PICKUP_ALL))) {
                e.cancel();
            }

        }
    }
    @EventHandler
    public void onCloseScreen(CloseScreenEvent e) {
        if (!mode.isSelected("Обычный") && !packets.isEmpty()) {
            InventoryFlowManager.addTask(() -> {
                packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
                packets.clear();
                InventoryTask.updateSlots();
            });
        }
    }
}