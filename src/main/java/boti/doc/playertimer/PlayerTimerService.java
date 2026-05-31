package boti.doc.playertimer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class PlayerTimerService {

    private final Map<UUID, PlayerTimer> timers = new HashMap<>();

    public int startCountup(CommandSourceStack source, String colorName) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        // If the player has a running timer do nothing but alert the player
        if (timer != null && timer.getState() == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

        // No timer exists for the player => create timer with mode fixed at construction
        PlayerTimer newTimer = new PlayerTimer(TimerMode.COUNTUP, 0);
        newTimer.setColor(parseColor(colorName));
        newTimer.start();
        timers.put(id, newTimer);

        player.sendMessage("Your timer has been started.");
        return Command.SINGLE_SUCCESS;
    }

    public int startCountdown(CommandSourceStack source, int seconds, String colorName) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        // If the player has a running timer do nothing but alert the player
        if (timer != null && timer.getState() == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

        // No timer exists for the player => create timer with mode fixed at construction
        PlayerTimer newTimer = new PlayerTimer(TimerMode.COUNTUP, seconds);
        newTimer.setColor(parseColor(colorName));
        newTimer.start();
        timers.put(id, newTimer);

        player.sendMessage("Your timer has been started.");
        return Command.SINGLE_SUCCESS;
    }

    public int pauseTimer(CommandSourceStack source) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(player.getUniqueId());
        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        try {
            timer.pause();
            player.sendMessage("Your timer has been paused.");
        } catch (IllegalStateException e) {
            player.sendMessage("Your timer is not running.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int resumeTimer(CommandSourceStack source) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(player.getUniqueId());
        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        try {
            timer.resume();
            player.sendMessage("Your timer has been resumed.");
        } catch (IllegalStateException e) {
            player.sendMessage("You do not have a paused timer.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int stopTimer(CommandSourceStack source) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(player.getUniqueId());
        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        try {
            timer.stop();
            player.sendMessage("Your timer has been stopped.");
        } catch (IllegalStateException e) {
            player.sendMessage("Your timer was not running.");
        }

        return Command.SINGLE_SUCCESS;
    }

    public int resetTimer(CommandSourceStack source) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(player.getUniqueId());
        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        timer.reset(); // reset has no preconditions, so no try/catch needed
        player.sendMessage("Your timer has been reset.");

        return Command.SINGLE_SUCCESS;
    }

    public int hideTimer(CommandSourceStack source) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(player.getUniqueId());
        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

        timer.setVisible(false);
        player.sendMessage("Your timer is now hidden.");

        return Command.SINGLE_SUCCESS;
    }

    public int showTimer(CommandSourceStack source) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        PlayerTimer timer = timers.get(player.getUniqueId());
        if (timer == null) {
            player.sendMessage("You do not have a timer.");
            return Command.SINGLE_SUCCESS;
        }

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
            String duration, String colorName
    ) {
        try {
            int seconds = TimeParser.parseToSeconds(duration);
            return startCountdown(ctx.getSource(), seconds, colorName);

        } catch (IllegalArgumentException e) {
            if (ctx.getSource().getSender() != null) {
                ctx.getSource().getSender().sendMessage(
                        "Invalid duration. Use seconds, mm:ss, hh:mm:ss, or formats like 1h0m10s."
                );
            }

            return Command.SINGLE_SUCCESS;
        }
    }

    private NamedTextColor parseColor(String colorName) {
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());

        if (color == null) {
            return NamedTextColor.WHITE;
        }

        return color;
    }

    public void tickAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerTimer timer = timers.get(player.getUniqueId());
            if (timer == null) continue;

            boolean justFinished = timer.tick();
            if (justFinished) notifyFinished(player);
            if (timer.isVisible()) renderTimer(player, timer);
        }
    }

    private void notifyFinished(Player player) {
        player.sendMessage("Your time is up.");
        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.7f);
    }

    private void renderTimer(Player player, PlayerTimer timer) {
        player.sendActionBar(
                Component.text("Time: " + timer.toDisplayString(), timer.getColor())
        );
    }

    private Player requirePlayer(CommandSourceStack source) {
        if (source.getExecutor() instanceof Player player) {
            return player;
        }
        if (source.getSender() != null) {
            source.getSender().sendMessage("Only players can use this command.");
        }
        return null;
    }

}