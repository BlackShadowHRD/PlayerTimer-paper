package boti.doc.playertimer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;

public class PlayerTimerService implements Listener {

    private final Map<UUID, PlayerTimer> timers;
    private final TimerStore store;

    public PlayerTimerService(TimerStore store) {
        this.store = store;
        this.timers = store.load();
    }

    // --- Player commands ---

    public int startCountup(CommandSourceStack source, String colorName) {
        Player player = requirePlayer(source);
        if (player == null) return Command.SINGLE_SUCCESS;

        UUID id = player.getUniqueId();
        PlayerTimer timer = timers.get(id);

        if (timer != null && timer.getState() == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

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

        if (timer != null && timer.getState() == TimerState.RUNNING) {
            player.sendMessage("Your timer is already running.");
            return Command.SINGLE_SUCCESS;
        }

        PlayerTimer newTimer = new PlayerTimer(TimerMode.COUNTDOWN, seconds);
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

        timer.reset();
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

    // --- Admin commands ---

    public int clearAllTimers(CommandSourceStack source) {
        timers.clear();
        saveAll();
        source.getSender().sendMessage("All player timers have been cleared.");
        return Command.SINGLE_SUCCESS;
    }

    public int clearPlayerTimer(CommandSourceStack source, String playerName) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            source.getSender().sendMessage("Player not found or not online.");
            return Command.SINGLE_SUCCESS;
        }
        timers.remove(target.getUniqueId());
        saveAll();
        source.getSender().sendMessage("Timer cleared for " + target.getName() + ".");
        return Command.SINGLE_SUCCESS;
    }

    // --- Tick loop ---

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

    // --- Persistence ---

    public void saveAll() {
        store.save(timers);
    }

    // --- Events ---

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        saveAll();
        timers.remove(event.getPlayer().getUniqueId());
    }

    // --- Helpers ---

    private Player requirePlayer(CommandSourceStack source) {
        if (source.getExecutor() instanceof Player player) {
            return player;
        }
        if (source.getSender() != null) {
            source.getSender().sendMessage("Only players can use this command.");
        }
        return null;
    }

    private NamedTextColor parseColor(String colorName) {
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());
        return color != null ? color : NamedTextColor.WHITE;
    }
}