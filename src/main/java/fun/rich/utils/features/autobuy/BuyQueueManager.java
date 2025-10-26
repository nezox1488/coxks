package fun.rich.utils.features.autobuy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BuyQueueManager {
    private final ConcurrentLinkedQueue<BuyRequest> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BuyRequest> priorityQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> notFoundItems = ConcurrentHashMap.newKeySet();
    private final Set<String> processedItems = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> itemCooldowns = new ConcurrentHashMap<>();

    public void addToPriorityQueue(String itemName, int price) {
        priorityQueue.add(new BuyRequest(itemName, price));
    }

    public void addToQueue(String itemName, int price) {
        queue.add(new BuyRequest(itemName, price));
    }

    public BuyRequest getNextRequest() {
        BuyRequest request = priorityQueue.poll();
        if (request == null) {
            request = queue.poll();
        }
        return request;
    }

    public void addNotFound(String itemKey) {
        notFoundItems.add(itemKey);
    }

    public void clearNotFound() {
        notFoundItems.clear();
    }

    public void clearQueues() {
        queue.clear();
        priorityQueue.clear();
    }

    public void clearAll() {
        queue.clear();
        priorityQueue.clear();
        notFoundItems.clear();
        processedItems.clear();
        itemCooldowns.clear();
    }

    public int getQueueSize() {
        return queue.size() + priorityQueue.size();
    }

    public void setCooldown(String itemKey, long time) {
        itemCooldowns.put(itemKey, time);
    }

    public Long getCooldown(String itemKey) {
        return itemCooldowns.get(itemKey);
    }

    public static class BuyRequest {
        public final String itemName;
        public final int price;

        public BuyRequest(String itemName, int price) {
            this.itemName = itemName;
            this.price = price;
        }
    }
}