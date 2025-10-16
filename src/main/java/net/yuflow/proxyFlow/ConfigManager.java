package net.yuflow.proxyFlow;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader.Builder;

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
        this.loader = ((Builder)YamlConfigurationLoader.builder().path(configFile)).build();
        if (Files.notExists(configFile, new LinkOption[0])) {
            try {
                Files.createDirectories(dataDirectory);
                InputStream in = this.getClass().getResourceAsStream("/config.yml");

                try {
                    if (in != null) {
                        Files.copy(in, configFile, new CopyOption[0]);
                    } else {
                        logger.error("the standard config could not be found.");
                    }
                } catch (Throwable var8) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable var7) {
                            var8.addSuppressed(var7);
                        }
                    }

                    throw var8;
                }

                if (in != null) {
                    in.close();
                }
            } catch (IOException var9) {
                logger.error("cant create config.yml!", var9);
            }
        }

        this.loadConfig();
    }

    public void loadConfig() {
        try {
            this.root = (CommentedConfigurationNode)this.loader.load();
        } catch (ConfigurateException var2) {
            this.logger.error("Failed to load configuration!", var2);
        }
    }

    public void saveConfig() {
        try {
            this.loader.save(this.root);
        } catch (ConfigurateException e) {
            this.logger.error("Failed to safe configuration!", e);
        }
    }

    public boolean isMaintenanceEnabled() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"maintenance", "enabled"})).getBoolean(false);
    }

    public void setMaintenance(boolean enabled) {
        try {
            this.root.node("maintenance", "enabled").set(enabled);
            saveConfig();
        } catch (SerializationException e) {
            logger.error("cant write maintenance state in configuration!", e);
        }
    }

    public String getMaintenanceMotd() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"maintenance", "motd"})).getString("&cServer is in maintenance!");
    }

    public String getMaintenanceKickMessage() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"maintenance", "kick-message"})).getString("&cThis server is currently in maintenance");
    }

    public String getMaintenanceBypassPermission() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"maintenance", "bypass-permission"})).getString("proxyflow.maintenance.bypass");
    }

    public boolean isWhitelistEnabled() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"whitelist", "enabled"})).getBoolean(false);
    }

    public void setWhitelistEnabled(boolean enabled) {
        try {
            this.root.node("whitelist", "enabled").set(enabled);
            saveConfig();
        } catch (SerializationException e) {
            logger.error("cant write whitelist state in configuration!", e);
        }
    }

    public String getWhitelistKickMessage() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"whitelist", "kick-message"})).getString("&cYou are not whitelisted on this server!");
    }

    public String getWhitelistBypassPermission() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"whitelist", "bypass-permission"})).getString("proxyflow.whitelist.bypass");
    }

    public List<String> getWhitelistPlayers() {
        try {
            return (List)((CommentedConfigurationNode)this.root.node(new Object[]{"whitelist", "players"}))
                    .getList(Object.class, Collections.emptyList()).stream()
                    .map(Object::toString).collect(Collectors.toList());
        } catch (SerializationException e) {
            return Collections.emptyList();
        }
    }

    public void addWhitelistPlayer(String player) {
        List<String> players = new ArrayList<>(getWhitelistPlayers());
        if (!players.contains(player)) {
            players.add(player);
            try {
                this.root.node("whitelist", "players").set(players);
                saveConfig();
            } catch (SerializationException e) {
                logger.error("cant add player to whitelist!", e);
            }
        }
    }

    public boolean removeWhitelistPlayer(String player) {
        List<String> players = new ArrayList<>(getWhitelistPlayers());
        if (players.remove(player)) {
            try {
                this.root.node("whitelist", "players").set(players);
                saveConfig();
                return true;
            } catch (SerializationException e) {
                logger.error("cant remove player from whitelist!", e);
            }
        }
        return false;
    }

    public boolean isPlayerWhitelisted(String username) {
        return getWhitelistPlayers().stream()
                .anyMatch(p -> p.equalsIgnoreCase(username));
    }

    public boolean isQueueEnabled() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"queue", "enabled"})).getBoolean(false);
    }

    public int getQueueMaxPlayers() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"queue", "max-players"})).getInt(100);
    }

    public String getQueueServer() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"queue", "queue-server"})).getString("queue");
    }

    public String getQueueTargetServer() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"queue", "target-server"})).getString("lobby");
    }

    public String getQueueMessage() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"queue", "queue-message"}))
                .getString("&eYou are in queue... Position: &6{position}&e/&6{total}");
    }

    public String getQueueBypassPermission() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"queue", "bypass-permission"})).getString("proxyflow.queue.bypass");
    }

    public Map<String, Integer> getQueuePriorityPermissions() {
        Map<String, Integer> priorities = new HashMap<>();
        CommentedConfigurationNode priorityNode = (CommentedConfigurationNode)this.root.node(new Object[]{"queue", "priority-permissions"});

        for (Object key : priorityNode.childrenMap().keySet()) {
            String permission = key.toString();
            int priority = priorityNode.node(key).getInt(0);
            priorities.put(permission, priority);
        }

        return priorities;
    }

    public String getVpnBypassPermission() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "vpn-check", "bypass-permission"})).getString("proxyflow.security.vpn.bypass");
    }

    public boolean isVpnCheckEnabled() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "vpn-check", "enabled"})).getBoolean(true);
    }

    public String getVpnCheckApiKey() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "vpn-check", "api-key"})).getString("");
    }

    public List<String> getVpnWhitelistedPlayers() {
        try {
            return (List)((CommentedConfigurationNode)this.root.node(new Object[]{"security", "vpn-check", "whitelisted-players"}))
                    .getList(Object.class, Collections.emptyList()).stream()
                    .map(Object::toString).collect(Collectors.toList());
        } catch (SerializationException e) {
            return Collections.emptyList();
        }
    }

    public boolean isPlayerVpnWhitelisted(String username) {
        return getVpnWhitelistedPlayers().stream()
                .anyMatch(p -> p.equalsIgnoreCase(username));
    }

    public boolean isCountryBlockEnabled() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "country-block", "enabled"})).getBoolean(false);
    }

    public String getCountryBlockMode() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "country-block", "mode"})).getString("blacklist");
    }

    public List<String> getCountryList() {
        try {
            return (List)((CommentedConfigurationNode)this.root.node(new Object[]{"security", "country-block", "countries"}))
                    .getList(Object.class, Collections.emptyList()).stream()
                    .map(Object::toString).collect(Collectors.toList());
        } catch (SerializationException var2) {
            return Collections.emptyList();
        }
    }

    public boolean isMultiAccountCheckEnabled() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "multi-account", "enabled"})).getBoolean(true);
    }

    public String getMultiAccountBypassPermission() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "multi-account", "bypass-permission"}))
                .getString("proxyflow.security.multiaccount.bypass");
    }

    public String getMultiAccountAction() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "multi-account", "action"})).getString("kick");
    }

    public String getMultiAccountKickMessage() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "multi-account", "kick-message"}))
                .getString("&cA player with your IP address is already on this server");
    }

    public List<String> getMultiAccountWhitelistedPlayers() {
        try {
            return (List)((CommentedConfigurationNode)this.root.node(new Object[]{"security", "multi-account", "whitelisted-players"}))
                    .getList(Object.class, Collections.emptyList()).stream()
                    .map(Object::toString).collect(Collectors.toList());
        } catch (SerializationException e) {
            return Collections.emptyList();
        }
    }

    public boolean isPlayerMultiAccountWhitelisted(String username) {
        return getMultiAccountWhitelistedPlayers().stream()
                .anyMatch(p -> p.equalsIgnoreCase(username));
    }
}