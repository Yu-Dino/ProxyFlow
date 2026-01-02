package net.yuflow.proxyFlow.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.yuflow.proxyFlow.config.ConfigManager;

public class ReloadCommand implements SimpleCommand {
    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        this.configManager.loadConfig();
        invocation.source().sendMessage(Component.text("ProxyFlow configuration reloaded!", NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyflow.command.reload");
    }
}