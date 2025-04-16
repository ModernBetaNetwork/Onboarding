package org.modernbeta.onboarding.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.modernbeta.onboarding.Onboarding;

public class DenyCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage("This command can only be performed by a real player.");
            return false;
        }

        if (!Onboarding.needToAccept.contains(senderPlayer.getUniqueId())) {
            sender.sendMessage("You have already accepted the rules or don't need to accept them.");
            return true;
        }

        // Kick the player with a message
        senderPlayer.kickPlayer("You must accept the rules to play on this server. Please rejoin and type /accept when ready.");
        return true;
    }
}