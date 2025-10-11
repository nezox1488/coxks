package fun.rich.features.impl.player;

import antidaunleak.api.annotation.Native;
import fun.rich.events.keyboard.HotBarScrollEvent;
import fun.rich.events.player.HotBarUpdateEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.events.render.ItemRendererEvent;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.interactions.inv.InventoryTask;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

import java.util.Objects;

public class ItemFixSwap extends Module {
    private boolean lockActive = false;
    private int lockSlot = -1;
    private int deferredSlot = -1;
    private int lastSentSlot = -1;
    private ItemStack renderStack = null;
    private boolean pendingWorldApply = false;
    private Object lastWorldRef = null;

    public ItemFixSwap() {
        super("ItemFixSwap", "ItemFixSwap", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onItemRenderer(ItemRendererEvent e) {
        if (lockActive && e.getHand() == Hand.MAIN_HAND && mc.player != null && Objects.equals(mc.player, e.getPlayer()) && renderStack != null) {
            e.setStack(renderStack);
        }
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (shouldLock()) e.cancel();
        else ensureSelectedSynced(false);
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (shouldLock()) e.cancel();
        else ensureSelectedSynced(false);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (lastWorldRef != mc.world) {
            lastWorldRef = mc.world;
            if (mc.world != null) {
                pendingWorldApply = true;
                lastSentSlot = -1;
            }
        }

        if (mc.world == null || mc.player == null) return;

        if (pendingWorldApply) {
            if (deferredSlot != -1) setSelectedSlot(deferredSlot, true);
            else ensureSelectedSynced(true);
            pendingWorldApply = false;
        }

        boolean usingMain = isUsingMainhand();
        int currentSelected = mc.player.getInventory().selectedSlot;

        if (usingMain) {
            if (!lockActive) {
                lockActive = true;
                lockSlot = clamp(currentSelected);
                renderStack = mc.player.getMainHandStack().copy();
                sendSelected(lockSlot, true);
            } else {
                if (currentSelected != lockSlot) {
                    deferredSlot = clamp(currentSelected);
                    mc.player.getInventory().selectedSlot = lockSlot;
                    sendSelected(lockSlot, true);
                }
            }
        } else {
            if (lockActive) {
                lockActive = false;
                if (deferredSlot != -1 && deferredSlot != lockSlot) {
                    setSelectedSlot(deferredSlot, true);
                    InventoryTask.updateSlots();
                } else {
                    ensureSelectedSynced(true);
                }
                lockSlot = -1;
                deferredSlot = -1;
                renderStack = null;
            } else {
                ensureSelectedSynced(false);
            }
        }
    }

    private boolean shouldLock() {
        return lockActive || isUsingMainhand();
    }

    private boolean isUsingMainhand() {
        return mc.player != null && mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND;
    }

    private void ensureSelectedSynced(boolean force) {
        if (mc.player == null) return;
        int s = clamp(mc.player.getInventory().selectedSlot);
        sendSelected(s, force || lastSentSlot != s);
    }

    private void setSelectedSlot(int idx, boolean force) {
        if (mc.player == null) return;
        idx = clamp(idx);
        mc.player.getInventory().selectedSlot = idx;
        sendSelected(idx, true || force);
    }

    private void sendSelected(int idx, boolean force) {
        if (mc.player == null || mc.player.networkHandler == null) return;
        if (!force && lastSentSlot == idx) return;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(idx));
        lastSentSlot = idx;
    }

    private int clamp(int idx) {
        if (idx < 0) return 0;
        if (idx > 8) return 8;
        return idx;
    }
}