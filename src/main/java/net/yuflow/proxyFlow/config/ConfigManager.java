package net.yuflow.proxyFlow.config;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private final Logger logger;
    private final Path dataDirectory;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    @Inject
    public ConfigManager(Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        Path configFile = dataDirectory.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder().path(configFile).build();

        if (Files.notExists(configFile)) {
            try {
                Files.createDirectories(dataDirectory);
                try (InputStream in = this.getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                    }
                }
            } catch (IOException e) {
                logger.error("Could not create config.yml", e);
            }
        }
        this.loadConfig();
    }

    public void loadConfig() {
        try {
            this.root = this.loader.load();
        } catch (ConfigurateException e) {
            this.logger.error("Failed to load configuration", e);
        }
    }

    public void saveConfig() {
        try {
            this.loader.save(this.root);
        } catch (ConfigurateException e) {
            this.logger.error("Failed to save configuration", e);
        }
    }

    public boolean isMaintenanceEnabled() { return this.root.node("maintenance", "enabled").getBoolean(false); }
    public void setMaintenance(boolean enabled) {
        try { this.root.node("maintenance", "enabled").set(enabled); saveConfig(); }
        catch (SerializationException e) { logger.error("Error saving maintenance state", e); }
    }
    public String getMaintenanceMotd() { return this.root.node("maintenance", "motd").getString(); }
    public String getMaintenanceKickMessage() { return this.root.node("maintenance", "kick-message").getString(); }
    public String getMaintenanceBypassPermission() { return this.root.node("maintenance", "bypass-permission").getString("proxyflow.maintenance.bypass"); }

    public boolean isWhitelistEnabled() { return this.root.node("whitelist", "enabled").getBoolean(false); }
    public void setWhitelistEnabled(boolean enabled) {
        try { this.root.node("whitelist", "enabled").set(enabled); saveConfig(); }
        catch (SerializationException e) { logger.error("Error saving whitelist state", e); }
    }
    public String getWhitelistKickMessage() { return this.root.node("whitelist", "kick-message").getString(); }
    public String getWhitelistBypassPermission() { return this.root.node("whitelist", "bypass-permission").getString(); }
    public List<String> getWhitelistPlayers() {
        try { return this.root.node("whitelist", "players").getList(String.class, Collections.emptyList()); }
        catch (SerializationException e) { return Collections.emptyList(); }
    }
    public void addWhitelistPlayer(String player) {
        List<String> players = new ArrayList<>(getWhitelistPlayers());
        if (!players.contains(player)) {
            players.add(player);
            try { this.root.node("whitelist", "players").set(players); saveConfig(); }
            catch (SerializationException e) { logger.error("Error adding whitelist player", e); }
        }
    }
    public boolean removeWhitelistPlayer(String player) {
        List<String> players = new ArrayList<>(getWhitelistPlayers());
        if (players.remove(player)) {
            try { this.root.node("whitelist", "players").set(players); saveConfig(); return true; }
            catch (SerializationException e) { logger.error("Error removing whitelist player", e); }
        }
        return false;
    }
    public boolean isPlayerWhitelisted(String username) { return getWhitelistPlayers().stream().anyMatch(p -> p.equalsIgnoreCase(username)); }

    public boolean isQueueEnabled() { return this.root.node("queue", "enabled").getBoolean(false); }
    public int getQueueMaxPlayers() { return this.root.node("queue", "max-players").getInt(100); }
    public String getQueueServer() { return this.root.node("queue", "queue-server").getString("queue"); }
    public String getQueueTargetServer() { return this.root.node("queue", "target-server").getString("lobby"); }
    public String getQueueMessage() { return this.root.node("queue", "queue-message").getString(); }
    public String getQueueBypassPermission() { return this.root.node("queue", "bypass-permission").getString(); }
    public Map<String, Integer> getQueuePriorityPermissions() {
        Map<String, Integer> priorities = new HashMap<>();
        CommentedConfigurationNode node = this.root.node("queue", "priority-permissions");
        for (Map.Entry<Object, CommentedConfigurationNode> entry : node.childrenMap().entrySet()) {
            priorities.put(entry.getKey().toString(), entry.getValue().getInt(0));
        }
        return priorities;
    }

    public boolean isVpnCheckEnabled() { return this.root.node("security", "vpn-check", "enabled").getBoolean(true); }
    public String getVpnCheckApiKey() { return this.root.node("security", "vpn-check", "api-key").getString(""); }
    public int getVpnCacheDuration() { return this.root.node("security", "vpn-check", "cache-duration-minutes").getInt(60); }
    public String getVpnBypassPermission() { return this.root.node("security", "vpn-check", "bypass-permission").getString(); }
    public List<String> getVpnWhitelistedPlayers() {
        try { return this.root.node("security", "vpn-check", "whitelisted-players").getList(String.class, Collections.emptyList()); }
        catch (SerializationException e) { return Collections.emptyList(); }
    }
    public boolean isPlayerVpnWhitelisted(String username) { return getVpnWhitelistedPlayers().stream().anyMatch(p -> p.equalsIgnoreCase(username)); }
    public boolean isBlockServerIps() { return this.root.node("security", "vpn-check", "block-server-ips").getBoolean(true); }
    public void setBlockServerIps(boolean enabled) {
        try { this.root.node("security", "vpn-check", "block-server-ips").set(enabled); saveConfig(); }
        catch (SerializationException e) { logger.error("Error saving block-server-ips", e); }
    }
    public boolean isNotifyAdmins() { return this.root.node("security", "vpn-check", "notify-admins").getBoolean(true); }
    public void setNotifyAdmins(boolean enabled) {
        try { this.root.node("security", "vpn-check", "notify-admins").set(enabled); saveConfig(); }
        catch (SerializationException e) { logger.error("Error saving notify-admins", e); }
    }

    public boolean isCountryBlockEnabled() { return this.root.node("security", "country-block", "enabled").getBoolean(false); }
    public String getCountryBlockMode() { return this.root.node("security", "country-block", "mode").getString("blacklist"); }
    public List<String> getCountryList() {
        try { return this.root.node("security", "country-block", "countries").getList(String.class, Collections.emptyList()); }
        catch (SerializationException e) { return Collections.emptyList(); }
    }

    public boolean isMultiAccountCheckEnabled() { return this.root.node("security", "multi-account", "enabled").getBoolean(true); }
    public String getMultiAccountBypassPermission() { return this.root.node("security", "multi-account", "bypass-permission").getString(); }
    public String getMultiAccountAction() { return this.root.node("security", "multi-account", "action").getString("kick"); }
    public String getMultiAccountKickMessage() { return this.root.node("security", "multi-account", "kick-message").getString(); }
    public List<String> getMultiAccountWhitelistedPlayers() {
        try { return this.root.node("security", "multi-account", "whitelisted-players").getList(String.class, Collections.emptyList()); }
        catch (SerializationException e) { return Collections.emptyList(); }
    }
    public boolean isPlayerMultiAccountWhitelisted(String username) { return getMultiAccountWhitelistedPlayers().stream().anyMatch(p -> p.equalsIgnoreCase(username)); }

    public boolean isDiscordEnabled() { return this.root.node("discord", "enabled").getBoolean(false); }
    public String getDiscordWebhookUrl() { return this.root.node("discord", "webhook-url").getString(""); }
    public boolean isDiscordNotifyVpnBlock() { return this.root.node("discord", "notify-vpn-block").getBoolean(true); }
    public boolean isDiscordNotifyMaintenance() { return this.root.node("discord", "notify-maintenance").getBoolean(true); }

    public String getStaffChatFormat() { return this.root.node("staffchat", "format").getString("&8[&cSC&8] &e{player}&8: &f{message}"); }
}