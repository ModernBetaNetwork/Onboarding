package org.modernbeta.onboarding.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.modernbeta.onboarding.Rules;

public class RulesCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        Rules.sendRules(commandSender);
        return true;

    }

}
