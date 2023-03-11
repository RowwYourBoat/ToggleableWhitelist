package me.rowanscripts.customwhitelist;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public final class CustomWhitelist extends JavaPlugin implements Listener {

    public static CustomWhitelist plugin;
    public static boolean joiningAllowed;

    private File whitelistFile;
    private YamlConfiguration whitelistYaml;

    @Override
    public void onEnable() {
        plugin = this;
        if (Bukkit.hasWhitelist()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Please disable the vanilla whitelist, as it will break the plugin!");
            plugin.setEnabled(false);
        }
        whitelistFile = new File(plugin.getDataFolder(), "settings.yml");
        whitelistYaml = YamlConfiguration.loadConfiguration(whitelistFile);
        if (!whitelistFile.exists()) {
            whitelistYaml.set("closed_message", "The server is currently closed to the public!");
            whitelistYaml.set("not_whitelisted_message", "You're not on the whitelist!");
            whitelistYaml.set("open_by_default", false);
            save();
        }
        joiningAllowed = whitelistYaml.getBoolean("open_by_default");

        Bukkit.getPluginCommand("togglejoining").setExecutor(new mainCommandExecutor());
        Bukkit.getPluginCommand("reloadwhitelist").setExecutor(new mainCommandExecutor());
        Bukkit.getPluginManager().registerEvents(plugin, plugin);
    }

    public void reloadSettings() {
        try {
            whitelistYaml.load(whitelistFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            whitelistYaml.save(whitelistFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void checkWhitelistOnLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("whitelist.bypass"))
            return;

        Set<OfflinePlayer> whitelistedPlayers = Bukkit.getWhitelistedPlayers();
        if (!whitelistedPlayers.contains(Bukkit.getOfflinePlayer(player.getUniqueId()))) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.RED + whitelistYaml.getString("not_whitelisted_message"));
            return;
        }

        if (!joiningAllowed) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.RED + whitelistYaml.getString("closed_message"));
        }
    }

    public class mainCommandExecutor implements CommandExecutor {

        @Override
        public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, String label, String[] args) {
            if (label.equalsIgnoreCase("togglejoining")) {
                CustomWhitelist.joiningAllowed = !CustomWhitelist.joiningAllowed;
                if (CustomWhitelist.joiningAllowed)
                    sender.sendMessage(ChatColor.DARK_GREEN + "Whitelisted players will now be able to join!");
                else
                    sender.sendMessage(ChatColor.RED + "Whitelisted players will now no longer be able to join!");
            } else if (label.equalsIgnoreCase("reloadwhitelist")) {
                CustomWhitelist.plugin.reloadSettings();
                sender.sendMessage(ChatColor.DARK_GREEN + "You've loaded the settings file into the server!");
            }
            return true;
        }

    }

}