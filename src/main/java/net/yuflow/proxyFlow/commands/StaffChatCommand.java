package net.yuflow.proxyFlow.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.yuflow.proxyFlow.managers.StaffChatManager;
import java.util.Arrays;

public class StaffChatCommand implements SimpleCommand {
    private final StaffChatManager staffChatManager;

    public StaffChatCommand(StaffChatManager staffChatManager) {
        this.staffChatManager = staffChatManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("Console cannot use staffchat toggle.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            boolean state = staffChatManager.toggle(player.getUniqueId());
            if (state) {
                player.sendMessage(Component.text("StaffChat toggled ON.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("StaffChat toggled OFF.", NamedTextColor.RED));
            }
        } else {
            String message = String.join(" ", Arrays.asList(args));
            staffChatManager.broadcast(player, message);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("proxyflow.staffchat");
    }
}