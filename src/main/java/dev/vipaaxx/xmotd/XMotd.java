package dev.vipaaxx.xmotd;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class XMotd extends JavaPlugin implements Listener, TabExecutor {

    private volatile String motd = "";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMotd();

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("xmotd") != null) {
            getCommand("xmotd").setExecutor(this);
            getCommand("xmotd").setTabCompleter(this);
        }
        getLogger().info("xMotd enabled.");
    }

    private void loadMotd() {
        reloadConfig();
        List<String> lines = getConfig().getStringList("motd");
        List<String> parsed = new ArrayList<>();
        for (String line : lines) {
            try {
                parsed.add(MotdParser.parse(line));
            } catch (Exception e) {
                getLogger().warning("Could not parse MOTD line \"" + line + "\": " + e.getMessage());
                parsed.add(line);
            }
        }
        motd = String.join("\n", parsed);
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        event.setMotd(motd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("xmotd.reload") && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                return true;
            }
            loadMotd();
            sender.sendMessage(ChatColor.GREEN + "xMotd reloaded.");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("xmotd.reload")
                && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
