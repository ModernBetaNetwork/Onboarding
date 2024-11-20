package org.modernbeta.onboarding;

import dev.mzga.beta2releasefixer.Beta2ReleaseFixer;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Rules {

    public static final int RULES_AMOUNT = 9;

    // Sends rules to a CommandSender.
    public static void sendRules(CommandSender sender) {
        if (isLegacyOrConsole(sender)) {
            sendLegacyRules(sender);
        } else {
            sendTranslatedRules(sender);
        }
    }

    // Determines if the sender is using a legacy client or is the console.
    private static boolean isLegacyOrConsole(CommandSender sender) {
        return !(sender instanceof Player player) || Beta2ReleaseFixer.getAuthManager().isBetaPlayer(player.getName());
    }


    // Sends legacy English rules to the sender. (for console/beta clients)
    private static void sendLegacyRules(CommandSender sender) {
        final String NO = ChatColor.RED + "" + ChatColor.BOLD + "NO ";
        final String DO = ChatColor.GREEN + "" + ChatColor.BOLD + "DO ";

        sender.sendMessage(NO + ChatColor.RESET + "hacking or xraying");
        sender.sendMessage(NO + ChatColor.RESET + "destroying other's homes/things");
        sender.sendMessage(NO + ChatColor.RESET + "stealing from chests that aren't marked as public");
        sender.sendMessage(NO + ChatColor.RESET + "languages besides english in global chats");
        sender.sendMessage(NO + ChatColor.RESET + "duplication exploits (except sand/gravel using pistons)");
        sender.sendMessage(NO + ChatColor.RESET + "over sexual comments, racism, sexism, homophobia, etc.");
        sender.sendMessage(NO + ChatColor.RESET + "REAL war/drug/political/religious discussions or builds");
        sender.sendMessage(DO + ChatColor.RESET + "use common sense and make some friends <3");
        sender.sendMessage(DO + ChatColor.RESET + "keep real life at the door and enjoy block game <3");
    }


    // Sends translated rules to the sender.
    private static void sendTranslatedRules(CommandSender sender) {
        for (int i = 1; i <= RULES_AMOUNT; i++) {
            sender.spigot().sendMessage(new TranslatableComponent("modernbeta.rules." + i));
        }
    }
}
