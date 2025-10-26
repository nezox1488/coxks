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
import fun.rich.events.packet.PacketEvent;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import java.time.Instant;
import java.util.BitSet;
import net.minecraft.network.message.LastSeenMessageList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import java.util.HashMap;

public class AutoBuy extends Module {
    SelectSetting leaveType = new SelectSetting("Тип обхода", "Проверяющий").value("Проверяющий", "Покупающий");
    SliderSettings timer2 = new SliderSettings("Таймер обновления аукциона", "").setValue(500).range(250, 1000);
    BooleanSetting bypassDelay = new BooleanSetting("Обход задержки 1.16.5 анках", "");
    BooleanSetting bypassDelay1214 = new BooleanSetting("Обход задержки 1.21.4 анках", "");
    TimerUtil openTimer = TimerUtil.create();
    TimerUtil updateTimer = TimerUtil.create();
    TimerUtil buyTimer = TimerUtil.create();
    TimerUtil switchTimer = TimerUtil.create();
    TimerUtil enterDelayTimer = TimerUtil.create();
    TimerUtil ahSpamTimer = TimerUtil.create();
    ConcurrentLinkedQueue<BuyRequest> queue = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<BuyRequest> priorityQueue = new ConcurrentLinkedQueue<>();
    Set<String> notFoundItems = ConcurrentHashMap.newKeySet();
    Set<String> processedItems = ConcurrentHashMap.newKeySet();
    Set<String> sentItems = ConcurrentHashMap.newKeySet();
    AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    PrintWriter clientOut = null;
    BufferedReader clientIn = null;
    List<Socket> connections = new ArrayList<>();
    Map<Socket, PrintWriter> outs = new ConcurrentHashMap<>();
    Map<Socket, BufferedReader> ins = new ConcurrentHashMap<>();
    Map<Socket, Boolean> clientInAuction = new ConcurrentHashMap<>();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    boolean open = false;
    boolean serverInAuction = false;
    volatile boolean running = false;
    static final int PORT = 20001;
    int failedCount = 0;
    String currentServer = "";
    List<String> anarchyServers165 = new ArrayList<>();
    List<String> anarchyServers214 = new ArrayList<>();
    boolean justEntered = false;
    boolean spammingAh = false;
    boolean waitingForServerLoad = false;
    private static final Pattern PURCHASE_PATTERN = Pattern.compile("Вы успешно купили (.+?) за \\$([\\d,]+)!");
    int currentServerIndex = 0;
    List<AutoBuyableItem> cachedEnabledItems = new ArrayList<>();
    TimerUtil serverSwitchCooldown = TimerUtil.create();
    Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();

    public AutoBuy() {
        super("Auto Buy", "Auto Buy", ModuleCategory.MISC);
        anarchyServers165.addAll(List.of("/an102", "/an103", "/an104", "/an105", "/an106", "/an107"));
        for (int i = 203; i <= 221; i++) {
            anarchyServers165.add("/an" + i);
        }
        for (int i = 302; i <= 313; i++) {
            anarchyServers165.add("/an" + i);
        }
        anarchyServers165.addAll(List.of("/an502", "/an503", "/an504", "/an505", "/an506", "/an507", "/an602"));

        for (int i = 11; i <= 14; i++) {
            anarchyServers214.add("/an" + i);
        }
        for (int i = 21; i <= 27; i++) {
            anarchyServers214.add("/an" + i);
        }
        for (int i = 31; i <= 34; i++) {
            anarchyServers214.add("/an" + i);
        }
        for (int i = 51; i <= 53; i++) {
            anarchyServers214.add("/an" + i);
        }
        anarchyServers214.add("/an91");

        timer2.visible(() -> leaveType.isSelected("Покупающий"));
        bypassDelay.visible(() -> leaveType.isSelected("Покупающий"));
        bypassDelay1214.visible(() -> leaveType.isSelected("Покупающий"));

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
        serverSwitchCooldown.resetCounter();
        open = false;
        serverInAuction = false;
        running = true;
        notFoundItems.clear();
        processedItems.clear();
        sentItems.clear();
        lastMessageTime.clear();
        failedCount = 0;
        currentServer = "";
        justEntered = false;
        spammingAh = false;
        waitingForServerLoad = false;
        currentServerIndex = 0;
        cachedEnabledItems.clear();
        if (leaveType.isSelected("Покупающий") && (bypassDelay.isValue() || bypassDelay1214.isValue())) {
            mc.options.pauseOnLostFocus = false;
        }
        cacheEnabledItems();
        startConnection();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        running = false;
        executorService.shutdownNow();
        executorService = Executors.newFixedThreadPool(10);
        stopAll();
    }

