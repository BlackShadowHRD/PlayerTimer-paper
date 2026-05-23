package boti.doc.playertimer;

import org.bukkit.entity.Player;

public interface PlayerTimerAPI {
    boolean startCountup(Player player);
    boolean startCountdown(Player player, int seconds);

    boolean pause(Player player);
    boolean resume(Player player);
    boolean stop(Player player);
    boolean reset(Player player);

    boolean show(Player player);
    boolean hide(Player player);

    boolean hasTimer(Player player);
    int getTime(Player player);
    TimerState getState(Player player);
    TimerMode getMode(Player player);
}