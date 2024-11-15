package org.modernbeta.onboarding;

import de.myzelyam.api.vanish.VanishAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.modernbeta.onboarding.commands.AcceptCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Onboarding extends JavaPlugin implements Listener {

    public static List<Player> needToAccept = new ArrayList<>();
    public static List<UUID> needToAcceptOffline = new ArrayList<>();
    static PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 1, true, false, false);

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!Bukkit.getPluginManager().isPluginEnabled("SuperVanish") && !Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
            Bukkit.getLogger().warning("Super/Premium Vanish not installed, disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand acceptCommand = getCommand("accept");
        if (acceptCommand != null) acceptCommand.setExecutor(new AcceptCommand());

        spamRules();
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        // get player and check if this is their first time joining
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore() || isOnboarding(player))
            startOnboardingProcess(player);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!needToAccept.contains(player)) return;

        needToAccept.remove(player);
        needToAcceptOffline.add(player.getUniqueId());
    }


    @EventHandler
    public void onPlayerMovement(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isOnboarding(player)) event.setCancelled(true);
    }

    boolean isOnboarding(Player player) {
        return !player.hasPermission("onboarding.ignore") && (needToAccept.contains(player) || needToAcceptOffline.contains(player.getUniqueId()));
    }

    public static void startOnboardingProcess(Player player) {
        needToAccept.add(player);
        needToAcceptOffline.remove(player.getUniqueId());
        VanishAPI.hidePlayer(player, player.getName(), true);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(blindness);
        player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ACCEPT RULES IN CHAT", ChatColor.RED + "Then you can continue.", 10, 160, 10);
        sendRulesAcceptMessage(player);
    }

    public static void acceptOnboardingProcess(Player player) {
        needToAccept.remove(player);
        needToAcceptOffline.remove(player.getUniqueId());
        VanishAPI.showPlayer(player);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setGameMode(GameMode.SURVIVAL);
        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Have fun!", ChatColor.GREEN + "All your actions are logged.", 10, 80, 10);
        player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n" +
                ChatColor.GREEN + "" + ChatColor.BOLD + "Thank you for accepting our Rules!\n" +
                ChatColor.GREEN + "Welcome to 2011, enjoy Modern Beta!\n ");
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            otherPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "Welcome " + player.getName() + " to the server!");
        }
    }

    static void sendRulesAcceptMessage(Player player) {
        player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "hacking or xraying");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "destroying other's homes/things (griefing)");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "building above other on-land builds w/o permission.");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "stealing from chests that aren't marked as public.");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "languages besides english in global chats");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "duplication exploits (except sand/gravel using pistons)");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "political or (real)religious conversations or builds.");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ DO " + ChatColor.RESET + "keep real life at the door and enjoy block game ❤");
        player.sendMessage("\n" + ChatColor.RED + "Enter " + ChatColor.BOLD + "/accept" + ChatColor.RED + " to agree to these rules.");
    }

    private void spamRules() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (needToAccept.isEmpty()) return;
                for (Player player : needToAccept) {
                    sendRulesAcceptMessage(player);
                }
            }
        }.runTaskTimer(this, 0, 10); // 0 delay, 20 ticks (1 second) period
    }
}
