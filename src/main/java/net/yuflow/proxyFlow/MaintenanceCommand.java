package net.yuflow.proxyFlow;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MaintenanceCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;

    public MaintenanceCommand(ConfigManager configManager, ProxyServer server) {
        this.configManager = configManager;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        boolean currentState = configManager.isMaintenanceEnabled();
        boolean newState = !currentState;

        configManager.setMaintenance(newState);

        if (newState) {
            invocation.source().sendMessage(Component.text("This server is now in maintenance mode", NamedTextColor.YELLOW));

            Component kickMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMaintenanceKickMessage());
            String bypassPermission = configManager.getMaintenanceBypassPermission();

            for (Player player : server.getAllPlayers()) {
                if (!player.hasPermission(bypassPermission)) {
                    player.disconnect(kickMessage);
                }
            }
        } else {
            invocation.source().sendMessage(Component.text("This server is no longer in maintenance mode", NamedTextColor.GREEN));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyflow.command.maintenance");
    }
}