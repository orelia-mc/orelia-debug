package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;
import rpg.world.api.WorldDebugApi;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code /oladmin dungeon <unlock|forcestart|forceend|ids|status> ...} - {@code player}
 * defaults to the sender when omitted (not applicable to {@code ids}). Requires OreliaWorld
 * (soft dependency). Named "dungeon" (not "dungeonblock") - that name belongs to
 * orelia-world's own admin command for placing/removing trigger blocks, a different plugin
 * registering into the same shared {@code /oladmin} dispatcher.
 */
public final class DungeonDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("unlock", "forcestart", "forceend", "ids", "status");

    private final MessageManager messages;
    private final WorldDebugApi worldDebugApi;

    public DungeonDebugCommand(MessageManager messages, WorldDebugApi worldDebugApi) {
        this.messages = messages;
        this.worldDebugApi = worldDebugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (worldDebugApi == null) {
            messages.send(sender, "gui.world-not-installed");
            return true;
        }
        if (args.length < 1) {
            messages.send(sender, "usage.dungeon");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("ids")) {
            messages.send(sender, "dungeon.ids-header");
            for (String dungeonId : worldDebugApi.listDungeonIds()) {
                messages.send(sender, "dungeon.ids-entry", "dungeon", dungeonId);
            }
            return true;
        }
        switch (sub) {
            case "unlock" -> withTargetAndDungeon(sender, args, (target, dungeonId) -> {
                boolean done = worldDebugApi.unlockDungeonForPlayer(target.getUniqueId(), dungeonId);
                messages.send(sender, done ? "dungeon.unlocked" : "dungeon.unlock-failed",
                        "player", target.getName(), "dungeon", dungeonId);
            });
            case "forcestart" -> withTargetAndDungeon(sender, args, (target, dungeonId) -> {
                var failure = worldDebugApi.forceStartDungeon(target.getUniqueId(), dungeonId);
                if (failure.isEmpty()) {
                    messages.send(sender, "dungeon.force-started", "player", target.getName(), "dungeon", dungeonId);
                } else {
                    messages.send(sender, "dungeon.force-start-failed", "player", target.getName(), "reason", failure.get());
                }
            });
            case "forceend" -> withTargetOnly(sender, args, target -> {
                boolean done = worldDebugApi.forceEndDungeon(target.getUniqueId());
                messages.send(sender, done ? "dungeon.force-ended" : "dungeon.force-end-failed", "player", target.getName());
            });
            case "status" -> withTargetOnly(sender, args, target -> {
                var activeDungeonId = worldDebugApi.getActiveDungeonId(target.getUniqueId());
                messages.send(sender, activeDungeonId.isPresent() ? "dungeon.status-active" : "dungeon.status-idle",
                        "player", target.getName(), "dungeon", activeDungeonId.orElse(""));
            });
            default -> messages.send(sender, "usage.dungeon");
        }
        return true;
    }

    private void withTargetAndDungeon(CommandSender sender, String[] args, BiConsumer<Player, String> action) {
        if (args.length < 2) {
            messages.send(sender, "usage.dungeon");
            return;
        }
        String playerName;
        String dungeonId;
        if (args.length >= 3) {
            playerName = args[1];
            dungeonId = args[2];
        } else {
            playerName = sender instanceof Player self ? self.getName() : null;
            dungeonId = args[1];
        }
        Player target = resolveTarget(sender, playerName);
        if (target != null) {
            action.accept(target, dungeonId);
        }
    }

    private void withTargetOnly(CommandSender sender, String[] args, Consumer<Player> action) {
        String playerName = args.length >= 2 ? args[1] : (sender instanceof Player self ? self.getName() : null);
        Player target = resolveTarget(sender, playerName);
        if (target != null) {
            action.accept(target);
        }
    }

    private Player resolveTarget(CommandSender sender, String playerName) {
        if (playerName == null) {
            messages.send(sender, "command.player-only");
            return null;
        }
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", playerName);
            return null;
        }
        return target;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return TabCompletions.matching(SUBCOMMANDS, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("ids")) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        if (args.length == 3 && worldDebugApi != null
                && (args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("forcestart"))) {
            return TabCompletions.matching(worldDebugApi.listDungeonIds(), args[2]);
        }
        return List.of();
    }
}
