package boti.doc.playertimer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class PlayerTimerService {

    private final Map<UUID, PlayerTimer> timers = new HashMap<>();

    public int startCountup(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();

        // If the player does not have a timer, create one and start it
        timers.putIfAbsent(id, new PlayerTimer(TimerMode.COUNTUP, TimerState.READY, false, 0));
        PlayerTimer timer = timers.get(id);

        // If the player has a running timer do nothing but alert the player
        // Otherwise set time to 0 and running to true
        if (timer.getState() == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
        } else {
            timer.setMode(TimerMode.COUNTUP);
            timer.setTime(0);
            timer.setState(TimerState.RUNNING);
            timer.setVisible(true);
            player.sendMessage("Your timer has been started.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int startCountdown(CommandSourceStack source, int seconds) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();

        timers.putIfAbsent(id, new PlayerTimer(TimerMode.COUNTDOWN, TimerState.READY, true, seconds));
        PlayerTimer timer = timers.get(id);

        // If the player has a running timer do nothing but alert the player
        // Otherwise set time to the countdown value and running to true
        if (timer.getState() == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
        } else {
            timer.setMode(TimerMode.COUNTDOWN);
            timer.setTime(seconds);
            timer.setState(TimerState.RUNNING);
            timer.setVisible(true);
            player.sendMessage("Your timer has been started.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int pauseTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else if (timer.getState() == TimerState.RUNNING){
            timer.setState(TimerState.PAUSED);
            player.sendMessage("Your timer has been paused.");
        } else {
            player.sendMessage("Your timer is not running.");
        }

        return Command.SINGLE_SUCCESS;
    }


    public int resumeTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else if (timer.getState() != TimerState.PAUSED) {
            player.sendMessage("You do not have a paused timer.");
        } else {
            timer.setState(TimerState.RUNNING);
            timer.setVisible(true);
            player.sendMessage("Your timer has been resumed.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int stopTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else if (timer.getState() == TimerState.RUNNING || timer.getState() == TimerState.PAUSED) {
            timer.setState(TimerState.STOPPED);
            timer.setTime(0);
            timer.setVisible(false);
            player.sendMessage("Your timer has been stopped.");
        } else {
            player.sendMessage("Your timer was not running.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int resetTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
        } else {
            timer.setState(TimerState.READY);
            timer.setTime(0);
            timer.setVisible(true);
            player.sendMessage("Your timer has been reset.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int hideTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        // Hide timer whether or not it was visible before
        timer.setVisible(false);
        player.sendMessage("Your timer is now hidden.");

        return Command.SINGLE_SUCCESS;
    }

    public int showTimer(CommandSourceStack source) {
        if (!(source.getExecutor() instanceof Player player)) {
            if (source.getSender() != null) {
                source.getSender().sendMessage("Only players can use this command.");
            }
            return Command.SINGLE_SUCCESS;
        }

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        // Show timer whether or not it was visible before
        timer.setVisible(true);
        player.sendMessage("Your timer is now visible.");

        return Command.SINGLE_SUCCESS;
    }

    // convert seconds value into mm:ss string for display
    public String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    public int executeStartCountdown(
            CommandContext<CommandSourceStack> ctx,
            String duration
    ) {
        try {
            int seconds = TimeParser.parseToSeconds(duration);
            return startCountdown(ctx.getSource(), seconds);

        } catch (IllegalArgumentException e) {
            if (ctx.getSource().getSender() != null) {
                ctx.getSource().getSender().sendMessage(
                        "Invalid duration. Use seconds, mm:ss, hh:mm:ss, or formats like 1h0m10s."
                );
            }

            return Command.SINGLE_SUCCESS;
        }
    }

    public void tickAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerTimer timer = timers.get(id);

            if (timer == null) {
                continue;
            }

            if (timer.getState() != TimerState.RUNNING) {
                if (timer.isVisible()) {
                    player.sendActionBar(Component.text("Time: " + formatTime(timer.getTime())));
                }
                continue;
            }

            if (timer.getMode() == TimerMode.COUNTDOWN) {
                timer.setTime(timer.getTime() - 1);
                if (timer.getTime() <= 0) {
                    timer.setTime(0);
                    timer.setState(TimerState.FINISHED);
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
                timer.setTime(timer.getTime() + 1);
            }
            if (timer.isVisible()) {
                player.sendActionBar(Component.text("Time: " + formatTime(timer.getTime())));
            }
        }
    }

}