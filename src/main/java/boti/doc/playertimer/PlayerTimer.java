package boti.doc.playertimer;

import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerTimer {

    private TimerMode mode;
    private TimerState state;
    private boolean visible;
    private int time;
    private NamedTextColor color;
    private boolean alarm;

    public PlayerTimer(
            TimerMode mode,
            TimerState state,
            boolean visible,
            int time
    ) {
        this.mode = mode;
        this.state = state;
        this.visible = visible;
        this.time = time;
        this.color = NamedTextColor.WHITE;
        this.alarm = false;
    }

    public TimerMode getMode() {
        return mode;
    }

    public void setMode(TimerMode mode) {
        this.mode = mode;
    }

    public TimerState getState() {
        return state;
    }

    public void setState(TimerState state) {
        this.state = state;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public void setColor(NamedTextColor color) {
        this.color = color;
    }

    public boolean hasAlarm() {
        return alarm;
    }

    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }
}