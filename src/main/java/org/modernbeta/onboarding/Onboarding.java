package org.modernbeta.onboarding;

import de.myzelyam.api.vanish.VanishAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.modernbeta.onboarding.commands.AcceptCommand;
import org.modernbeta.onboarding.commands.RulesCommand;

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
        PluginCommand rulesCommand = this.getCommand("rules");
        if (rulesCommand != null) rulesCommand.setExecutor(new RulesCommand());

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
    public void onPlayerMessage(AsyncPlayerChatEvent event) {
        if (isOnboarding(event.getPlayer())) {
            event.setCancelled(true);
        }
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
        VanishAPI.getPlugin().getVisibilityChanger().hidePlayer(player, player.getName(), true);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(blindness);
        player.sendTitle(new TranslatableComponent("modernbeta.onboarding.take_action.title").toString(), new TranslatableComponent("modernbeta.onboarding.take_action.subtitle").toString(), 10, 160, 10);
        Rules.sendRules(player);
    }

    public static void acceptOnboardingProcess(Player player) {
        needToAccept.remove(player);
        needToAcceptOffline.remove(player.getUniqueId());
        VanishAPI.showPlayer(player);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setGameMode(GameMode.SURVIVAL);
        TranslatableComponent title1Component = new TranslatableComponent("modernbeta.onboarding.success.title");
        TranslatableComponent title2Component = new TranslatableComponent("modernbeta.onboarding.success.subtitle");

        String title = title1Component.toLegacyText();
        String subtitle = title2Component.toLegacyText();

        player.sendTitle(title, subtitle, 10, 80, 10);

        player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n");
        player.spigot().sendMessage(new TranslatableComponent("modernbeta.onboarding.success.chat"));

        TranslatableComponent welcomeMessage = new TranslatableComponent("modernbeta.welcome_message");
        welcomeMessage.addWith(player.getName());
        welcomeMessage.setColor(ChatColor.LIGHT_PURPLE);

        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            otherPlayer.spigot().sendMessage(welcomeMessage);
        }
    }

    private void spamRules() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (needToAccept.isEmpty()) return;
                for (Player player : needToAccept) {
                    player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n");
                    Rules.sendRules(player);
                }
            }
        }.runTaskTimer(this, 0, 10); // 0 delay, 20 ticks (1 second) period
    }
}
