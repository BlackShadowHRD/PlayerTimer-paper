package boti.doc.playertimer;

import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerTimer {

    private final TimerMode mode;
    private TimerState state;
    private boolean visible;
    private int time;
    private NamedTextColor color;

    public PlayerTimer(TimerMode mode, int startTime) {
        this.mode = mode;
        this.time = startTime;
        this.state = TimerState.READY;
        this.visible = true;
        this.color = NamedTextColor.WHITE;
    }

    // --- Domain operations ---

    public void start() {
        if (state == TimerState.RUNNING) {
            throw new IllegalStateException("Timer is already running");
        }
        state = TimerState.RUNNING;
    }

    public void pause() {
        if (state != TimerState.RUNNING) {
            throw new IllegalStateException("Cannot pause a timer that isn't running");
        }
        state = TimerState.PAUSED;
    }

    public void resume() {
        if (state != TimerState.PAUSED) {
            throw new IllegalStateException("Cannot resume a timer that isn't paused");
        }
        state = TimerState.RUNNING;
    }

    public void stop() {
        if (state != TimerState.RUNNING && state != TimerState.PAUSED) {
            throw new IllegalStateException("Timer is not active");
        }
        state = TimerState.STOPPED;
        time = 0;
    }

    public void reset() {
        state = TimerState.READY;
        time = 0;
    }

    /**
     * Advances the timer by one second up or down depending on mode.
     * Enforces invariants 1–6. Returns true if the timer just finished (hit zero),
     * so callers can react (play sound, send message) without inspecting state themselves.
     */
    public boolean tick() {
        if (state != TimerState.RUNNING) {       // invariant 1
            return false;
        }

        if (mode == TimerMode.COUNTDOWN) {       // invariants 3 & 4
            time = Math.max(0, time - 1);        // invariant 5
            if (time == 0) {                     // invariant 6
                state = TimerState.FINISHED;
                return true;                     // signal: timer just expired
            }
        } else {
            time += 1;
        }

        return false;
    }

    public String toDisplayString() {
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int seconds = time % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    // This is to bypass the normal state preconditions during loading
    void restore(TimerState state, boolean visible, NamedTextColor color) {
        this.state = state;
        this.visible = visible;
        this.color = color;
    }

    // --- Accessors (no more raw setters for time/state/mode) ---

    public TimerMode getMode()        { return mode; }
    public TimerState getState()      { return state; }
    public int getTime()              { return time; }
    public boolean isVisible()        { return visible; }
    public NamedTextColor getColor()  { return color; }

    public void setVisible(boolean visible) { this.visible = visible; }
    public void setColor(NamedTextColor color) { this.color = color; }
}