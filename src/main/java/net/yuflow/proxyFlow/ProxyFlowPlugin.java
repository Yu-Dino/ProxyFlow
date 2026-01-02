package net.yuflow.proxyFlow;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.yuflow.proxyFlow.commands.*;
import net.yuflow.proxyFlow.config.ConfigManager;
import net.yuflow.proxyFlow.listeners.ConnectionListener;
import net.yuflow.proxyFlow.listeners.MaintenanceListener;
import net.yuflow.proxyFlow.listeners.QueueListener;
import net.yuflow.proxyFlow.managers.QueueManager;
import net.yuflow.proxyFlow.managers.StaffChatManager;
import net.yuflow.proxyFlow.services.DatabaseManager;
import net.yuflow.proxyFlow.services.DiscordService;
import net.yuflow.proxyFlow.services.SecurityService;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "proxyflow",
        name = "ProxyFlow",
        version = "1.4.0",
        description = "Advanced Security & Utility Plugin",
        authors = {"Yu_Dino"}
)
public class ProxyFlowPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private QueueManager queueManager;
    private DatabaseManager databaseManager;
    private SecurityService securityService;
    private DiscordService discordService;
    private StaffChatManager staffChatManager;

    @Inject
    public ProxyFlowPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configManager = new ConfigManager(this.logger, this.dataDirectory);

        this.databaseManager = new DatabaseManager(this.logger, this.dataDirectory);
        this.discordService = new DiscordService(this.configManager, this.logger);
        this.securityService = new SecurityService(this.configManager, this.logger);
        this.staffChatManager = new StaffChatManager(this.server, this.configManager);
        this.queueManager = new QueueManager(this.server, this.logger, this.configManager);

        CommandManager cm = this.server.getCommandManager();
        cm.register(cm.metaBuilder("proxyflow").aliases("pf").build(), new ReloadCommand(this.configManager));
        cm.register(cm.metaBuilder("maintenance").build(), new MaintenanceCommand(this.configManager, this.server, this.discordService));
        cm.register(cm.metaBuilder("whitelist").aliases("wl").build(), new WhitelistCommand(this.configManager));
        cm.register(cm.metaBuilder("security").aliases("sec").build(), new SecurityCommand(this.configManager));
        cm.register(cm.metaBuilder("staffchat").aliases("sc").build(), new StaffChatCommand(this.staffChatManager));

        this.server.getEventManager().register(this, new ConnectionListener(
                this.server, this.logger, this.configManager, this.databaseManager,
                this.securityService, this.discordService, this.staffChatManager));

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
        this.logger.info("|   |_| ||   |_||_ |  | |  ||       ||       ||   |___ |   |    |  | |  ||       |  |       | |   |      +++++++++* +++++++++++****************** ***");
        this.logger.info("|    ___||    __  ||  |_|  | |     | |_     _||    ___||   |___ |  |_|  ||       |  |       | |   |     +++          ++++++++++*****");
        this.logger.info("|   |    |   |  | ||       ||   _   |  |   |  |   |    |       ||       ||   _   |   |     |  |   |     +++*++++++++  ++++++++****************************");
        this.logger.info("|___|    |___|  |_||_______||__| |__|  |___|  |___|    |_______||_______||__| |__|    |___|   |___|      ++++++++++++   +++++*****************************");
        this.logger.info("                                                                                                          *+++++++++++* +++****************");
        this.logger.info("                          ProxyFlow-Velocity v1.4.0 by Yu_Dino                                              +++++++++++++++*************************");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
    }
}