package boti.doc.playertimer;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// This plugin provides a timer on a per player basis. The timer can be counting up
// or down and the time the countdown runs for is user adjustable but will default
// to 5 minutes
public final class PlayerTimerPlugin extends JavaPlugin {

    // The timer can have one of two values but will default to COUNTUP
    private enum TimerMode {
        COUNTUP,
        COUNTDOWN
    }
    // hash map of players and the current value of their timer
    private final Map<UUID, Integer> timers = new HashMap<>();
    // hash map of players who have an active timer
    private final Map<UUID, Boolean> running = new HashMap<>();
    // hashmap of players and the type of timer they have running
    private final Map<UUID, TimerMode> timerMode = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("PlayerTimer enabled");

        // Run this loop once every second. It will scan for active players and check if they
        // have an active timer. Depending on the timer mode, the timer value will be increased
        // or decreased by 1 second. The new time will be displayed in the player's action bar.
        // A countdown timer reaching 0 will be stopped and the user be sent a message.
        // In future we may have countup timers that will terminate at a user defined time.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID id = player.getUniqueId();

                if (!running.getOrDefault(id, false)) {
                    continue;
                }

                int seconds;
                if (timerMode.getOrDefault(id, TimerMode.COUNTUP) == TimerMode.COUNTDOWN) {
                    seconds = timers.getOrDefault(id, 0) - 1;
                    if (seconds <= 0) {
                        seconds = 0;
                        running.put(id, false);
                        player.sendMessage("Your time is up.");
                    }
                    timers.put(id, seconds);
                }
                else {
                    seconds = timers.getOrDefault(id, 0) + 1;
                    timers.put(id, seconds);
                }
                player.sendActionBar(Component.text("Time: " + formatTime(seconds)));
            }
        }, 20L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerTimer disabled");
    }

    // convert seconds value into mm:ss string for display
    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // At present the command has to be initiated by the player. Eventually we will change the code so
    // that the timer can be triggered either by the player pressing a button or by the player entering
    // a defined area. We may also provide an API to allow minigame designers access to the timer.
    // The following commands need to be implemented:
    //      /playertimer start [countup|countdown] [second]
    //      /playertimer stop
    //      /playertimer reset
    //      /playertimer pause
    //      /playertimer resume
    //      /playertimer hide
    //      /playertimer show
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Use /playertimer start, stop, or reset.");
            return true;
        }

        UUID id = player.getUniqueId();

        switch (args[0].toLowerCase()) {
            case "start" -> {
                timers.putIfAbsent(id, 0);
                timerMode.putIfAbsent(id, TimerMode.COUNTUP);
                if (args.length >= 2) {
                    switch (args[1].toLowerCase()) {
                        case "countdown" -> {
                            timerMode.put(id, TimerMode.COUNTDOWN);
                            int countdownSeconds;
                            if (args.length >= 3) {
                                try {
                                    countdownSeconds = Integer.parseInt(args[2]);
                                } catch (NumberFormatException e) {
                                    player.sendMessage("Countdown length must be a number of seconds.");
                                    return true;
                                }

                            } else {
                                countdownSeconds = 300; // if the user does not specify the length of the countdown, it will default to 5 min
                            }
                            timers.put(id, countdownSeconds);
                        }
                        case "countup" -> {
                            timerMode.put(id, TimerMode.COUNTUP);
                        }
                    }
                }
                running.put(id, true);
                player.sendMessage("Your timer has started.");
            }
            case "stop" -> {
                running.put(id, false);
                player.sendMessage("Your timer has stopped.");
            }
            case "reset" -> {
                timers.put(id, 0);
                running.put(id, false);
                player.sendMessage("Your timer has reset.");
            }
            default -> player.sendMessage("Use /playertimer start, stop, or reset.");
        }

        return true;
    }
}
