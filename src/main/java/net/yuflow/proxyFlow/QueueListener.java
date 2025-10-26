package net.yuflow.proxyFlow;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

public class QueueListener {
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final QueueManager queueManager;

    public QueueListener(ProxyServer server, Logger logger, ConfigManager configManager, QueueManager queueManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        this.queueManager = queueManager;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onServerConnected(ServerConnectedEvent event) {
        if (!configManager.isQueueEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String targetServer = configManager.getQueueTargetServer();
        String queueServer = configManager.getQueueServer();
        String connectedServer = event.getServer().getServerInfo().getName();

        if (event.getPreviousServer().isPresent()
                && event.getPreviousServer().get().getServerInfo().getName().equals(targetServer)) {
            queueManager.processQueue();
        }

        if (connectedServer.equals(queueServer) && !queueManager.isInQueue(player.getUniqueId())) {
            if (!player.hasPermission(configManager.getQueueBypassPermission())) {
                int maxPlayers = configManager.getQueueMaxPlayers();
                long currentPlayers = server.getAllPlayers().stream()
                        .filter(p -> p.getCurrentServer().isPresent()
                                && p.getCurrentServer().get().getServerInfo().getName().equals(targetServer))
                        .count();

                if (currentPlayers >= maxPlayers) {
                    queueManager.addToQueue(player);
                } else {
                    server.getServer(targetServer).ifPresent(target ->
                            player.createConnectionRequest(target).fireAndForget());
                }
            } else {
                server.getServer(targetServer).ifPresent(target ->
                        player.createConnectionRequest(target).fireAndForget());
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (!configManager.isQueueEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        if (queueManager.isInQueue(player.getUniqueId())) {
            queueManager.removeFromQueue(player.getUniqueId());
        }

        if (player.getCurrentServer().isPresent()) {
            String targetServer = configManager.getQueueTargetServer();
            String currentServer = player.getCurrentServer().get().getServerInfo().getName();

            if (currentServer.equals(targetServer)) {
                queueManager.processQueue();
            }
        }
    }
}