# PlayerTimer

A Paper plugin providing per-player timers for Minecraft minigames and SMP events.

Each player has their own independent timer which can run simultaneously with timers belonging to other players. Timers may count up or count down and can be controlled using Brigadier commands.

Designed for:
- minigame servers
- puzzle maps
- speedrun challenges
- escape rooms
- timed parkour
- multiplayer events where players participate independently

---

# Features

- Independent timer per player
- Countup and countdown modes
- Brigadier command support
- Pause and resume support
- Timer visibility control
- Countdown completion notification
- Action bar display
- Built for Paper servers

---

# Commands

| Command | Description |
|---|---|
| `/playertimer start` | Start a countup timer |
| `/playertimer start countup` | Explicitly start a countup timer |
| `/playertimer start countdown` | Start a 5 minute countdown |
| `/playertimer start countdown <seconds>` | Start a countdown with custom duration |
| `/playertimer pause` | Pause the current timer |
| `/playertimer resume` | Resume a paused timer |
| `/playertimer stop` | Stop the timer and reset to 00:00 |
| `/playertimer reset` | Reset the timer to 00:00 and set it to READY state |
| `/playertimer hide` | Hide the timer display |
| `/playertimer show` | Show the timer display |

---

# Timer States

The plugin internally tracks timers using a state system.

| State | Meaning |
|---|---|
| `READY` | Timer exists but has not yet started |
| `RUNNING` | Timer is actively counting |
| `PAUSED` | Timer is temporarily halted |
| `STOPPED` | Timer was manually stopped |
| `FINISHED` | Countdown timer reached zero |

This prevents invalid operations such as:
- resuming a stopped timer
- resuming a finished countdown
- starting multiple timers simultaneously

---

# Visibility Behaviour

Timers can be visible or hidden independently of their running state.

Current behaviour:

| Action | Visibility Result |
|---|---|
| Start timer | Timer becomes visible |
| Pause timer | Timer remains visible |
| Resume timer | Timer becomes visible/remains visible |
| Stop timer | Timer remains visible for 1 second, then hides |
| Reset timer | Timer remains visible |
| Hide command | Timer becomes hidden |
| Show command | Timer becomes visible |

---

# Countdown Completion

When a countdown timer reaches zero:
- the timer enters the `FINISHED` state
- the timer stops automatically
- the player receives a chat notification
- a sound effect is played

---

# Planned Features

- Region-based automatic timer triggering
- Button-triggered timers
- API for minigame developers
- Boss bar support
- Scoreboard support
- Configurable sounds
- Persistence across relogs/restarts
- PlaceholderAPI support

---

# Building

Requires:
- Java 21+
- Gradle
- Paper API

Build with:

```bash
./gradlew build
