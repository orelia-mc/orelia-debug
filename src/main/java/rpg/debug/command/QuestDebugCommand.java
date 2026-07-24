package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;
import rpg.world.api.QuestApi;
import rpg.world.api.WorldDebugApi;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code /oladmin quest <complete|start|resetcooldown|list|ids> ...} - {@code player} defaults
 * to the sender when omitted (not applicable to {@code ids}). Requires OreliaWorld (soft
 * dependency).
 */
public final class QuestDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("complete", "start", "resetcooldown", "list", "ids");

    private final MessageManager messages;
    private final WorldDebugApi worldDebugApi;
    private final QuestApi questApi;

    public QuestDebugCommand(MessageManager messages, WorldDebugApi worldDebugApi, QuestApi questApi) {
        this.messages = messages;
        this.worldDebugApi = worldDebugApi;
        this.questApi = questApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (worldDebugApi == null) {
            messages.send(sender, "gui.world-not-installed");
            return true;
        }
        if (args.length < 1) {
            messages.send(sender, "usage.quest");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("ids")) {
            messages.send(sender, "quest.ids-header");
            for (String questId : worldDebugApi.listQuestIds()) {
                messages.send(sender, "quest.ids-entry", "quest", questId);
            }
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage.quest");
            return true;
        }
        switch (sub) {
            case "complete" -> withTargetAndQuest(sender, args, (target, questId) -> {
                boolean done = worldDebugApi.forceCompleteQuestObjectives(target.getUniqueId(), questId);
                messages.send(sender, done ? "quest.force-completed" : "quest.force-complete-failed",
                        "player", target.getName(), "quest", questId);
            });
            case "start" -> withTargetAndQuest(sender, args, (target, questId) -> {
                boolean done = worldDebugApi.forceStartQuest(target.getUniqueId(), questId);
                messages.send(sender, done ? "quest.force-started" : "quest.force-start-failed",
                        "player", target.getName(), "quest", questId);
            });
            case "resetcooldown" -> withTargetAndQuest(sender, args, (target, questId) -> {
                boolean done = worldDebugApi.resetQuestCompletion(target.getUniqueId(), questId);
                messages.send(sender, done ? "quest.cooldown-reset" : "quest.cooldown-reset-failed",
                        "player", target.getName(), "quest", questId);
            });
            case "list" -> withTargetOnly(sender, args, target -> {
                var activeQuestIds = questApi.getActiveQuestIds(target.getUniqueId());
                if (activeQuestIds.isEmpty()) {
                    messages.send(sender, "quest.list-empty", "player", target.getName());
                    return;
                }
                messages.send(sender, "quest.list-header", "player", target.getName());
                for (String questId : activeQuestIds) {
                    messages.send(sender, "quest.list-entry", "quest", questId);
                }
            });
            default -> messages.send(sender, "usage.quest");
        }
        return true;
    }

    private void withTargetAndQuest(CommandSender sender, String[] args, BiConsumer<Player, String> action) {
        String playerName;
        String questId;
        if (args.length >= 3) {
            playerName = args[1];
            questId = args[2];
        } else {
            playerName = sender instanceof Player self ? self.getName() : null;
            questId = args[1];
        }
        Player target = resolveTarget(sender, playerName);
        if (target != null) {
            action.accept(target, questId);
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
                && (args[0].equalsIgnoreCase("complete") || args[0].equalsIgnoreCase("start")
                    || args[0].equalsIgnoreCase("resetcooldown"))) {
            return TabCompletions.matching(worldDebugApi.listQuestIds(), args[2]);
        }
        return List.of();
    }
}
