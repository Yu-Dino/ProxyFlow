package net.yuflow.proxyFlow;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.SimpleCommand.Invocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadCommand implements SimpleCommand {
    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void execute(Invocation invocation) {
        this.configManager.loadConfig();
        invocation.source().sendMessage(Component.text("ProxyFlow Konfiguration wurde neu geladen!", NamedTextColor.GREEN));
    }

    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyflow.command.reload");
    }
}