    private void cacheEnabledItems() {
        cachedEnabledItems.clear();
        for (AutoBuyableItem item : autoBuyManager.getAllItems()) {
            if (item.isEnabled()) {
                cachedEnabledItems.add(item);
            }
        }
    }

    private void startConnection() {
        executorService.execute(() -> {
            while (running && state) {
                if (leaveType.isSelected("Покупающий")) {
                    if (serverSocket == null || serverSocket.isClosed()) {
                        try {
                            serverSocket = new ServerSocket(PORT);
                            ChatMessage.brandmessage("Сервер запущен на порту " + PORT);
                            executorService.execute(this::listenerThread);
                        } catch (IOException e) {
                            ChatMessage.brandmessage("Ошибка запуска сервера");
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                } else if (leaveType.isSelected("Проверяющий")) {
                    if (clientSocket == null || clientSocket.isClosed()) {
                        try {
                            clientSocket = new Socket("localhost", PORT);
                            clientSocket.setTcpNoDelay(true);
                            clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                            clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            clientOut.println("connect");
                            executorService.execute(this::clientReaderThread);
                            ChatMessage.brandmessage("Подключено к покупающему аккаунту");
                        } catch (IOException e) {
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                }
            }
        });
    }

    private void listenerThread() {
        try {
            while (state && serverSocket != null && !serverSocket.isClosed()) {
                Socket conn = serverSocket.accept();
                conn.setTcpNoDelay(true);
                connections.add(conn);
                PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                outs.put(conn, out);
                ins.put(conn, in);
                clientInAuction.put(conn, false);
                ChatMessage.brandmessage("Подключен аккаунт с проверяющим");
                executorService.execute(() -> readerThread(conn));
            }
        } catch (IOException ignored) {}
    }

    private void readerThread(Socket conn) {
        try {
            BufferedReader in = ins.get(conn);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("buy:")) {
                    try {
                        String[] parts = line.substring(4).split("\\|");
                        if (parts.length == 2) {
                            String itemName = parts[0];
                            int price = Integer.parseInt(parts[1]);
                            BuyRequest request = new BuyRequest(itemName, price);
                            priorityQueue.add(request);
                        }
                    } catch (NumberFormatException ignored) {}
                } else if (line.equals("enter_auction")) {
                    clientInAuction.put(conn, true);
                } else if (line.equals("leave_auction")) {
                    clientInAuction.put(conn, false);
                } else if (line.startsWith("switch_server:")) {
                    String cmd = line.substring(14);
                    mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(cmd, Instant.now(), 0L, null, new LastSeenMessageList.Acknowledgment(0, new BitSet())));
                    waitingForServerLoad = true;
                } else if (line.equals("connect")) {
                }
            }
        } catch (IOException ignored) {}
        finally {
            removeConnection(conn);
        }
    }

    private void clientReaderThread() {
        try {
            String line;
            while ((line = clientIn.readLine()) != null) {
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
                }
            }
        } catch (IOException ignored) {}
        finally {
            stopClient();
        }
    }

    private void removeConnection(Socket conn) {
        connections.remove(conn);
        outs.remove(conn);
        ins.remove(conn);
        clientInAuction.remove(conn);
        try {
            conn.close();
        } catch (IOException ignored) {}
    }

