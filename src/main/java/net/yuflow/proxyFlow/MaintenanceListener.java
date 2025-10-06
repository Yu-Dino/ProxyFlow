package net.yuflow.proxyFlow;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MaintenanceListener {

    private final ConfigManager configManager;

    public MaintenanceListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        if (configManager.isMaintenanceEnabled()) {
            ServerPing.Builder builder = event.getPing().asBuilder();
            builder.description(LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMaintenanceMotd()));
            builder.version(new ServerPing.Version(-1, "maintenance"));
            builder.maximumPlayers(0);
            builder.onlinePlayers(0);
            event.setPing(builder.build());
        }
    }
}