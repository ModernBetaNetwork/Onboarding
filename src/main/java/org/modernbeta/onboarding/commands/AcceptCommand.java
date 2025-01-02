package org.modernbeta.onboarding.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.modernbeta.onboarding.Onboarding;

public class AcceptCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player senderPlayer)) {
            sender.sendMessage("This command can only be performed by a real player.");
            return false;
        }

        if (!Onboarding.needToAccept.contains(senderPlayer.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You have already accepted the rules, if you need a refresher, read /rules <3");
            return true;
        }

        Onboarding.instance.acceptOnboardingProcess(senderPlayer);
        return true;
    }

}
