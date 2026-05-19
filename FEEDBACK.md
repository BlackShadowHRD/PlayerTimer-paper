# One HashMap to rule them all

Currently, timers are stored across three separate hash maps.

```java
public final class PlayerTimerPlugin extends JavaPlugin {
    // ...

    // hash map of players and the current value of their timer
    private final Map<UUID, Integer> timers = new HashMap<>();
    // hash map of players who have an active timer
    private final Map<UUID, Boolean> running = new HashMap<>();
    // hashmap of players and the type of timer they have running
    private final Map<UUID, TimerMode> timerMode = new HashMap<>();
```

As far as I can tell, there are currently no bugs resulting from this. However, there are a number of reasons why this structure is not desirable.

- Correctness - A timer should always have a "seconds" count, a "running" state, and a "mode". However, because these are stored in 3 separate maps, this is not enforced by the type system. It is therefore quite easy to introduce logic bugs, e.g. by failing to register the timer in one or more of the maps. This would be a runtime bug. Wherever possible, we should try to make bugs visible at compile time.

- Readability - Because we use 3 maps instead of one, our initialisation logic is spread out, making it harder to read. Also, because the type system can't verify whether the timer has been fully initialised, we end up using `getOrDefault` a lot, which begs the question "when would we fall back to default?". The answer seems to be "never", so this is a waste of cognitive load. Ideally, we want whoever reads the code to spend their time thinking about the programs possible states, rather than spending time reasoning about impossible states. This is a classic case of essential complexity vs. accidental complexity.

- Memory usage - Not really an issue here, but including for interest. TLDR; Hash maps have a significant memory overhead compared to an array. All else being equal, it is preferable to use one hash map instead of 3.

- Performance - Again, not really an issue here. TLDR; Hash maps involve some extra pointer lookups, elements are not stored contiguously, and a hash needs to be computed on each lookup. Minimising the number of hash maps used is therefore beneficial for performance.

I would recommend unifying the timer state into a single class:

```java
public class PlayerTimer {
    public TimerMode mode;
    public boolean running;
    public int time;

    public PlayerTimer(TimerMode mode, boolean running, int time) {
        this.mode = mode;
        this.running = running;
        this.time = time;
    }
}
```

*Note: We're using `int` and `boolean` instead of `Integer` and `Boolean`. The latter are classes so will always be stored as a pointer in Java.*

Now you can create a single hash map for all the data associated with each players timers:

```java
public final class PlayerTimerPlugin extends JavaPlugin {
    // ...
    private final Map<UUID, PlayerTimer> timers = new HashMap<>();
```

# Paper Command System / Brigadier

It is generally recommended to use brigadier to parse and execute commands in paper plugins. This is documented [here](https://docs.papermc.io/paper/dev/command-api/basics/introduction/). This removes the need for some of the hand written logic.

For example, instead of:

```java
switch (args[1].toLowerCase()) {
    case "countdown" -> {
        // ...
    }
    case "countup" -> {
        // ...
    }
}
```

You can do something like this:

```java
Commands.literal("playertimer")
    .then(Commands.literal("start")
        .then(Commands.literal("countdown")
            .then(Commands.argument("seconds", LongArgumentType.longArg(/* min */ 0))
                .executes(ctx -> {
                    // ...
                })
            )
        )
        .then(Commands.literal("countup")
            .then(Commands.argument("seconds", LongArgumentType.longArg(/* min */ 0))
                .executes(ctx -> {
                    // ...
                })
            )
        )
    )
    .then(Commands.literal("stop")
        .then(/* ... */)
    )
```

This does a lot of the validation for you, and is a little easier to read than the manual validation logic.

# Persisting Data

Currently, timers are lost every time the server restarts. Paper has a concept called Persistent Data Containers (PDCs). These can be used to store data on e.g. a player entity. You could implement `PersistentDataType` on the `PlayerTimer` class, and this way the current timer state can be persisted across restarts. This also eliminates the need to track timers in a hash map.
