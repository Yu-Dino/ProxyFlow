package net.yuflow.proxyFlow.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.yuflow.proxyFlow.config.ConfigManager;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class QueueManager {
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final PriorityBlockingQueue<QueueEntry> queue;
    private final Map<UUID, QueueEntry> playerEntries;
    private boolean processingQueue = false;

    public QueueManager(ProxyServer server, Logger logger, ConfigManager configManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        this.queue = new PriorityBlockingQueue<>();
        this.playerEntries = new ConcurrentHashMap<>();
    }

    public void addToQueue(Player player) {
        if (playerEntries.containsKey(player.getUniqueId())) return;

        int priority = getPriority(player);
        QueueEntry entry = new QueueEntry(player, priority);
        queue.offer(entry);
        playerEntries.put(player.getUniqueId(), entry);

        logger.info("[ProxyFlow] Player {} added to queue with priority {}", player.getUsername(), priority);
        updateQueueMessages();
    }

    public void removeFromQueue(UUID playerId) {
        QueueEntry entry = playerEntries.remove(playerId);
        if (entry != null) {
            queue.remove(entry);
            updateQueueMessages();
        }
    }

    public boolean isInQueue(UUID playerId) { return playerEntries.containsKey(playerId); }

    public int getPosition(UUID playerId) {
        QueueEntry entry = playerEntries.get(playerId);
        if (entry == null) return -1;

        List<QueueEntry> sortedQueue = new ArrayList<>(queue);
        sortedQueue.sort(Comparator.comparingInt(QueueEntry::getPriority).reversed().thenComparingLong(QueueEntry::getTimestamp));

        for (int i = 0; i < sortedQueue.size(); i++) {
            if (sortedQueue.get(i).getPlayer().getUniqueId().equals(playerId)) return i + 1;
        }
        return -1;
    }

    public void processQueue() {
        if (!configManager.isQueueEnabled() || processingQueue) return;

        processingQueue = true;
        try {
            String targetServerName = configManager.getQueueTargetServer();
            Optional<RegisteredServer> targetServer = server.getServer(targetServerName);

            if (!targetServer.isPresent()) {
                logger.warn("[ProxyFlow] Target server '{}' not found!", targetServerName);
                return;
            }

            int maxPlayers = configManager.getQueueMaxPlayers();
            int currentPlayers = (int) server.getAllPlayers().stream()
                    .filter(p -> p.getCurrentServer().isPresent() && p.getCurrentServer().get().getServerInfo().getName().equals(targetServerName))
                    .count();

            while (!queue.isEmpty() && currentPlayers < maxPlayers) {
                QueueEntry entry = queue.poll();
                if (entry == null) break;

                Player player = entry.getPlayer();
                if (!player.isActive()) {
                    playerEntries.remove(player.getUniqueId());
                    continue;
                }

                player.createConnectionRequest(targetServer.get()).fireAndForget();
                playerEntries.remove(player.getUniqueId());
                logger.info("[ProxyFlow] Player {} sent from queue to {}", player.getUsername(), targetServerName);
                currentPlayers++;
            }
            updateQueueMessages();
        } finally {
            processingQueue = false;
        }
    }

    private int getPriority(Player player) {
        if (player.hasPermission(configManager.getQueueBypassPermission())) return Integer.MAX_VALUE;
        Map<String, Integer> priorities = configManager.getQueuePriorityPermissions();
        int maxPriority = 0;
        for (Map.Entry<String, Integer> priorityEntry : priorities.entrySet()) {
            if (player.hasPermission(priorityEntry.getKey())) maxPriority = Math.max(maxPriority, priorityEntry.getValue());
        }
        return maxPriority;
    }

    private void updateQueueMessages() {
        int totalInQueue = queue.size();
        String messageTemplate = configManager.getQueueMessage();

        for (QueueEntry entry : queue) {
            Player player = entry.getPlayer();
            if (player.isActive()) {
                int position = getPosition(player.getUniqueId());
                String message = messageTemplate.replace("{position}", String.valueOf(position)).replace("{total}", String.valueOf(totalInQueue));
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            }
        }
    }

    private static class QueueEntry implements Comparable<QueueEntry> {
        private final Player player;
        private final int priority;
        private final long timestamp;

        public QueueEntry(Player player, int priority) {
            this.player = player;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }

        public Player getPlayer() { return player; }
        public int getPriority() { return priority; }
        public long getTimestamp() { return timestamp; }

        @Override
        public int compareTo(QueueEntry other) {
            int priorityCompare = Integer.compare(other.priority, this.priority);
            if (priorityCompare != 0) return priorityCompare;
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
}