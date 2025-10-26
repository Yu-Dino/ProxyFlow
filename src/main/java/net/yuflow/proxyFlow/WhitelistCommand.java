package net.yuflow.proxyFlow;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.ArrayList;

public class WhitelistCommand implements SimpleCommand {
    private final ConfigManager configManager;

    public WhitelistCommand(ConfigManager configManager) {
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
            case "on":
                configManager.setWhitelistEnabled(true);
                invocation.source().sendMessage(Component.text("Whitelist enabled!", NamedTextColor.GREEN));
                break;

            case "off":
                configManager.setWhitelistEnabled(false);
                invocation.source().sendMessage(Component.text("Whitelist disabled!", NamedTextColor.RED));
                break;

            case "add":
                if (args.length < 2) {
                    invocation.source().sendMessage(Component.text("Usage: /whitelist add <player>", NamedTextColor.RED));
                    return;
                }
                configManager.addWhitelistPlayer(args[1]);
                invocation.source().sendMessage(Component.text("Player " + args[1] + " added to whitelist!", NamedTextColor.GREEN));
                break;

            case "remove":
                if (args.length < 2) {
                    invocation.source().sendMessage(Component.text("Usage: /whitelist remove <player>", NamedTextColor.RED));
                    return;
                }
                if (configManager.removeWhitelistPlayer(args[1])) {
                    invocation.source().sendMessage(Component.text("Player " + args[1] + " removed from whitelist!", NamedTextColor.GREEN));
                } else {
                    invocation.source().sendMessage(Component.text("Player " + args[1] + " is not on the whitelist!", NamedTextColor.RED));
                }
                break;

            case "list":
                List<String> players = configManager.getWhitelistPlayers();
                if (players.isEmpty()) {
                    invocation.source().sendMessage(Component.text("Whitelist is empty!", NamedTextColor.YELLOW));
                } else {
                    invocation.source().sendMessage(Component.text("Whitelisted players:", NamedTextColor.GREEN));
                    for (String player : players) {
                        invocation.source().sendMessage(Component.text("- " + player, NamedTextColor.GRAY));
                    }
                }
                break;

            default:
                sendHelp(invocation);
                break;
        }
    }

    private void sendHelp(Invocation invocation) {
        invocation.source().sendMessage(Component.text("=== Whitelist Commands ===", NamedTextColor.GOLD));
        invocation.source().sendMessage(Component.text("/whitelist on - Enable whitelist", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/whitelist off - Disable whitelist", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/whitelist add <player> - Add player", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/whitelist remove <player> - Remove player", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/whitelist list - List players", NamedTextColor.YELLOW));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyflow.command.whitelist");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            suggestions.add("on");
            suggestions.add("off");
            suggestions.add("add");
            suggestions.add("remove");
            suggestions.add("list");
        }

        return suggestions;
    }
}