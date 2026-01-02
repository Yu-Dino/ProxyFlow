package net.yuflow.proxyFlow.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.yuflow.proxyFlow.config.ConfigManager;
import net.yuflow.proxyFlow.services.DiscordService;

public class MaintenanceCommand implements SimpleCommand {
    private final ConfigManager configManager;
    private final ProxyServer server;
    private final DiscordService discordService;

    public MaintenanceCommand(ConfigManager configManager, ProxyServer server, DiscordService discordService) {
        this.configManager = configManager;
        this.server = server;
        this.discordService = discordService;
    }

    @Override
    public void execute(Invocation invocation) {
        boolean newState = !configManager.isMaintenanceEnabled();
        configManager.setMaintenance(newState);

        if (newState) {
            invocation.source().sendMessage(Component.text("Maintenance mode ENABLED", NamedTextColor.YELLOW));
            if (configManager.isDiscordNotifyMaintenance()) {
                discordService.sendNotification("Maintenance Update", "Maintenance mode has been **ENABLED**.", 16776960);
            }
            Component kickMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMaintenanceKickMessage());
            for (Player player : server.getAllPlayers()) {
                if (!player.hasPermission(configManager.getMaintenanceBypassPermission())) {
                    player.disconnect(kickMessage);
                }
            }
        } else {
            invocation.source().sendMessage(Component.text("Maintenance mode DISABLED", NamedTextColor.GREEN));
            if (configManager.isDiscordNotifyMaintenance()) {
                discordService.sendNotification("Maintenance Update", "Maintenance mode has been **DISABLED**.", 65280);
            }
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyflow.command.maintenance");
    }
}