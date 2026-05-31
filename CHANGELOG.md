# Changelog

## 26.1.x-1.3
- Timers are now persistent over server restarts
- Added an admin command that allows ops to reset all timers or specific timers

## 26.1.x-1.2
- Code refactored

## 26.1.x-1.1
- Added coloured timer support
- Added optional colour arguments to timer start commands

## 26.1.x-1.0

- Major internal refactor
- Moved timer logic into `PlayerTimerService`
- Added `PlayerTimer` model class
- Added `PlayerTimerAPI` interface groundwork
- Split `TimerMode` and `TimerState` into dedicated enum files
- Simplified `PlayerTimerPlugin` to focus on plugin lifecycle and command registration
- Improved separation of concerns and maintainability
- Improved support for command block execution using `/execute as @p`
- Added safer sender/executor handling

## 0.3.0

- Added Java 25 support
- Changed to /playertimer startcountup and /playertimer startcountdown
- Changed seconds parameter in countdown to duration
- Added advance duration parsing with overflow protection
- Updated README documentation

## 0.2.0

- Migrated command handling to Brigadier
- Added timer state system
- Added visibility system
- Added pause/resume support
- Added countdown completion sounds
- Added command block compatibility
- Refactored timer storage into `PlayerTimer` objects
- Added countdown completion handling
- Improved README documentation

---
