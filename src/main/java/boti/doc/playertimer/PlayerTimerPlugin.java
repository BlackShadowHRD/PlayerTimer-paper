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
    // Instead of the original three separate hashmaps we are creating the class PlayerTimer
    // which will store the timer mode (countup or countdown), whether it is running and
    // what it's current value in seconds is. We can then create a single hashmap which will
    // have the UUID as the key and PlayerTimer with all the required timer information.
    public class PlayerTimer {
        public TimerMode mode;
        public boolean running;
        public int time;

        public PlayerTimer(TimerMode mode, boolean running, int time) {
            this.mode = mode;
            this.running = running;
            this.time = time;
        }
    }
    private final Map<UUID, PlayerTimer> timers = new HashMap<>();

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
                PlayerTimer timer = timers.get(id);

                if (timer == null || !timer.running) {
                    continue;
                }

                if (timer.mode == TimerMode.COUNTDOWN) {
                    timer.time -= 1;
                    if (timer.time <= 0) {
                        timer.time = 0;
                        timer.running = false;
                        player.sendMessage("Your time is up.");
                    }
                }
                else {
                    timer.time += 1;
                }
                player.sendActionBar(Component.text("Time: " + formatTime(timer.time)));
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
                timers.putIfAbsent(id, new PlayerTimer(TimerMode.COUNTUP, false, 0));
                PlayerTimer timer = timers.get(id);
                if (args.length >= 2) {
                    switch (args[1].toLowerCase()) {
                        case "countdown" -> {
                            timer.mode = TimerMode.COUNTDOWN;
                            if (args.length >= 3) {
                                try {
                                    timer.time = Integer.parseInt(args[2]);
                                } catch (NumberFormatException e) {
                                    player.sendMessage("Countdown length must be a number of seconds.");
                                    return true;
                                }

                            } else {
                                timer.time = 300; // if the user does not specify the length of the countdown, it will default to 5 min
                            }
                        }
                        case "countup" -> {
                            timer.mode = TimerMode.COUNTUP;
                        } // If we are going to allow an upper limit for countup, the logic to deal with it needs to go here
                    }
                }
                timer.running = true;
                player.sendMessage("Your timer has started.");
            }
            case "stop" -> {
                PlayerTimer timer = timers.get(id);
                if (timer == null) {
                    player.sendMessage("You do not have a timer.");
                    return true;
                }
                timer.running = false;
                player.sendMessage("Your timer has stopped.");
            }
            case "reset" -> {
                PlayerTimer timer = timers.get(id);
                if (timer == null) {
                    player.sendMessage("You do not have a timer.");
                    return true;
                }
                timer.running = false;
                timer.time = 0;
                player.sendMessage("Your timer has been reset to 00:00.");
            }
            default -> player.sendMessage("Use /playertimer start, stop, or reset.");
        }
        return true;
    }
}
