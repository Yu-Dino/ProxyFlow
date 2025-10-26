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
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

@Plugin(
        id = "proxyflow",
        name = "ProxyFlow",
        version = "1.3.0",
        description = "Ein Sicherheitsplugin für deinen Proxy!",
        authors = {"Yu_Dino"}
)
public class plugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ConfigManager configManager;
    private QueueManager queueManager;

    @Inject
    public plugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(this.logger, this.dataDirectory);
        this.queueManager = new QueueManager(this.server, this.logger, this.configManager);

        CommandManager commandManager = this.server.getCommandManager();

        CommandMeta reloadMeta = commandManager.metaBuilder("proxyflow").aliases("pf").build();
        commandManager.register(reloadMeta, new ReloadCommand(this.configManager));

        CommandMeta maintenanceMeta = commandManager.metaBuilder("maintenance").build();
        commandManager.register(maintenanceMeta, new MaintenanceCommand(this.configManager, this.server));

        CommandMeta whitelistMeta = commandManager.metaBuilder("whitelist").aliases("wl").build();
        commandManager.register(whitelistMeta, new WhitelistCommand(this.configManager));

        CommandMeta securityMeta = commandManager.metaBuilder("security").aliases("sec").build();
        commandManager.register(securityMeta, new SecurityCommand(this.configManager));

        this.server.getEventManager().register(this, new ConnectionListener(this.server, this.logger, this.configManager));
        this.server.getEventManager().register(this, new MaintenanceListener(this.configManager));
        this.server.getEventManager().register(this, new QueueListener(this.server, this.logger, this.configManager, this.queueManager));

        this.server.getScheduler().buildTask(this, () -> {
            if (configManager.isQueueEnabled()) {
                queueManager.processQueue();
            }
        }).repeat(2L, TimeUnit.SECONDS).schedule();

        this.logger.info("                                                                                                              *+++++++++++");
        this.logger.info(" _______  ______    _______  __   __  __   __  _______  ___      _______  _     _    __   __  ____          ++++++++++  +++*#########################");
        this.logger.info("|       ||    _ |  |       ||  |_|  ||  | |  ||       ||   |    |       || | _ | |  |  | |  ||    |        ++++++++++  ++++++*************************");
        this.logger.info("|    _  ||   | ||  |   _   ||       ||  |_|  ||    ___||   |    |   _   || || || |  |  |_|  | |   |       ++++++++++  ++++++++**********");
        this.logger.info("|   |_| ||   |_||_ |  | |  ||       ||       ||   |___ |   |    |  | |  ||       |  |       | |   |      +++++++++*  +++++++++++****************** ***");
        this.logger.info("|    ___||    __  ||  |_|  | |     | |_     _||    ___||   |___ |  |_|  ||       |  |       | |   |     +++          ++++++++++*****");
        this.logger.info("|   |    |   |  | ||       ||   _   |  |   |  |   |    |       ||       ||   _   |   |     |  |   |     +++*++++++++  ++++++++****************************");
        this.logger.info("|___|    |___|  |_||_______||__| |__|  |___|  |___|    |_______||_______||__| |__|    |___|   |___|      ++++++++++++   +++++*****************************");
        this.logger.info("                                                                                                          *+++++++++++*  +++****************");
        this.logger.info("                  ProxyFlow-Velocity v1.3.0 powered by Yu_Dino                                              +++++++++++++++*************************");
        this.logger.info("                         Plugin got fully initialised!                                                        ++++++++++++");
    }
}