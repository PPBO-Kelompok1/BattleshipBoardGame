# Battleship Swing

A Java Swing Battleship game refactored from a single monolithic source file into focused modules.

## Setup

Requires a JDK that supports modern switch expressions.

Compile:

```powershell
javac -d out $(Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

Run:

```powershell
java -cp out Main
```

## Architecture

The application uses a simple layered Swing architecture:

```text
Main
  -> core.Game
      -> rendering.BoardPanel
      -> entities.Player / entities.AIPlayer
          -> physics.Board / physics.Tile
          -> entities.Ship subclasses
          -> systems.ai.AttackPlanner
      -> input.GameCallback / input.CoordConsumer
      -> config.GameConfig
```

## Module Responsibilities

`src/Main.java`

Application bootstrap. Schedules the Swing UI on the event dispatch thread and starts `core.Game`.

`src/core`

Owns top-level game flow and shared gameplay enums. `Game` coordinates setup, turns, win checks, skill callbacks, and restart behavior. `Direction` and `Difficulty` describe game options.

`src/config`

Centralized constants such as board size, ship count, and attacks per turn.

`src/entities`

Domain objects for ships and players. `Ship` defines placement, orientation, health, and skill contract. `Destroyer`, `Battleship`, and `Submarine` implement the existing skills. `Player` owns a board and fleet. `AIPlayer` exposes AI actions while delegating targeting decisions to the AI system.

`src/physics`

Board state and tile state. Placement validation, attack state, sunk checks, and recent attack clearing live here.

`src/systems/ai`

AI memory, hit clustering, probability heatmap targeting, focused targeting, and difficulty-based forgetting.

`src/rendering`

Swing board rendering, hover effects, tile buttons, enum display formatting, and optional console board rendering.

`src/input`

Small callback interfaces used by ship skills to request board coordinates without depending on Swing classes.

`src/utils`

General helpers. Currently contains terminal color parsing for console rendering.

## Dependency Notes

`core.Game` is the composition root and is allowed to know about UI, entities, and configuration. Entities do not depend on Swing. Ship skills depend only on `input.GameCallback` and `physics.Board`, which keeps gameplay logic testable. `systems.ai.AttackPlanner` depends on the domain model and board state, but the controller only calls `AIPlayer.performTurn(...)`.

No circular package dependencies are required for normal gameplay flow.

## Behavior Preservation

The refactor preserves the existing gameplay flow:

- Player places three ships.
- AI places three random ships.
- Player gets three attacks per turn.
- AI performs three attacks per turn.
- Existing ship skills remain available through the skill selector.
- Existing difficulty modes and AI memory behavior are preserved.
- Existing board colors, labels, hover previews, and win conditions are preserved.

## Future Improvements

Possible next steps:

- Add automated tests for board placement, skills, and AI targeting.
- Replace random AI placement retry loops with bounded placement generation.
- Add a formal scene/state enum for setup, player turn, AI turn, and game over.
- Introduce asset/audio managers if visual or sound assets are added.
- Persist settings or match history through a save/load service.
