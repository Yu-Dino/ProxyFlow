package net.yuflow.proxyFlow.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.yuflow.proxyFlow.config.ConfigManager;
import java.util.ArrayList;
import java.util.List;

public class SecurityCommand implements SimpleCommand {
    private final ConfigManager configManager;

    public SecurityCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(invocation);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "blockserverips":
                if (args.length < 2) {
                    boolean current = configManager.isBlockServerIps();
                    invocation.source().sendMessage(Component.text("Block Server IPs is currently: " + (current ? "enabled" : "disabled"), NamedTextColor.YELLOW));
                    return;
                }
                if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true")) {
                    configManager.setBlockServerIps(true);
                    invocation.source().sendMessage(Component.text("Block Server IPs enabled!", NamedTextColor.GREEN));
                } else if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("false")) {
                    configManager.setBlockServerIps(false);
                    invocation.source().sendMessage(Component.text("Block Server IPs disabled!", NamedTextColor.RED));
                }
                break;
            case "notify":
            case "notifyadmins":
                if (args.length < 2) {
                    boolean current = configManager.isNotifyAdmins();
                    invocation.source().sendMessage(Component.text("Admin notifications are currently: " + (current ? "enabled" : "disabled"), NamedTextColor.YELLOW));
                    return;
                }
                if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true")) {
                    configManager.setNotifyAdmins(true);
                    invocation.source().sendMessage(Component.text("Admin notifications enabled!", NamedTextColor.GREEN));
                } else if (args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("false")) {
                    configManager.setNotifyAdmins(false);
                    invocation.source().sendMessage(Component.text("Admin notifications disabled!", NamedTextColor.RED));
                }
                break;
            case "status":
                invocation.source().sendMessage(Component.text("=== ProxyFlow Security Status ===", NamedTextColor.GOLD));
                invocation.source().sendMessage(Component.text("VPN Check: " + (configManager.isVpnCheckEnabled() ? "enabled" : "disabled"), NamedTextColor.YELLOW));
                invocation.source().sendMessage(Component.text("Block Server IPs: " + (configManager.isBlockServerIps() ? "enabled" : "disabled"), NamedTextColor.YELLOW));
                invocation.source().sendMessage(Component.text("Admin Notifications: " + (configManager.isNotifyAdmins() ? "enabled" : "disabled"), NamedTextColor.YELLOW));
                invocation.source().sendMessage(Component.text("Multi-Account Check: " + (configManager.isMultiAccountCheckEnabled() ? "enabled" : "disabled"), NamedTextColor.YELLOW));
                invocation.source().sendMessage(Component.text("Country Block: " + (configManager.isCountryBlockEnabled() ? "enabled" : "disabled"), NamedTextColor.YELLOW));
                break;
            default:
                sendHelp(invocation);
                break;
        }
    }

    private void sendHelp(Invocation invocation) {
        invocation.source().sendMessage(Component.text("=== Security Commands ===", NamedTextColor.GOLD));
        invocation.source().sendMessage(Component.text("/security blockserverips <on|off> - Toggle server IP blocking", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/security notify <on|off> - Toggle admin notifications", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/security status - Show security status", NamedTextColor.YELLOW));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyflow.command.security");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();
        if (args.length == 0 || args.length == 1) {
            suggestions.add("blockserverips"); suggestions.add("notify"); suggestions.add("status");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("blockserverips") || args[0].equalsIgnoreCase("notify"))) {
            suggestions.add("on"); suggestions.add("off");
        }
        return suggestions;
    }
}