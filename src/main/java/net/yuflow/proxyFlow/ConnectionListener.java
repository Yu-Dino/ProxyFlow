package net.yuflow.proxyFlow;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

public class ConnectionListener {
    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager configManager;
    private final Gson gson = new Gson();
    private final Map<String, Long> connectionTimestamps = new ConcurrentHashMap();
    private static final long CONNECTION_TIMEOUT = 2000L;
    private static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private final Map<String, Integer> violationCounts = new ConcurrentHashMap();
    private final Map<String, Long> tempBannedIps = new ConcurrentHashMap();
    private static final int MAX_VIOLATIONS = 3;
    private static final long BAN_DURATION_MINUTES = 5L;
    private final Set<String> vpnIpCache = ConcurrentHashMap.newKeySet();

    public ConnectionListener(ProxyServer server, Logger logger, ConfigManager configManager) {
        this.server = server;
        this.logger = logger;
        this.configManager = configManager;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onPreLogin(PreLoginEvent event) {
        String ipAddress = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
        String username = event.getUsername();

        if (this.isIpBanned(ipAddress)) {
            this.deny(event, "Your ip got temporally banned", "");
        } else if (!this.performExternalChecks(event, ipAddress, username)) {
            long currentTime = System.currentTimeMillis();
            Long lastConnection = (Long)this.connectionTimestamps.get(ipAddress);
            if (lastConnection != null && currentTime - lastConnection < 2000L) {
                this.deny(event, "You are connecting to fast", "[ProxyFlow] Risk found! (Anti-Bot) for IP: {}. Join cancelled!", ipAddress);
                this.incrementViolation(ipAddress);
            } else {
                this.connectionTimestamps.put(ipAddress, currentTime);
                if (!VALID_USERNAME_PATTERN.matcher(username).matches()) {
                    this.deny(event, "Your username has disallowed letters", "[ProxyFlow] Risk found! (invalid Name: {}) for IP: {}. Join cancelled!", username, ipAddress);
                    this.incrementViolation(ipAddress);
                }
            }
        }
    }

    private boolean performExternalChecks(PreLoginEvent event, String ipAddress, String username) {
        String apiKey = this.configManager.getVpnCheckApiKey();
        if ((this.configManager.isVpnCheckEnabled() || this.configManager.isCountryBlockEnabled()) && apiKey != null && !apiKey.isEmpty() && !apiKey.equals("API KEY")) {
            try {
                URL url = new URL("https://proxycheck.io/v2/" + ipAddress + "?key=" + apiKey + "&vpn=1");
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() != 200) {
                    this.logger.warn("[ProxyFlow] Failed to send API request (Status-Code: {})", connection.getResponseCode());
                    return false;
                }

                JsonObject response = (JsonObject)(new Gson()).fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);
                if (response.has(ipAddress)) {
                    JsonObject ipInfo = response.getAsJsonObject(ipAddress);

                    if (this.configManager.isVpnCheckEnabled() && ipInfo.has("proxy") && "yes".equals(ipInfo.get("proxy").getAsString())) {
                        if (!configManager.isPlayerVpnWhitelisted(username)) {
                            this.vpnIpCache.add(ipAddress);
                        }
                    }

                    if (this.configManager.isCountryBlockEnabled() && ipInfo.has("isocode")) {
                        String countryCode = ipInfo.get("isocode").getAsString();

                        if (countryCode != null && !countryCode.isEmpty()) {
                            String mode = this.configManager.getCountryBlockMode();
                            List<String> countries = this.configManager.getCountryList();

                            boolean isBlacklisted = "blacklist".equalsIgnoreCase(mode) && countries.contains(countryCode);
                            boolean isNotWhitelisted = "whitelist".equalsIgnoreCase(mode) && !countries.contains(countryCode);

                            if (isBlacklisted || isNotWhitelisted) {
                                this.deny(event, "Connections from your country are not allowed!", "[ProxyFlow] Connection from country ({}) from IP {} blocked.", countryCode, ipAddress);
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception var11) {
                this.logger.error("[ProxyFlow] Failed to inspect IP address from outside" + ipAddress, var11);
            }
            return false;
        } else {
            return false;
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String playerIp = player.getRemoteAddress().getAddress().getHostAddress();
        String playerName = player.getUsername();

        if (vpnIpCache.contains(playerIp)) {
            vpnIpCache.remove(playerIp);
            if (!player.hasPermission(configManager.getVpnBypassPermission())) {
                event.setResult(ComponentResult.denied(Component.text("VPNs or Proxys are not allowed on this server").color(NamedTextColor.RED)));
                logger.warn("[ProxyFlow] VPN/Proxy from IP {} for Player {} blocked. Join cancelled!.", playerIp, playerName);
                return;
            }
        }

        if (configManager.isWhitelistEnabled()) {
            if (!player.hasPermission(configManager.getWhitelistBypassPermission())
                    && !configManager.isPlayerWhitelisted(playerName)) {
                event.setResult(ComponentResult.denied(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getWhitelistKickMessage())
                ));
                logger.warn("[ProxyFlow] Player {} is not whitelisted. Join cancelled!", playerName);
                return;
            }
        }

        if (configManager.isMaintenanceEnabled()) {
            if (!player.hasPermission(configManager.getMaintenanceBypassPermission())) {
                event.setResult(ComponentResult.denied(
                        LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMaintenanceKickMessage())
                ));
                return;
            }
        }

        if (event.getResult().isAllowed()) {
            if (this.server.getPlayer(playerName).isPresent()) {
                event.setResult(ComponentResult.denied(Component.text("A player with this name are already on this server").color(NamedTextColor.RED)));
                this.logger.warn("[ProxyFlow] Risk found (Double-Login) for player: {}. Join cancelled!", playerName);
                return;
            }

            if (configManager.isMultiAccountCheckEnabled()) {
                if (!player.hasPermission(configManager.getMultiAccountBypassPermission())
                        && !configManager.isPlayerMultiAccountWhitelisted(playerName)) {
                    for (Player onlinePlayer : this.server.getAllPlayers()) {
                        if (onlinePlayer.getRemoteAddress().getAddress().equals(player.getRemoteAddress().getAddress())) {
                            if (configManager.isPlayerMultiAccountWhitelisted(onlinePlayer.getUsername())) {
                                continue;
                            }

                            String action = configManager.getMultiAccountAction();

                            if ("kick".equalsIgnoreCase(action)) {
                                event.setResult(ComponentResult.denied(
                                        LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getMultiAccountKickMessage())
                                ));
                                this.logger.warn("[ProxyFlow] Risk found! (Multi-Account) for player: {} (IP: {}). player '{}' is already online. Join cancelled!",
                                        playerName, playerIp, onlinePlayer.getUsername());
                                this.incrementViolation(playerIp);
                                return;
                            } else {
                                this.logger.info("[ProxyFlow] Multi-Account detected for player: {} (IP: {}). player '{}' is already online. Join allowed by config.",
                                        playerName, playerIp, onlinePlayer.getUsername());
                            }
                        }
                    }
                }
            }

            this.logger.info("[ProxyFlow] Player {} (IP: {}) had all checks passed. no risks found!", playerName, playerIp);
        }
    }

    private void incrementViolation(String ipAddress) {
        int violations = (Integer)this.violationCounts.getOrDefault(ipAddress, 0) + 1;
        if (violations >= 3) {
            long banUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5L);
            this.tempBannedIps.put(ipAddress, banUntil);
            this.violationCounts.remove(ipAddress);
            this.logger.warn("[ProxyFlow] IP-Address {} get for {} Minutes temporally banned!", ipAddress, 5L);
        } else {
            this.violationCounts.put(ipAddress, violations);
        }
    }

    private boolean isIpBanned(String ipAddress) {
        Long banUntil = (Long)this.tempBannedIps.get(ipAddress);
        if (banUntil == null) {
            return false;
        } else if (System.currentTimeMillis() > banUntil) {
            this.tempBannedIps.remove(ipAddress);
            this.logger.info("[ProxyFlow] Tempban for IP {} rejected.", ipAddress);
            return false;
        } else {
            return true;
        }
    }

    private void deny(PreLoginEvent event, String playerMessage, String logMessage, Object... args) {
        event.setResult(PreLoginComponentResult.denied(Component.text(playerMessage).color(NamedTextColor.RED)));
        if (logMessage != null && !logMessage.isEmpty()) {
            this.logger.warn(logMessage, args);
        }
    }
}