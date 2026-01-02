package net.yuflow.proxyFlow.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.yuflow.proxyFlow.config.ConfigManager;
import net.yuflow.proxyFlow.managers.StaffChatManager;
import net.yuflow.proxyFlow.services.DatabaseManager;
import net.yuflow.proxyFlow.services.DiscordService;
import net.yuflow.proxyFlow.services.SecurityService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ConnectionListener {
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final SecurityService securityService;
    private final DiscordService discordService;
    private final StaffChatManager staffChatManager;

    private final Map<String, Long> connectionTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> violationCounts = new ConcurrentHashMap<>();
    private static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]{3,16}$");

    public ConnectionListener(ProxyServer server, Logger logger, ConfigManager configManager,
                              DatabaseManager databaseManager, SecurityService securityService,
                              DiscordService discordService, StaffChatManager staffChatManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.securityService = securityService;
        this.discordService = discordService;
        this.staffChatManager = staffChatManager;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onPreLogin(PreLoginEvent event) {
        String ipAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
        String username = event.getUsername();

        if (this.databaseManager.isBanned(ipAddress)) {
            this.deny(event, "Your IP is temporarily banned due to suspicious activity.");
            return;
        }

        if (!validateConnectionSpeed(ipAddress) || !validateUsername(event, username, ipAddress)) {
            return;
        }

        if (configManager.isVpnCheckEnabled() || configManager.isCountryBlockEnabled()) {
            SecurityService.VpnCheckResult result = securityService.checkIp(ipAddress);
            processSecurityResult(event, result, username, ipAddress);
        }
    }

    private boolean validateConnectionSpeed(String ipAddress) {
        long currentTime = System.currentTimeMillis();
        Long lastConnection = this.connectionTimestamps.get(ipAddress);
        this.connectionTimestamps.put(ipAddress, currentTime);

        if (lastConnection != null && currentTime - lastConnection < 2000L) {
            incrementViolation(ipAddress);
            return false;
        }
        return true;
    }

    private boolean validateUsername(PreLoginEvent event, String username, String ipAddress) {
        if (!VALID_USERNAME_PATTERN.matcher(username).matches()) {
            this.deny(event, "Invalid username characters.");
            incrementViolation(ipAddress);
            return false;
        }
        return true;
    }

    private void processSecurityResult(PreLoginEvent event, SecurityService.VpnCheckResult result, String username, String ipAddress) {
        if (configManager.isVpnCheckEnabled() && !configManager.isPlayerVpnWhitelisted(username)) {
            if (result.isVpn && !event.getResult().isAllowed()) return;

            if (result.isVpn || (configManager.isBlockServerIps() && result.isHosting)) {
                if (!configManager.isVpnCheckEnabled()) return;

                this.deny(event, "VPN/Proxy connections are not allowed.");
                notifyAdmins(username, ipAddress, "VPN/Proxy/Hosting");
                if (configManager.isDiscordNotifyVpnBlock()) {
                    discordService.sendNotification("Security Block",
                            "Blocked " + username + " (" + ipAddress + ") due to VPN/Proxy usage.", 16711680);
                }
                return;
            }
        }

        if (configManager.isCountryBlockEnabled() && result.countryCode != null) {
            String mode = configManager.getCountryBlockMode();
            List<String> countries = configManager.getCountryList();
            boolean blocked = "blacklist".equalsIgnoreCase(mode) ? countries.contains(result.countryCode) : !countries.contains(result.countryCode);

            if (blocked) {
                this.deny(event, "Connections from your country are not allowed.");
            }
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String playerIp = player.getRemoteAddress().getAddress().getHostAddress();

        if (configManager.isWhitelistEnabled()
                && !player.hasPermission(configManager.getWhitelistBypassPermission())
                && !configManager.isPlayerWhitelisted(player.getUsername())) {
            event.setResult(ComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getWhitelistKickMessage())));
            return;
        }

        if (configManager.isMaintenanceEnabled() && !player.hasPermission(configManager.getMaintenanceBypassPermission())) {
            event.setResult(ComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMaintenanceKickMessage())));
            return;
        }

        if (this.server.getPlayer(player.getUsername()).isPresent()) {
            event.setResult(ComponentResult.denied(Component.text("Already connected.", NamedTextColor.RED)));
            return;
        }

        checkMultiAccount(event, player, playerIp);
    }

    private void checkMultiAccount(LoginEvent event, Player player, String playerIp) {
        if (configManager.isMultiAccountCheckEnabled()
                && !player.hasPermission(configManager.getMultiAccountBypassPermission())
                && !configManager.isPlayerMultiAccountWhitelisted(player.getUsername())) {

            for (Player onlinePlayer : this.server.getAllPlayers()) {
                if (onlinePlayer.getRemoteAddress().getAddress().getHostAddress().equals(playerIp)) {
                    if (configManager.isPlayerMultiAccountWhitelisted(onlinePlayer.getUsername())) continue;

                    if ("kick".equalsIgnoreCase(configManager.getMultiAccountAction())) {
                        event.setResult(ComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMultiAccountKickMessage())));
                        incrementViolation(playerIp);
                        notifyAdmins(player.getUsername(), playerIp, "Multi-Account");
                        return;
                    }
                }
            }
        }
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if (staffChatManager.isToggled(player.getUniqueId())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            staffChatManager.broadcast(player, event.getMessage());
        }
    }

    private void notifyAdmins(String playerName, String ipAddress, String reason) {
        if (!configManager.isNotifyAdmins()) return;
        Component message = Component.text("[ProxyFlow] ", NamedTextColor.RED)
                .append(Component.text(playerName, NamedTextColor.YELLOW))
                .append(Component.text(" blocked: " + reason, NamedTextColor.GRAY));

        for (Player onlinePlayer : server.getAllPlayers()) {
            if (onlinePlayer.hasPermission("proxyflow.notify")) {
                onlinePlayer.sendMessage(message);
            }
        }
    }

    private void incrementViolation(String ipAddress) {
        int violations = this.violationCounts.getOrDefault(ipAddress, 0) + 1;
        if (violations >= 3) {
            long banDuration = TimeUnit.MINUTES.toMillis(5);
            long expiry = System.currentTimeMillis() + banDuration;
            this.databaseManager.addBan(ipAddress, expiry);
            this.violationCounts.remove(ipAddress);
            this.logger.warn("IP " + ipAddress + " temporarily banned for 5 minutes.");
        } else {
            this.violationCounts.put(ipAddress, violations);
        }
    }

    private void deny(PreLoginEvent event, String reason) {
        event.setResult(PreLoginComponentResult.denied(Component.text(reason, NamedTextColor.RED)));
    }
}