package boti.doc.playertimer;


import com.mojang.brigadier.arguments.StringArgumentType;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

// This plugin provides a timer on a per player basis. The timer can be counting up
// or down and the time the countdown runs for is user adjustable but will default
// to 5 minutes
public final class PlayerTimerPlugin extends JavaPlugin {

    private PlayerTimerService timerService;

    @Override
    public void onEnable() {
        getLogger().info("PlayerTimer enabled");

        timerService = new PlayerTimerService();

        // Implement the command using Brigadier
        // playertimer
        // ├── start
        // │   ├── countup
        // │       └── color
        // │   └── countdown
        // │       └── seconds
        // │            └── color
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
                                            .executes(ctx -> timerService.startCountup(ctx.getSource(), "white"))

                                            .then(Commands.argument("color", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String colorName = StringArgumentType.getString(ctx, "color");
                                                        return timerService.startCountup(ctx.getSource(), colorName);
                                                    })
                                            )
                                    )

                                    .then(Commands.literal("startcountdown")
                                            .executes(ctx -> timerService.startCountdown(ctx.getSource(), 300, "white"))

                                            .then(Commands.argument("duration", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String duration = StringArgumentType.getString(ctx, "duration");
                                                        return timerService.executeStartCountdown(ctx, duration, "white");
                                                    })

                                                    .then(Commands.argument("color", StringArgumentType.word())
                                                            .executes(ctx -> {
                                                                String duration = StringArgumentType.getString(ctx, "duration");
                                                                String colorName = StringArgumentType.getString(ctx, "color");
                                                                return timerService.executeStartCountdown(ctx, duration, colorName);
                                                            })
                                                    )
                                            )
                                    )

                                    .then(Commands.literal("pause")
                                            .executes(ctx -> timerService.pauseTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("resume")
                                            .executes(ctx -> timerService.resumeTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("stop")
                                            .executes(ctx -> timerService.stopTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("reset")
                                            .executes(ctx -> timerService.resetTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("hide")
                                            .executes(ctx -> timerService.hideTimer(ctx.getSource()))
                                    )

                                    .then(Commands.literal("show")
                                            .executes(ctx -> timerService.showTimer(ctx.getSource()))
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
            timerService.tickAllPlayers();
        }, 20L, 20L);

    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerTimer disabled");
    }

}
