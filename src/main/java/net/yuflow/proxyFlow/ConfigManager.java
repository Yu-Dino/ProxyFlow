package net.yuflow.proxyFlow;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
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
                        logger.error("Die Standard-Konfigurationsdatei 'config.yml' konnte nicht in den Ressourcen gefunden werden!");
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
                logger.error("Konnte die Standard-Konfigurationsdatei nicht erstellen!", var9);
            }
        }

        this.loadConfig();
    }

    public void loadConfig() {
        try {
            this.root = (CommentedConfigurationNode)this.loader.load();
        } catch (ConfigurateException var2) {
            this.logger.error("Fehler beim Laden der Konfiguration!", var2);
        }

    }

    public void saveConfig() {
        try {
            this.loader.save(this.root);
        } catch (ConfigurateException e) {
            this.logger.error("Fehler beim Speichern der Konfiguration!", e);
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
            logger.error("Konnte den Wartungsstatus nicht in die Konfiguration schreiben!", e);
        }
    }

    public String getMaintenanceMotd() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"maintenance", "motd"})).getString("&cServer is in maintenance!");
    }

    public String getMaintenanceKickMessage() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"maintenance", "kick-message"})).getString("&cDer Server befindet sich im Wartungsmodus.");
    }

    public String getMaintenanceBypassPermission() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"maintenance", "bypass-permission"})).getString("proxyflow.maintenance.bypass");
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

    public boolean isCountryBlockEnabled() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "country-block", "enabled"})).getBoolean(false);
    }

    public String getCountryBlockMode() {
        return ((CommentedConfigurationNode)this.root.node(new Object[]{"security", "country-block", "mode"})).getString("blacklist");
    }

    public List<String> getCountryList() {
        try {
            return (List)((CommentedConfigurationNode)this.root.node(new Object[]{"security", "country-block", "countries"})).getList(Object.class, Collections.emptyList()).stream().map(Object::toString).collect(Collectors.toList());
        } catch (SerializationException var2) {
            return Collections.emptyList();
        }
    }
}