    private void stopAll() {
        queue.clear();
        priorityQueue.clear();
        notFoundItems.clear();
        processedItems.clear();
        sentItems.clear();
        lastMessageTime.clear();
        cachedEnabledItems.clear();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }
        for (Socket conn : new ArrayList<>(connections)) {
            removeConnection(conn);
        }
        stopClient();
    }

    private void stopClient() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
            clientSocket = null;
        }
        clientOut = null;
        clientIn = null;
    }

    private int getCurrentAnarchyNumber() {
        if (mc.world == null) return -1;
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective != null) {
            String displayName = objective.getDisplayName().getString();
            if (displayName.contains("Анархия-")) {
                String[] parts = displayName.split("-");
                if (parts.length > 1) {
                    try {
                        return Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private String getNextServer(List<String> servers) {
        if (servers.isEmpty()) return null;

        int currentAnarchy = getCurrentAnarchyNumber();

        if (currentAnarchy != -1) {
            String currentServerCmd = "/an" + currentAnarchy;
            int currentIdx = servers.indexOf(currentServerCmd);

            if (currentIdx != -1) {
                currentServerIndex = currentIdx;
            }
        }

        currentServerIndex = (currentServerIndex + 1) % servers.size();
        return servers.get(currentServerIndex);
    }

    private void switchToNextServer() {
        if (!serverSwitchCooldown.hasTimeElapsed(3000)) {
            return;
        }

        if (!leaveType.isSelected("Покупающий")) {
            return;
        }

        List<String> availableServers;
        if (bypassDelay1214.isValue()) {
            availableServers = new ArrayList<>(anarchyServers214);
        } else if (bypassDelay.isValue()) {
            availableServers = new ArrayList<>(anarchyServers165);
        } else {
            return;
        }

        String newServer = getNextServer(availableServers);

        if (newServer != null) {
            currentServer = newServer;
            mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(newServer, Instant.now(), 0L, null, new LastSeenMessageList.Acknowledgment(0, new BitSet())));
            sendToAllClients("switch_server:" + newServer);
            waitingForServerLoad = true;
            serverSwitchCooldown.resetCounter();
            switchTimer.resetCounter();
            ChatMessage.brandmessage("Переход на сервер: " + newServer);
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof GameMessageS2CPacket gameMessage) {
            Text content = gameMessage.content();
            String message = content.getString();

            if (message.contains("Вы уже подключены к этому серверу!")) {
                ChatMessage.brandmessage("Вижу сообщение: Вы уже подключены к этому серверу!");
                switchToNextServer();
                return;
            }

            if (leaveType.isSelected("Покупающий")) {
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

        if ((bypassDelay.isValue() || bypassDelay1214.isValue())) {
            if (justEntered && enterDelayTimer.hasTimeElapsed(2000)) {
                if (!spammingAh) {
                    spammingAh = true;
                    ahSpamTimer.resetCounter();
                }
            }
            if (spammingAh) {
                if (ahSpamTimer.hasTimeElapsed(1250)) {
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
                    notFoundItems.clear();
                    processedItems.clear();
                    sentItems.clear();
                    justEntered = false;
                    spammingAh = false;
                    cacheEnabledItems();
                    if (leaveType.isSelected("Проверяющий") && clientOut != null) {
                        clientOut.println("enter_auction");
                    }
                    return;
                }

                if (leaveType.isSelected("Покупающий")) {
                    long clientCount = clientInAuction.values().stream().filter(Boolean::booleanValue).count();

                    if (priorityQueue.size() + queue.size() > 30) {
                        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                        sendToAllClients("update_now");
                        updateTimer.resetCounter();
                        notFoundItems.clear();
                        queue.clear();
                        priorityQueue.clear();
                        failedCount = 0;
                    }

                    BuyRequest request = priorityQueue.poll();
                    if (request == null) {
                        request = queue.poll();
                    }

                    if (request != null) {
                        BuyRequest finalRequest = request;
                        Slot targetSlot = findSlotByItemAndPrice(slots, finalRequest.itemName, finalRequest.price);
                        if (targetSlot != null) {
                            mc.interactionManager.clickSlot(syncId, targetSlot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                            buyTimer.resetCounter();
                            updateTimer.resetCounter();
                            failedCount = 0;
                        } else {
                            String itemKey = finalRequest.itemName + "|" + finalRequest.price;
                            if (!notFoundItems.contains(itemKey)) {
                                notFoundItems.add(itemKey);
                            }
                            failedCount++;
                        }
                    }

                    if (failedCount > 3) {
                        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                        sendToAllClients("update_now");
                        updateTimer.resetCounter();
                        notFoundItems.clear();
                        queue.clear();
                        priorityQueue.clear();
                        failedCount = 0;
                    }

                    if (updateTimer.hasTimeElapsed((long) timer2.getValue()) && serverInAuction && clientCount > 0 && priorityQueue.isEmpty() && queue.isEmpty()) {
                        mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
                        sendToAllClients("update_now");
                        updateTimer.resetCounter();
                        notFoundItems.clear();
                    }
                } else if (leaveType.isSelected("Проверяющий")) {
                    List<Slot> bestSlots = findMatchingSlots(slots);
                    if (!bestSlots.isEmpty()) {
                        Map<String, Integer> itemCounts = new HashMap<>();
                        for (Slot bestSlot : bestSlots) {
                            ItemStack stack = bestSlot.getStack();
                            String itemName = stack.getName().getString();
                            String cleanName = AuctionUtils.funTimePricePattern.matcher(itemName).replaceAll("").trim();
                            int price = AuctionUtils.getPrice(stack);
                            String itemKey = cleanName + "|" + price;

                            itemCounts.put(cleanName, itemCounts.getOrDefault(cleanName, 0) + 1);

                            if (!sentItems.contains(itemKey)) {
                                sentItems.add(itemKey);
                                sendBuy(cleanName, price);
                            }
                        }

                        long currentTime = System.currentTimeMillis();
                        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                            String itemName = entry.getKey();
                            Long lastTime = lastMessageTime.get(itemName);
                            if (lastTime == null || currentTime - lastTime > 2000) {
                                ChatMessage.brandmessage("Вижу предмет: " + itemName + " x" + entry.getValue());
                                lastMessageTime.put(itemName, currentTime);
                            }
                        }
                        buyTimer.resetCounter();
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
                    notFoundItems.clear();
                    processedItems.clear();
                    sentItems.clear();
                    if (leaveType.isSelected("Проверяющий") && clientOut != null) {
                        clientOut.println("leave_auction");
                    }
                }
            }
        } else {
            if (open) {
                open = false;
                serverInAuction = false;
                notFoundItems.clear();
                processedItems.clear();
                sentItems.clear();
                if (leaveType.isSelected("Проверяющий") && clientOut != null) {
                    clientOut.println("leave_auction");
                }
            }
        }

        if (leaveType.isSelected("Покупающий") && (bypassDelay.isValue() || bypassDelay1214.isValue())) {
            if (switchTimer.hasTimeElapsed(50000)) {
                switchToNextServer();
            }
        }
    }

    private Slot findSlotByItemAndPrice(List<Slot> slots, String itemName, int expectedPrice) {
        for (int i = 0; i <= 44; i++) {
            Slot slot = slots.get(i);
            if (slot.getStack().isEmpty()) continue;
            ItemStack stack = slot.getStack();

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) {
                continue;
            }

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

        if (cachedEnabledItems.isEmpty()) {
            cacheEnabledItems();
        }

        for (int i = 0; i <= 44; i++) {
            Slot slot = slots.get(i);
            if (slot.getStack().isEmpty()) continue;
            ItemStack stack = slot.getStack();

            if (AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) {
                continue;
            }

            int price = AuctionUtils.getPrice(stack);
            if (price <= 0) continue;

            for (AutoBuyableItem item : cachedEnabledItems) {
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
        return matching;
    }

    private void sendToAllClients(String message) {
        for (Socket conn : connections) {
            if (clientInAuction.getOrDefault(conn, false)) {
                PrintWriter out = outs.get(conn);
                if (out != null) {
                    out.println(message);
                }
            }
        }
    }

    private void sendBuy(String itemName, int price) {
        if (clientOut != null) {
            clientOut.println("buy:" + itemName + "|" + price);
        }
    }

    private static class BuyRequest {
        String itemName;
        int price;

        BuyRequest(String itemName, int price) {
            this.itemName = itemName;
            this.price = price;
        }
    }
}