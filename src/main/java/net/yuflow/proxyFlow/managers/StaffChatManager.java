package net.yuflow.proxyFlow.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.yuflow.proxyFlow.config.ConfigManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StaffChatManager {
    private final Set<UUID> toggledPlayers;
    private final ProxyServer server;
    private final ConfigManager configManager;

    public StaffChatManager(ProxyServer server, ConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
        this.toggledPlayers = new HashSet<>();
    }

    public boolean toggle(UUID playerId) {
        if (toggledPlayers.contains(playerId)) {
            toggledPlayers.remove(playerId);
            return false;
        } else {
            toggledPlayers.add(playerId);
            return true;
        }
    }

    public boolean isToggled(UUID playerId) {
        return toggledPlayers.contains(playerId);
    }

    public void broadcast(Player sender, String message) {
        String format = configManager.getStaffChatFormat();
        String formattedMessage = format
                .replace("{player}", sender.getUsername())
                .replace("{message}", message);

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(formattedMessage);

        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("proxyflow.staffchat")) {
                player.sendMessage(component);
            }
        }
    }
}