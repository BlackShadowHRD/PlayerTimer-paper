package boti.doc.playertimer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.kyori.adventure.text.format.NamedTextColor;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimerStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "timers.json";

    private final Path dataFolder;

    public TimerStore(Path dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void save(Map<UUID, PlayerTimer> timers) {
        Map<String, TimerData> serializable = new HashMap<>();

        for (Map.Entry<UUID, PlayerTimer> entry : timers.entrySet()) {
            serializable.put(entry.getKey().toString(), TimerData.from(entry.getValue()));
        }

        try {
            Files.createDirectories(dataFolder);
            try (Writer writer = Files.newBufferedWriter(dataFolder.resolve(FILE_NAME))) {
                GSON.toJson(serializable, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, PlayerTimer> load() {
        Path file = dataFolder.resolve(FILE_NAME);
        if (!Files.exists(file)) return new HashMap<>();

        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<Map<String, TimerData>>() {}.getType();
            Map<String, TimerData> raw = GSON.fromJson(reader, type);

            if (raw == null) return new HashMap<>();

            Map<UUID, PlayerTimer> timers = new HashMap<>();
            for (Map.Entry<String, TimerData> entry : raw.entrySet()) {
                timers.put(UUID.fromString(entry.getKey()), entry.getValue().toTimer());
            }
            return timers;

        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // Flat DTO — Gson serializes this directly
    private static class TimerData {
        String mode;
        String state;
        boolean visible;
        int time;
        String color;

        static TimerData from(PlayerTimer timer) {
            TimerData d = new TimerData();
            d.mode = timer.getMode().name();
            d.state = timer.getState().name();
            d.visible = timer.isVisible();
            d.time = timer.getTime();
            d.color = NamedTextColor.NAMES.key(timer.getColor());
            return d;
        }

        PlayerTimer toTimer() {
            PlayerTimer timer = new PlayerTimer(
                    TimerMode.valueOf(mode),
                    time
            );
            // Pause running timers on restore — the server stopped, so they weren't ticking
            TimerState restored = TimerState.valueOf(state);
            if (restored == TimerState.RUNNING) restored = TimerState.PAUSED;

            // Use package-private or a restore method to set state without
            // triggering normal precondition checks
            timer.restore(restored, visible, NamedTextColor.NAMES.value(color));
            return timer;
        }
    }
}