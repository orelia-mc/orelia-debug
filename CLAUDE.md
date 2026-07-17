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
  from OreliaCore's `ServicesManager` and registers a single `debug` subcommand handler
  under it (not a standalone Bukkit command — `/oladmin` itself belongs to orelia-core).
  Also loads the core/world/extra APIs and constructs `ConfigManager`/`MessageManager`
  (both reused from `rpg.core.*`, same as orelia-world/orelia-extra do).
- **`DebugAdminCommand`** (`command/`) — the single `CommandExecutor`/`TabCompleter` for
  every `/oladmin debug <subcommand>`. All subcommands (`gui`, `money`, `config`,
  `confighelp`, `quest`, `npc`, `exp`, `manual`) are dispatched from one class; each private
  handler method follows the same shape: validate arg count → resolve target player (if any)
  → call into the relevant `rpg.api`/`rpg.world.api`/`rpg.extra.api` interface → send a
  `messages.yml`-keyed response. `worldDebugApi`/`extraDebugApi` are checked for `null` at
  the top of any branch that needs them.
- **`DebugManual`** (`command/`) — renders `/oladmin debug manual [page]`, a paginated
  in-game mirror of README.md's command list. Keep this list in sync with README.md and
  `messages.yml`'s `usage.*` keys when subcommands change.
- **`messages.yml`** — every user-facing string is a key here, sent through
  `MessageManager`. Do not hardcode `ChatColor` + literal strings in command code; add a
  key instead (this convention is stated directly in the resource file's header comment).

### Adding a new debug subcommand

Touch, in order: `DebugAdminCommand` (dispatch in `onCommand`, tab-completion in
`onTabComplete`, a new private handler), `messages.yml` (usage + result keys), `DebugManual`
(new `Entry`), and `README.md` (command reference section).
