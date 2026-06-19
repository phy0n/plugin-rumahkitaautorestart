package dev.rumahkita.autorestart;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public final class AutoRestartPlugin extends JavaPlugin implements CommandExecutor {

    private int timeLeft;
    private BukkitRunnable countdownTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startCountdown();
        getCommand("autorestart").setExecutor(this);
        getLogger().info("RumahKita AutoRestart system has been enabled!");
    }

    @Override
    public void onDisable() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        getLogger().info("RumahKita AutoRestart system has been disabled.");
    }

    private void startCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }

        timeLeft = getConfig().getInt("restart-time-seconds", 43200); 
        List<Integer> warnTimes = getConfig().getIntegerList("warning-times-seconds");
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    Bukkit.broadcastMessage(color(getConfig().getString("messages.restarting", "&cServer is restarting now!")));

                    String kickMsg = color(getConfig().getString("messages.kick-message", "&cServer is restarting!"));
                    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                        p.kickPlayer(kickMsg);
                    }

                    if (getConfig().getBoolean("sounds.enabled", true)) {
                        try {
                            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(getConfig().getString("sounds.restart-sound", "ENTITY_ENDER_DRAGON_GROWL"));
                            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                            }
                        } catch (Exception ignored) {}
                    }

                    String commandToRun = getConfig().getString("restart-command", "restart");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                    
                    this.cancel();
                    return;
                }
                
                if (warnTimes.contains(timeLeft)) {
                    String message = getConfig().getString("messages.warning", "&eServer will restart in &c%time%&e!");
                    message = message.replace("%time%", formatTime(timeLeft));
                    Bukkit.broadcastMessage(color(message));

                    if (getConfig().getBoolean("title-alerts.enabled", true)) {
                        String title = color(getConfig().getString("title-alerts.title", "&c&lSERVER RESTART"));
                        String subtitle = color(getConfig().getString("title-alerts.subtitle", "&fServer will restart in &e%time%&f!")).replace("%time%", formatTime(timeLeft));
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            p.sendTitle(title, subtitle, 10, 70, 20);
                        }
                    }

                    if (getConfig().getBoolean("sounds.enabled", true)) {
                        try {
                            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(getConfig().getString("sounds.warning-sound", "BLOCK_NOTE_BLOCK_PLING"));
                            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (getConfig().getBoolean("action-bar-countdown", true) && timeLeft <= getConfig().getInt("action-bar-starts-at", 10)) {
                    String actionMsg = color("&#FF3333&lRestarting &8» &#FFC000" + timeLeft + "s");
                    net.md_5.bungee.api.chat.TextComponent tc = new net.md_5.bungee.api.chat.TextComponent(actionMsg);
                    for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, tc);
                    }
                }

                timeLeft--;
            }
        };
        countdownTask.runTaskTimer(this, 20L, 20L); 
    }

    private String formatTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append(h == 1 ? " hour " : " hours ");
        if (m > 0) sb.append(m).append(m == 1 ? " minute " : " minutes ");
        if (s > 0) sb.append(s).append(s == 1 ? " second" : " seconds");
        
        return sb.toString().trim();
    }
    
    private String color(String msg) {
        if (msg == null) return "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#[a-fA-F0-9]{6}");
        java.util.regex.Matcher matcher = pattern.matcher(msg);
        while (matcher.find()) {
            String color = msg.substring(matcher.start(), matcher.end());
            msg = msg.replace(color, net.md_5.bungee.api.ChatColor.of(color.replace("&", "")) + "");
            matcher = pattern.matcher(msg);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("autorestart.admin")) {
            sender.sendMessage(color("&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /autorestart [time | reload | now <seconds>]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("time")) {
            sender.sendMessage(color("&#FF3333&lRUMAH KITA &8| &7Time before restart: &#FFC000" + formatTime(timeLeft)));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            startCountdown(); 
            sender.sendMessage(color("&#FF3333&lRUMAH KITA &8| &#33FF33Config reloaded & timer reset!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("now")) {
            if (args.length >= 2) {
                try {
                    int newTime = Integer.parseInt(args[1]);
                    timeLeft = newTime;
                    sender.sendMessage(color("&#FF3333&lRUMAH KITA &8| &7Restart time changed to &#FFC000" + formatTime(newTime) + "&7!"));
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&#FF3333&lRUMAH KITA &8| &#FF5555Please enter a valid number of seconds!"));
                }
            } else {
                timeLeft = 0; 
                sender.sendMessage(color("&#FF3333&lRUMAH KITA &8| &#FF5555Forcing restart now!"));
            }
            return true;
        }

        sender.sendMessage(color("&cUsage: /autorestart [time | reload | now <seconds>]"));
        return true;
    }
}
