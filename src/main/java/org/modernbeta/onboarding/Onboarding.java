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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.modernbeta.onboarding.commands.AcceptCommand;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Onboarding extends JavaPlugin implements Listener {

    public static Onboarding instance;

    public static List<Player> needToAccept = new ArrayList<>();
    static PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 1, true, false, false);

    private static Connection connection;

    @Override
    public void onEnable() {
        instance = this;

        // Plugin startup logic
        if (!Bukkit.getPluginManager().isPluginEnabled("SuperVanish") && !Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) {
            Bukkit.getLogger().warning("Super/Premium Vanish not installed, disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand acceptCommand = getCommand("accept");
        if (acceptCommand != null) acceptCommand.setExecutor(new AcceptCommand());

        setupDatabase();
        spamRules();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        closeDatabase();
    }

    private void setupDatabase() {
        try {
            // Ensure plugin folder exists
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            // Set up SQLite connection
            String databaseUrl = "jdbc:sqlite:" + getDataFolder() + "/onboarding.db";
            connection = DriverManager.getConnection(databaseUrl);
            getLogger().info("Connected to database at: " + databaseUrl);

            // Create table if not exists
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS need_to_accept (uuid TEXT PRIMARY KEY)"
            )) {
                statement.executeUpdate();
                getLogger().info("Database setup complete.");
            }
        } catch (SQLException e) {
            getLogger().severe("Database setup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeDatabase() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                getLogger().severe("Failed to close database: " + e.getMessage());
            }
        }
    }

    private void addPlayerToDatabase(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO need_to_accept (uuid) VALUES (?)"
        )) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("Failed to add player to database: " + e.getMessage());
        }
    }

    private void removePlayerFromDatabase(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM need_to_accept WHERE uuid = ?"
        )) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            getLogger().severe("Failed to remove player from database: " + e.getMessage());
        }
    }

    private boolean isPlayerInDatabase(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM need_to_accept WHERE uuid = ?"
        )) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to query database: " + e.getMessage());
            return false;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore() || isOnboarding(player)) {
            startOnboardingProcess(player);
        } else {
            removeOnboardingEffects(player);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (needToAccept.contains(player)) {
            needToAccept.remove(player);
            addPlayerToDatabase(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerMovement(PlayerMoveEvent event) {
        if (isOnboarding(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMessage(AsyncPlayerChatEvent event) {
        if (isOnboarding(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    boolean isOnboarding(Player player) {
        return !player.hasPermission("onboarding.ignore") && (needToAccept.contains(player) || isPlayerInDatabase(player.getUniqueId()));
    }

    public void startOnboardingProcess(Player player) {
        needToAccept.add(player);
        removePlayerFromDatabase(player.getUniqueId());
        VanishAPI.getPlugin().getVisibilityChanger().hidePlayer(player, player.getName(), true);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(blindness);
        player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ACCEPT RULES IN CHAT", ChatColor.RED + "Then you can continue.", 10, 160, 10);
        sendRulesAcceptMessage(player);
    }

    public void acceptOnboardingProcess(Player player) {
        needToAccept.remove(player);
        removeOnboardingEffects(player);
        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Have fun!", ChatColor.GREEN + "All your actions are logged.", 10, 80, 10);
        player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n" +
                ChatColor.GREEN + "" + ChatColor.BOLD + "Thank you for accepting our Rules!\n" +
                ChatColor.GREEN + "Welcome to 2011, enjoy Modern Beta!\n ");
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            otherPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "Welcome " + player.getName() + " to the server!");
        }
    }

    private void removeOnboardingEffects(Player player) {
        removePlayerFromDatabase(player.getUniqueId());
        if (VanishAPI.isInvisible(player))
            VanishAPI.showPlayer(player);
        if (player.hasPotionEffect(PotionEffectType.BLINDNESS))
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        if (player.getGameMode().equals(GameMode.ADVENTURE))
            player.setGameMode(GameMode.SURVIVAL);
    }

    static void sendRulesAcceptMessage(Player player) {
        player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "hacking or xraying");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "destroying other's homes/things (griefing)");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "building above other on-land builds w/o permission");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "stealing from chests that aren't marked as public");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "languages besides english in global chats");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "abusing exploits (except sand/gravel piston dupes)");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "sexual, racist, sexist or homophobic content");
        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✖ NO " + ChatColor.RESET + "war/political/(real)religious conversations/imagery");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ DO " + ChatColor.RESET + "keep real life at the door and enjoy block game ❤");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ DO " + ChatColor.RESET + "use common sense and make some friends!");
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
        }.runTaskTimer(this, 0, 20); // 0 delay, 20 ticks (1 second) period
    }
}
