# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`orelia-debug` is a Paper/Bukkit plugin that adds admin-only testplay/debug tooling
(`/oladmin debug ...`) for the Orelia RPG plugin suite. It owns no gameplay state of its
own — every feature is implemented by reaching into the other Orelia plugins through their
published API interfaces via Bukkit's `ServicesManager`.

Dependency relationship (see README.md for full command reference):
- **OreliaCore** — hard dependency (`plugin.yml` `depend`). Provides `AdminCommandRegistry`,
  `ConfigManager`/`MessageManager`, and the `DebugApi`/`GuiApi`/`EconomyApi`/`StatusApi`
  services. If any of these services are missing at `onEnable`, the plugin disables itself.
- **OreliaWorld** / **OreliaExtra** — soft dependencies (`plugin.yml` `softdepend`). Their
  `WorldDebugApi`/`ExtraDebugApi` services may be `null` if not installed; every call site
  must null-guard and report "not installed" rather than throwing.

## Build

```
./gradlew build
```

Produces `build/libs/orelia-debug-1.0.0.jar` (shadowJar, classifier stripped). Requires
network access to `repo.papermc.io` and `jitpack.io` (resolves `orelia-core`/`orelia-world`/
`orelia-extra` directly from their GitHub repos via jitpack).

There is no `src/test` currently — `./gradlew test` runs the (empty) JUnit 5 suite.

### Local dev loop against sibling repos

`build.gradle.kts` has a temporary `mavenLocal()` entry ahead of jitpack in `repositories`.
When iterating on `orelia-core`/`orelia-world`/`orelia-extra` in parallel with this repo, run
`./gradlew publishToMavenLocal` in those repos first so changes are picked up without a push.
This line is meant to be removed before a production release (comment in build.gradle.kts
flags it) — don't remove it unprompted, but don't be surprised it's there.

## Architecture

- **`OreliaDebugPlugin`** (`core/`) — `onEnable` entry point. Fetches `AdminCommandRegistry`
  from OreliaCore's `ServicesManager` and registers each debug subcommand as its own flat
  entry directly under it (not standalone Bukkit commands — `/oladmin` itself belongs to
  orelia-core). Also loads the core/world/extra APIs and constructs `ConfigManager`/
  `MessageManager` (both reused from `rpg.core.*`, same as orelia-world/orelia-extra do).
- **`command/`** — one small `CommandExecutor`/`TabCompleter` class per subcommand
  (`GuiDebugCommand`, `MoneyDebugCommand`, `SkillPointsDebugCommand`, `ExpDebugCommand`,
  `ConfigDebugCommand`, `ConfigHelpDebugCommand`, `QuestDebugCommand`, `ManualCommand`) —
  registered flat under `/oladmin <name>` (e.g. `/oladmin money`), not nested under a
  `debug` subcommand. Each follows the same shape: validate arg count → resolve target
  player (defaulting to the sender when omitted, for the ones that take a target) → call
  into the relevant `rpg.api`/`rpg.world.api`/`rpg.extra.api` interface → send a
  `messages.yml`-keyed response. Commands needing `worldDebugApi`/`extraDebugApi` null-guard
  and report "not installed" rather than throwing (soft dependencies).
- **`DebugManual`** (`command/`) — renders `/oladmin manual [page]`, a clickable-pagination
  in-game mirror of README.md's command list (via orelia-core's `Pagination`). Keep this
  list in sync with README.md and `messages.yml`'s `usage.*` keys when subcommands change.
- **`messages.yml`** — every user-facing string is a key here, sent through
  `MessageManager`. Do not hardcode `ChatColor` + literal strings in command code; add a
  key instead (this convention is stated directly in the resource file's header comment).

### Adding a new debug subcommand

Touch, in order: a new `command/XyzDebugCommand.java` (mirror `MoneyDebugCommand`'s shape),
`OreliaDebugPlugin` (load any new API service, register the command under
`AdminCommandRegistry`), `messages.yml` (usage + result keys), `DebugManual` (new `Entry`),
and `README.md` (command reference section).
