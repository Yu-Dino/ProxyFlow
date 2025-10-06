package net.yuflow.proxyFlow;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(
        id = "proxyflow",
        name = "ProxyFlow",
        version = "1.1.2",
        description = "Ein Sicherheitsplugin für deinen Proxy!",
        authors = {"Yu_Dino"}
)
public class plugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ConfigManager configManager;

    @Inject
    public plugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(this.logger, this.dataDirectory);

        CommandManager commandManager = this.server.getCommandManager();
        CommandMeta reloadMeta = commandManager.metaBuilder("proxyflow").aliases("pf").build();
        commandManager.register(reloadMeta, new ReloadCommand(this.configManager));

        CommandMeta maintenanceMeta = commandManager.metaBuilder("maintenance").aliases("maintenance").build();
        commandManager.register(maintenanceMeta, new MaintenanceCommand(this.configManager, this.server));

        this.server.getEventManager().register(this, new ConnectionListener(this.server, this.logger, this.configManager));
        this.server.getEventManager().register(this, new MaintenanceListener(this.configManager));

        this.logger.info("                                                                                                              *+++++++++++");
        this.logger.info(" _______  ______    _______  __   __  __   __  _______  ___      _______  _     _    __   __  ____          ++++++++++  +++*#########################");
        this.logger.info("|       ||    _ |  |       ||  |_|  ||  | |  ||       ||   |    |       || | _ | |  |  | |  ||    |        ++++++++++  ++++++*************************");
        this.logger.info("|    _  ||   | ||  |   _   ||       ||  |_|  ||    ___||   |    |   _   || || || |  |  |_|  | |   |       ++++++++++  ++++++++**********");
        this.logger.info("|   |_| ||   |_||_ |  | |  ||       ||       ||   |___ |   |    |  | |  ||       |  |       | |   |      +++++++++*  +++++++++++****************** ***");
        this.logger.info("|    ___||    __  ||  |_|  | |     | |_     _||    ___||   |___ |  |_|  ||       |  |       | |   |     +++          ++++++++++*****");
        this.logger.info("|   |    |   |  | ||       ||   _   |  |   |  |   |    |       ||       ||   _   |   |     |  |   |     +++*++++++++  ++++++++****************************");
        this.logger.info("|___|    |___|  |_||_______||__| |__|  |___|  |___|    |_______||_______||__| |__|    |___|   |___|      ++++++++++++   +++++*****************************");
        this.logger.info("                                                                                                          *+++++++++++*  +++****************");
        this.logger.info("                  ProxyFlow-Velocity v1.1.2 powered by Yu_Dino                                              +++++++++++++++*************************");
        this.logger.info("                         Plugin got fully initialised!                                                        ++++++++++++");
        this.logger.info("                                                                                                                +");
    }
}