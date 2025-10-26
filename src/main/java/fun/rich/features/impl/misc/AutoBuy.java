package fun.rich.features.impl.misc;

import antidaunleak.api.annotation.Native;
import fun.rich.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.rich.display.screens.clickgui.components.implement.autobuy.util.AuctionUtils;
import fun.rich.display.screens.clickgui.components.implement.autobuy.autobuyui.PurchaseHistoryWindow;
import fun.rich.events.chat.ChatEvent;
import fun.rich.events.player.TickEvent;
import fun.rich.utils.client.managers.event.EventHandler;
import fun.rich.features.module.Module;
import fun.rich.features.module.ModuleCategory;
import fun.rich.features.module.setting.implement.BooleanSetting;
import fun.rich.features.module.setting.implement.SelectSetting;
import fun.rich.features.module.setting.implement.SliderSettings;
import fun.rich.utils.math.time.TimerUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import fun.rich.display.screens.clickgui.components.implement.autobuy.manager.AutoBuyManager;
import fun.rich.utils.client.chat.ChatMessage;
import fun.rich.utils.features.autobuy.ServerManager;
import fun.rich.utils.features.autobuy.ConnectionManager;
import fun.rich.utils.features.autobuy.BuyQueueManager;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import java.time.Instant;
import java.util.BitSet;
import net.minecraft.network.message.LastSeenMessageList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBuy extends Module {
    SelectSetting leaveType = new SelectSetting("Тип обхода", "Проверяющий").value("Проверяющий", "Покупающий");
    SliderSettings timer2 = new SliderSettings("Таймер обновления аукциона", "").setValue(500).range(250, 1000);
    BooleanSetting bypassDelay = new BooleanSetting("Обход задержки 1.16.5 анках", "").visible(() -> leaveType.isSelected("Покупающий"));
    BooleanSetting bypassDelay1214 = new BooleanSetting("Обход задержки 1.21.4 анках", "").visible(() -> leaveType.isSelected("Покупающий"));

    TimerUtil openTimer = TimerUtil.create();
    TimerUtil updateTimer = TimerUtil.create();
    TimerUtil buyTimer = TimerUtil.create();
    TimerUtil switchTimer = TimerUtil.create();
    TimerUtil enterDelayTimer = TimerUtil.create();
    TimerUtil ahSpamTimer = TimerUtil.create();

    ServerManager serverManager = new ServerManager();
    ConnectionManager connectionManager = new ConnectionManager();
    BuyQueueManager queueManager = new BuyQueueManager();
    AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    boolean open = false;
    boolean serverInAuction = false;
    int failedCount = 0;
    boolean justEntered = false;
    boolean spammingAh = false;
    boolean waitingForServerLoad = false;
    long lastScanTime = 0;
    boolean currentBypassMode1214 = false;

    private static final Pattern PURCHASE_PATTERN = Pattern.compile("Вы успешно купили (.+?) за \\$([\\d,]+)!");

    public AutoBuy() {
        super("Auto Buy", "Auto Buy", ModuleCategory.MISC);
        setup(leaveType, timer2, bypassDelay, bypassDelay1214);
    }

    @Override
    public void activate() {
        super.activate();
        openTimer.resetCounter();
        updateTimer.resetCounter();
        buyTimer.resetCounter();
        switchTimer.resetCounter();
        enterDelayTimer.resetCounter();
        ahSpamTimer.resetCounter();
        open = false;
        serverInAuction = false;
        queueManager.clearAll();
        failedCount = 0;
        serverManager.reset();
        justEntered = false;
        spammingAh = false;
        waitingForServerLoad = false;
        lastScanTime = 0;
        currentBypassMode1214 = bypassDelay1214.isValue();

        if (bypassDelay.isValue() || bypassDelay1214.isValue()) {
            mc.options.pauseOnLostFocus = false;
        }

        connectionManager.setRunning(true);
        connectionManager.setMessageHandler(this::handleMessage);
        connectionManager.setConnectionHandler(this::handleConnection);

        if (leaveType.isSelected("Покупающий")) {
            connectionManager.startServer();
        } else if (leaveType.isSelected("Проверяющий")) {
            connectionManager.startClient();
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        connectionManager.stopAll();
        connectionManager.shutdown();
    }

    private void handleMessage(String line) {
        if (leaveType.isSelected("Покупающий")) {
            if (line.startsWith("buy:")) {
                try {
                    String[] parts = line.substring(4).split("\\|");
                    if (parts.length == 2) {
                        String itemName = parts[0];
                        int price = Integer.parseInt(parts[1]);
                        queueManager.addToPriorityQueue(itemName, price);
                    }
                } catch (NumberFormatException ignored) {}
            } else if (line.startsWith("switch_server:")) {
                String cmd = line.substring(14);
                mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(cmd, Instant.now(), 0L, null, new LastSeenMessageList.Acknowledgment(0, new BitSet())));
                waitingForServerLoad = true;
            }
        } else if (leaveType.isSelected("Проверяющий")) {
            if (line.equals("update_now")) {
                if (mc.currentScreen instanceof GenericContainerScreen screen && serverInAuction) {
                    int syncId = screen.getScreenHandler().syncId;
                    mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                    updateTimer.resetCounter();
                }
            } else if (line.startsWith("switch_server:")) {
                String cmd = line.substring(14);
                mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(cmd, Instant.now(), 0L, null, new LastSeenMessageList.Acknowledgment(0, new BitSet())));
                waitingForServerLoad = true;
            } else if (line.startsWith("bypass_mode:")) {
                String mode = line.substring(12);
                currentBypassMode1214 = mode.equals("1214");
            }
        }
    }

    private void handleConnection(Socket conn) {
        if (bypassDelay.isValue() || bypassDelay1214.isValue()) {
            String mode = bypassDelay1214.isValue() ? "1214" : "165";
            connectionManager.sendToAllClients("bypass_mode:" + mode);
        }
    }

    @EventHandler
    public void onChat(ChatEvent e) {
        String message = e.getMessage();
        if (leaveType.isSelected("Покупающий")) {
            if (message.contains("У Вас не хватает денег!")) {
                buyTimer.resetCounter();
                updateTimer.resetCounter();
                return;
            }
            Matcher matcher = PURCHASE_PATTERN.matcher(message);
            if (matcher.find()) {
                String itemName = matcher.group(1);
                String priceStr = matcher.group(2).replace(",", "");
                try {
                    int price = Integer.parseInt(priceStr);
                    AutoBuyableItem purchasedItem = autoBuyManager.getAllItems().stream()
                            .filter(item -> item.getDisplayName().equals(itemName))
                            .findFirst()
                            .orElse(null);
                    if (purchasedItem != null) {
                        PurchaseHistoryWindow.addPurchase(purchasedItem, price);
                    } else {
                        PurchaseHistoryWindow.addPurchase(itemName, price);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (!autoBuyManager.isEnabled()) return;

        if (waitingForServerLoad) {
            waitingForServerLoad = false;
            justEntered = true;
            enterDelayTimer.resetCounter();
            switchTimer.resetCounter();
        }

        if (leaveType.isSelected("Покупающий") && (bypassDelay.isValue() || bypassDelay1214.isValue())) {
            if (justEntered && enterDelayTimer.hasTimeElapsed(2000)) {
                if (!spammingAh) {
                    spammingAh = true;
                    ahSpamTimer.resetCounter();
                }
            }
            if (spammingAh) {
                if (ahSpamTimer.hasTimeElapsed(10500)) {
                    if (mc.player.networkHandler != null) {
                        mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket("/ah", Instant.now(), 0L, null, new LastSeenMessageList.Acknowledgment(0, new BitSet())));
                    }
                    ahSpamTimer.resetCounter();
                }
            }
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();
            int syncId = screen.getScreenHandler().syncId;
            List<Slot> slots = screen.getScreenHandler().slots;

            if (title.contains("Аукцион") || title.contains("Аукционы") || title.contains("Поиск")) {
                if (!open) {
                    open = true;
                    openTimer.resetCounter();
                    updateTimer.resetCounter();
                    buyTimer.resetCounter();
                    serverInAuction = true;
                    queueManager.clearNotFound();
                    justEntered = false;
                    spammingAh = false;
                    if (leaveType.isSelected("Проверяющий")) {
                        connectionManager.sendToServer("enter_auction");
                    }
                    return;
                }

                if (leaveType.isSelected("Покупающий")) {
                    long clientCount = connectionManager.getClientsInAuctionCount();

                    if (queueManager.getQueueSize() > 30) {
                        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                        connectionManager.sendToAllClients("update_now");
                        updateTimer.resetCounter();
                        queueManager.clearQueues();
                        queueManager.clearNotFound();
                        failedCount = 0;
                    }

                    BuyQueueManager.BuyRequest request = queueManager.getNextRequest();
                    if (request != null) {
                        BuyQueueManager.BuyRequest finalRequest = request;
                        connectionManager.getExecutorService().execute(() -> {
                            Slot targetSlot = findSlotByItemAndPrice(slots, finalRequest.itemName, finalRequest.price);
                            if (targetSlot != null) {
                                mc.execute(() -> {
                                    mc.interactionManager.clickSlot(syncId, targetSlot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                                    buyTimer.resetCounter();
                                    updateTimer.resetCounter();
                                    failedCount = 0;
                                });
                            } else {
                                String itemKey = finalRequest.itemName + "|" + finalRequest.price;
                                queueManager.addNotFound(itemKey);
                                failedCount++;
                            }
                        });
                    }

                    if (failedCount > 3) {
                        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                        connectionManager.sendToAllClients("update_now");
                        updateTimer.resetCounter();
                        queueManager.clearQueues();
                        queueManager.clearNotFound();
                        failedCount = 0;
                    }

                    if (updateTimer.hasTimeElapsed((long) timer2.getValue()) && serverInAuction && clientCount > 0 && queueManager.getQueueSize() == 0) {
                        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                        connectionManager.sendToAllClients("update_now");
                        updateTimer.resetCounter();
                        queueManager.clearNotFound();
                    }
                } else if (leaveType.isSelected("Проверяющий")) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastScanTime >= 10) {
                        lastScanTime = currentTime;
                        connectionManager.getExecutorService().execute(() -> {
                            List<Slot> bestSlots = findMatchingSlots(slots);
                            if (!bestSlots.isEmpty()) {
                                for (Slot bestSlot : bestSlots) {
                                    ItemStack stack = bestSlot.getStack();
                                    String itemName = stack.getName().getString();
                                    String cleanName = AuctionUtils.funTimePricePattern.matcher(itemName).replaceAll("").trim();
                                    int price = AuctionUtils.getPrice(stack);
                                    String itemKey = cleanName + "|" + price;

                                    Long lastSent = queueManager.getCooldown(itemKey);
                                    if (lastSent == null || (currentTime - lastSent) > 100) {
                                        queueManager.setCooldown(itemKey, currentTime);
                                        connectionManager.sendToServer("buy:" + cleanName + "|" + price);
                                    }
                                }
                                buyTimer.resetCounter();
                            }
                        });
                    }
                }
            } else if (title.contains("Подозрительная цена")) {
                openTimer.resetCounter();
                buyTimer.resetCounter();
                Slot confirmSlot = slots.stream()
                        .filter(slot -> !slot.getStack().isEmpty())
                        .filter(slot -> slot.getStack().getItem() == Items.GREEN_STAINED_GLASS_PANE)
                        .findFirst()
                        .orElse(null);
                if (confirmSlot != null) {
                    mc.interactionManager.clickSlot(syncId, confirmSlot.id, 0, SlotActionType.PICKUP, mc.player);
                }
            } else {
                if (open) {
                    open = false;
                    serverInAuction = false;
                    queueManager.clearNotFound();
                    if (leaveType.isSelected("Проверяющий")) {
                        connectionManager.sendToServer("leave_auction");
                    }
                }
            }
        } else {
            if (open) {
                open = false;
                serverInAuction = false;
                queueManager.clearNotFound();
                if (leaveType.isSelected("Проверяющий")) {
                    connectionManager.sendToServer("leave_auction");
                }
            }
        }

        if (leaveType.isSelected("Покупающий") && (bypassDelay.isValue() || bypassDelay1214.isValue())) {
            if (switchTimer.hasTimeElapsed(90000)) {
                boolean is1214 = bypassDelay1214.isValue();
                String newServer = serverManager.getNextServer(is1214);

                mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(newServer, Instant.now(), 0L, null, new LastSeenMessageList.Acknowledgment(0, new BitSet())));
                connectionManager.sendToAllClients("switch_server:" + newServer);
                waitingForServerLoad = true;
                switchTimer.resetCounter();
            }
        }
    }

    private Slot findSlotByItemAndPrice(List<Slot> slots, String itemName, int expectedPrice) {
        for (Slot slot : slots) {
            if (slot.getStack().isEmpty() || slot.id > 44) continue;
            ItemStack stack = slot.getStack();
            String stackName = stack.getName().getString();
            stackName = AuctionUtils.funTimePricePattern.matcher(stackName).replaceAll("").trim();
            int price = AuctionUtils.getPrice(stack);
            if (stackName.equals(itemName) && price == expectedPrice) {
                return slot;
            }
        }
        return null;
    }

    private List<Slot> findMatchingSlots(List<Slot> slots) {
        List<Slot> matching = new ArrayList<>();
        List<AutoBuyableItem> enabledItems = new ArrayList<>();

        for (AutoBuyableItem item : autoBuyManager.getAllItems()) {
            if (item.isEnabled()) {
                enabledItems.add(item);
            }
        }

        for (Slot slot : slots) {
            if (slot.getStack().isEmpty() || slot.id > 44) continue;
            ItemStack stack = slot.getStack();
            int price = AuctionUtils.getPrice(stack);
            if (price <= 0) continue;

            for (AutoBuyableItem item : enabledItems) {
                int maxPrice = item.getSettings().getBuyBelow();
                if (price > maxPrice) continue;

                if (item.getSettings().isCanHaveQuantity()) {
                    int stackCount = stack.getCount();
                    if (stackCount < item.getSettings().getMinQuantity()) continue;
                }

                if (AuctionUtils.compareItem(stack, item.createItemStack())) {
                    matching.add(slot);
                    break;
                }
            }
        }

        matching.sort(Comparator.comparingInt(slot -> AuctionUtils.getPrice(slot.getStack())));
        return matching.isEmpty() ? matching : matching.subList(0, Math.min(3, matching.size()));
    }
}