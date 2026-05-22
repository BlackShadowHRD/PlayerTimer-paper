package boti.doc.playertimer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Sound;

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
    private enum TimerState {
        READY,
        RUNNING,
        PAUSED,
        STOPPED,
        FINISHED
    }

    // Instead of the original three separate hashmaps we are creating the class PlayerTimer
    // which will store the timer mode (countup or countdown), whether it is running and
    // what it's current value in seconds is. We can then create a single hashmap which will
    // have the UUID as the key and PlayerTimer with all the required timer information.
    public class PlayerTimer {
        public TimerMode mode;
        public TimerState state;
        public boolean visible;
        public int time;

        public PlayerTimer(TimerMode mode, TimerState state, boolean visible, int time) {
            this.mode = mode;
            this.state = state;
            this.visible = visible;
            this.time = time;
        }
    }
    private final Map<UUID, PlayerTimer> timers = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("PlayerTimer enabled");

        // Implement the command using Brigadier
        // playertimer
        // ├── start
        // │   ├── countup
        // │   └── countdown
        // │       └── seconds
        // ├── pause
        // ├── resume
        // ├── stop
        // ├── reset
        // ├── hide
        // └── show
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                    Commands commands = event.registrar();

                    commands.register(
                            Commands.literal("playertimer")
                                    .then(Commands.literal("startcountup")
                                            .executes(ctx -> startCountup(ctx.getSource()))
                                    )

                                    .then(Commands.literal("startcountdown")
                                            .executes(ctx -> startCountdown(ctx.getSource(), 300))

                                            .then(Commands.argument("duration", StringArgumentType.greedyString())
                                                    .executes(ctx -> {
                                                        try {
                                                            String duration = StringArgumentType.getString(ctx, "duration");
                                                            int seconds = TimeParser.parseToSeconds(duration);
                                                            return startCountdown(ctx.getSource(), seconds);
                                                        } catch (IllegalArgumentException e) {
                                                            ctx.getSource().getExecutor().sendMessage(
                                                                    "Invalid duration. Use seconds, or format like: 1h0m10s or 01:00:10"
                                                            );
                                                            return Command.SINGLE_SUCCESS;
                                                        }
                                                    })
                                            )

                                    )

                                    .then(Commands.literal("pause")
                                            .executes(ctx -> pauseTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("resume")
                                            .executes(ctx -> resumeTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("stop")
                                            .executes(ctx -> stopTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("reset")
                                            .executes(ctx -> resetTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("hide")
                                            .executes(ctx -> hideTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("show")
                                            .executes(ctx -> showTimer(ctx.getSource()))
                                    )

                                    .build()
                    );
                });

        // Run this loop once every second. It will scan for active players and check if they
        // have an active timer. Depending on the timer mode, the timer value will be increased
        // or decreased by 1 second. The new time will be displayed in the player's action bar.
        // A countdown timer reaching 0 will be stopped and the user be sent a message.
        // In future we may have countup timers that will terminate at a user defined time.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID id = player.getUniqueId();
                PlayerTimer timer = timers.get(id);

                if (timer == null) {
                    continue;
                }

                if (timer.state != TimerState.RUNNING) {
                    if (timer.visible) {
                        player.sendActionBar(Component.text("Time: " + formatTime(timer.time)));
                    }
                    continue;
                }

                if (timer.mode == TimerMode.COUNTDOWN) {
                    timer.time -= 1;
                    if (timer.time <= 0) {
                        timer.time = 0;
                        timer.state = TimerState.FINISHED;
                        player.sendMessage("Your time is up.");
                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_BELL_USE,
                                1.0f,
                                0.7f
                        );
                    }
                }
                else {
                    timer.time += 1;
                }
                if (timer.visible) {
                    player.sendActionBar(Component.text("Time: " + formatTime(timer.time)));
                }
            }
        }, 20L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerTimer disabled");
    }

    // convert seconds value into mm:ss string for display
    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private int startCountup(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();

        // If the player does not have a timer, create one and start it
        timers.putIfAbsent(id, new PlayerTimer(TimerMode.COUNTUP, TimerState.READY, false, 0));
        PlayerTimer timer = timers.get(id);

        // If the player has a running timer do nothing but alert the player
        // Otherwise set time to 0 and running to true
        if (timer.state == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
        } else {
            timer.mode = TimerMode.COUNTUP;
            timer.time = 0;
            timer.state = TimerState.RUNNING;
            timer.visible = true;
            player.sendMessage("Your timer has been started.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int startCountdown(CommandSourceStack source, int seconds) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();

        timers.putIfAbsent(id, new PlayerTimer(TimerMode.COUNTDOWN, TimerState.READY, true, seconds));
        PlayerTimer timer = timers.get(id);

        // If the player has a running timer do nothing but alert the player
        // Otherwise set time to the countdown value and running to true
        if (timer.state == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
        } else {
            timer.mode = TimerMode.COUNTDOWN;
            timer.time = seconds;
            timer.state = TimerState.RUNNING;
            timer.visible = true;
            player.sendMessage("Your timer has been started.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int pauseTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else if (timer.state == TimerState.RUNNING){
            timer.state = TimerState.PAUSED;
            player.sendMessage("Your timer has been paused.");
        } else {
            player.sendMessage("Your timer is not running.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int resumeTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else if (timer.state != TimerState.PAUSED) {
            player.sendMessage("You do not have a paused timer.");
        } else {
            timer.state = TimerState.RUNNING;
            timer.visible = true;
            player.sendMessage("Your timer has been resumed.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int stopTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else if (timer.state == TimerState.RUNNING || timer.state == TimerState.PAUSED) {
            timer.state = TimerState.STOPPED;
            timer.time = 0;
            timer.visible = false;
            player.sendMessage("Your timer has been stopped.");
        } else {
            player.sendMessage("Your timer was not running.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int resetTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else {
            timer.state = TimerState.READY;
            timer.time = 0;
            timer.visible = true;
            player.sendMessage("Your timer has been reset.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private int hideTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        // Set timer to not being visible whether or not it was visible before
        timer.visible = false;
        player.sendMessage("Your timer is now hidden.");

        return Command.SINGLE_SUCCESS;
    }

    private int showTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            source.getExecutor().sendMessage("Only players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        // Set timer to being visible whether or not it was visible before
        timer.visible = true;
        player.sendMessage("Your timer is now visible.");

        return Command.SINGLE_SUCCESS;
    }

}
