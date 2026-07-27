# Vertex Client

Vertex Client is an advanced automation and utility mod for Minecraft (Hypixel Skyblock), designed specifically to run on Fabric 1.21.11.

## Features

- **Dynamic HTML/CSS/JS Dashboard:** Fully interactive, embedded Chromium (MCEF) GUI that seamlessly bridges Svelte components with the Java backend. 
- **Dynamic Configuration Sync:** 150+ config annotations parsed via reflection to dynamically generate schema maps and sync back instantly to standard JSON configuration files over HTTP IPC.
- **Smart Task Macros:** Modular automated farming, mining, fishing, and combat solvers leveraging custom graph pathfinding protocols.
- **Automated Routing:** Complex internal routing and custom macro graphs loaded safely from JSON definitions.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11.
2. Put the `VertexClient-1.0.0.jar` into your `.minecraft/mods` directory.
3. Make sure you also install the required Fabric API dependency.

## Development & Building

1. Ensure you have **Java 21+** installed.
2. Clone this repository:
   ```bash
   git clone https://github.com/FuzzyDonutPro/Vertex-Client.git
   cd Vertex-Client
   ```
3. To build the Java Client & Svelte UI together:
   ```bash
   ./gradlew build
   ```
4. The fully compiled jar (including the embedded UI files) will be located in `build/libs/`.

### Modifying the UI

The UI is built with **Svelte** & **Vite**.
You can find the frontend code located inside the `/ui` folder.
During a Gradle build, `vite build` will automatically bundle the dashboard into the Java resources folder so it can be served locally by the `VertexUIServer` inside the game.
