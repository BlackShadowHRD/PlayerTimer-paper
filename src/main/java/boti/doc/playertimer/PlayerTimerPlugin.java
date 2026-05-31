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
        TimerStore store = new TimerStore(getDataFolder().toPath());
        timerService = new PlayerTimerService(store);
        new PlayerTimerCommand(timerService).register(getLifecycleManager());
        Bukkit.getScheduler().runTaskTimer(this, timerService::tickAllPlayers, 20L, 20L);
        Bukkit.getPluginManager().registerEvents(timerService, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerTimer disabled");
        timerService.saveAll();
    }

}
