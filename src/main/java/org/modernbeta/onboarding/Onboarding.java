package org.modernbeta.onboarding;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.textreader.TextInput;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.modernbeta.onboarding.commands.AcceptCommand;
import org.modernbeta.onboarding.commands.DenyCommand;

import java.io.IOException;
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
    Essentials essentials;

    public static List<UUID> needToAccept = new ArrayList<>();
    static PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 1, true, false, false);

    private static Connection connection;
    private static List<String> rules;

    @Override
    public void onEnable() {
        instance = this;

        // Get Essentials plugin instance
        this.essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

        // Register events and commands
        Bukkit.getPluginManager().registerEvents(this, this);

        // Register commands
        getCommand("accept").setExecutor(new AcceptCommand());
        getCommand("deny").setExecutor(new DenyCommand());

        setupDatabase();
        spamRules();
    }

    @Override
    public void onDisable() {
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
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPlayedBefore() || needToAccept.contains(playerUUID) || isPlayerInDatabase(playerUUID)) {
            startOnboardingProcess(player);
        } else if (!player.hasPermission("sv.use")) {
            stopOnboarding(player);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        needToAccept.remove(playerUUID);
    }

    @EventHandler
    public void onPlayerMovement(PlayerMoveEvent event) {
        if (needToAccept.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMessage(AsyncPlayerChatEvent event) {
        if (needToAccept.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public void startOnboardingProcess(Player player) {
        UUID playerUUID = player.getUniqueId();
        needToAccept.add(playerUUID);
        addPlayerToDatabase(playerUUID);
        VanishAPI.getPlugin().getVisibilityChanger().hidePlayer(player, player.getName(), true);
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(blindness);
        player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ACCEPT OR DENY RULES", ChatColor.RED + "Type /accept to play or /deny to leave.", 10, 160, 10);

        // Ensure rules are always up-to-date for new players
        reloadRules(essentials);

        sendRulesAcceptMessage(player);
    }

    public void acceptOnboardingProcess(Player player) {
        stopOnboarding(player);
        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Have fun!", ChatColor.GREEN + "All your actions are logged.", 10, 80, 10);
        player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n" +
                ChatColor.GREEN + "" + ChatColor.BOLD + "Thank you for accepting our Rules!\n" +
                ChatColor.GREEN + "Welcome to 2011, enjoy Modern Beta!\n ");
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            otherPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "Welcome " + player.getName() + " to the server!");
        }
    }

    private void stopOnboarding(Player player) {
        UUID playerUUID = player.getUniqueId();
        needToAccept.remove(playerUUID);
        removePlayerFromDatabase(playerUUID);
        if (VanishAPI.isInvisible(player))
            VanishAPI.showPlayer(player);
        if (player.hasPotionEffect(PotionEffectType.BLINDNESS))
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        if (player.getGameMode().equals(GameMode.ADVENTURE))
            player.setGameMode(GameMode.SURVIVAL);
    }

    static void sendRulesAcceptMessage(Player player) {
        player.sendMessage("\n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n");
        for(String rule : rules) {
            player.sendMessage(rule);
        }
        player.sendMessage("\n" + ChatColor.RED + "Enter " + ChatColor.BOLD + "/accept" + ChatColor.RED + " to agree to these rules.");
        player.sendMessage(ChatColor.RED + "Or enter " + ChatColor.BOLD + "/deny" + ChatColor.RED + " if you do not agree (this will kick you from the server).");
    }

    private void spamRules() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (needToAccept.isEmpty()) return;
                for (UUID playerUUID : needToAccept) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        sendRulesAcceptMessage(player);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // 0 delay, 20 ticks (1 second) period
    }

    private static void reloadRules(Essentials essentials) {
        Bukkit.getLogger().info("Reloading rules...");
        try {
            final TextInput ruleText =
                new TextInput(new CommandSource(Bukkit.getServer().getConsoleSender()), "rules", true, essentials);
            rules = ruleText.getLines();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
