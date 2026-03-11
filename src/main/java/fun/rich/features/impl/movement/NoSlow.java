package fun.rich.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.rich.events.player.TickEvent;
import fun.rich.utils.interactions.interact.PlayerInteractionHelper;
import fun.rich.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;

import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.utils.client.managers.event.types.EventType;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.utils.client.Instance;
import fun.rich.utils.math.time.StopWatch;
import fun.rich.utils.math.script.Script;
import fun.rich.events.item.UsingItemEvent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends Module {
    public static NoSlow getInstance() {
        return Instance.get(NoSlow.class);
    }

    private final StopWatch notifWatch = new StopWatch();
    private final Script script = new Script();
    private boolean finish;

    public final SelectSetting itemMode = new SelectSetting("Режим предмета", "Выберите режим обхода")
            .value("Grim Old", "SpookyTime", "Funsky");

    public NoSlow() {
        super("NoSlow", "NoSlow", ModuleCategory.MOVEMENT);
        setup(itemMode);
    }

    private int ticks = 0;
    private int slowEventCount = 0;

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player == null) return;
        if (mc.player.getActiveHand() == Hand.MAIN_HAND || mc.player.getActiveHand() == Hand.OFF_HAND) {
            ticks++;
        } else {
            ticks = 0;
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onUsingItem(UsingItemEvent e) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (!mc.player.isUsingItem()) {
            ticks = 0;
            slowEventCount = 0;
            return;
        }

        Hand first = mc.player.getActiveHand();
        Hand second = first == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;

        switch (e.getType()) {
            case EventType.ON -> {
                boolean spookyTest = itemMode.isSelected("FunSky");
                boolean spookyTime = itemMode.isSelected("SpookyTime");

                if (spookyTest || spookyTime) {
                    slowEventCount++;
                }

                if ((mc.player.getMainHandStack().getUseAction() == UseAction.BLOCK || mc.player.getOffHandStack().getUseAction() == UseAction.EAT) && first == Hand.MAIN_HAND) {
                    return;
                }

                mc.player.setSprinting(true);

                switch (itemMode.getSelected()) {
                    case "Grim Old" -> {
                        if (mc.player.getOffHandStack().getUseAction() == UseAction.NONE || mc.player.getMainHandStack().getUseAction() == UseAction.NONE) {
                            PlayerInteractionHelper.interactItem(first);
                            PlayerInteractionHelper.interactItem(second);
                            e.cancel();
                        }
                    }
                    case "SpookyTime" -> {
                        if (ticks > 1 && mc.player.getItemUseTime() > 1) {
                            e.cancel();
                            ticks = 0;
                        }
                    }
                    case "FunSky" -> {
                        if (slowEventCount >= 2) {
                            e.cancel();
                            slowEventCount = 0;
                        }
                    }
                }
            }
            case EventType.POST -> {
                while (!script.isFinished()) script.update();
            }
        }
    }
}