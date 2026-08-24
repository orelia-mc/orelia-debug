package rpg.debug.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.core.command.Pagination;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;
import rpg.util.ColorUtil;
import rpg.world.api.QuestApi;
import rpg.world.api.WorldDebugApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code /oladmin quest <complete|start|resetcooldown|list|ids|defs|info> ...} - {@code player}
 * defaults to the sender when omitted (not applicable to {@code ids}/{@code defs}/{@code info}).
 * Requires OreliaWorld (soft dependency).
 */
public final class QuestDebugCommand implements CommandExecutor, TabCompleter {

    private static final int DEFS_PAGE_SIZE = 10;

    private static final List<String> SUBCOMMANDS =
            List.of("complete", "start", "resetcooldown", "list", "ids", "defs", "info");

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
        if (sub.equals("defs")) {
            String search = args.length >= 3 && args[1].equalsIgnoreCase("search") ? args[2] : null;
            int page = args.length >= 2 && search == null ? parsePageOrDefault(args[1]) : 1;
            showDefinitions(sender, search, page);
            return true;
        }
        if (sub.equals("info")) {
            if (args.length < 2) {
                messages.send(sender, "usage.quest");
                return true;
            }
            showDefinition(sender, args[1]);
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
                var progress = worldDebugApi.listActiveQuestProgress(target.getUniqueId());
                if (progress.isEmpty()) {
                    messages.send(sender, "quest.list-empty", "player", target.getName());
                    return;
                }
                messages.send(sender, "quest.list-header", "player", target.getName());
                for (WorldDebugApi.QuestProgressDetail quest : progress) {
                    messages.send(sender, "quest.list-state", "quest", quest.questName(), "state", quest.state());
                    for (WorldDebugApi.QuestObjectiveProgressInfo objective : quest.objectives()) {
                        messages.send(sender, "quest.list-objective", "type", objective.type(),
                                "target", objective.targetId(), "current", objective.current(), "required", objective.required());
                    }
                }
            });
            default -> messages.send(sender, "usage.quest");
        }
        return true;
    }

    /** {@code /oladmin quest defs [page]|defs search <term>} - a human-readable quest catalog, unlike {@code ids}' bare id list. */
    private void showDefinitions(CommandSender sender, String search, int page) {
        List<WorldDebugApi.QuestDefinition> all = worldDebugApi.listQuestDefinitions();
        List<WorldDebugApi.QuestDefinition> filtered = search == null ? all : all.stream()
                .filter(q -> q.id().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))
                        || q.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .toList();
        List<Component> lines = new ArrayList<>();
        for (WorldDebugApi.QuestDefinition quest : filtered) {
            String text = messages.format("quest.defs-entry", "id", quest.id(), "type", quest.type(),
                    "level", quest.requiredLevel(), "name", quest.name());
            lines.add(ColorUtil.componentWithCommand(text, "/oladmin quest info " + quest.id()));
        }
        String baseCommand = search == null ? "/oladmin quest defs" : "/oladmin quest defs search " + search;
        Pagination.send(sender, "&%6&lクエスト定義一覧&%7 ({page}/{total}ページ)", lines, DEFS_PAGE_SIZE, page,
                baseCommand, messages.raw("quest.defs-empty"));
    }

    /** {@code /oladmin quest info <questId>} - full static definition (objectives/rewards/prerequisites), unlike {@code list}'s live progress. */
    private void showDefinition(CommandSender sender, String questId) {
        WorldDebugApi.QuestDefinition quest = worldDebugApi.getQuestDefinition(questId).orElse(null);
        if (quest == null) {
            messages.send(sender, "quest.info-not-found", "questId", questId);
            return;
        }
        messages.send(sender, "quest.info-header", "type", quest.type(), "name", quest.name(), "id", quest.id());
        messages.send(sender, "quest.info-level", "level", quest.requiredLevel());
        messages.send(sender, "quest.info-repeatable", "repeatable", quest.repeatable());
        messages.send(sender, "quest.info-party-only", "partyOnly", quest.partyOnly());
        if (quest.repeatable() && quest.cooldownHours() > 0) {
            messages.send(sender, "quest.info-cooldown", "hours", quest.cooldownHours());
        }
        if (quest.prerequisiteQuestIds().isEmpty()) {
            messages.send(sender, "quest.info-prerequisites-none");
        } else {
            messages.send(sender, "quest.info-prerequisites", "prerequisites", String.join(", ", quest.prerequisiteQuestIds()));
        }
        messages.send(sender, "quest.info-objectives-header");
        for (WorldDebugApi.QuestObjectiveInfo objective : quest.objectives()) {
            messages.send(sender, "quest.info-objective-entry", "type", objective.type(),
                    "target", objective.targetId(), "amount", objective.requiredAmount());
        }
        messages.send(sender, "quest.info-reward-header");
        WorldDebugApi.QuestRewardInfo reward = quest.reward();
        if (reward.exp() > 0) {
            messages.send(sender, "quest.info-reward-exp", "exp", reward.exp());
        }
        if (reward.money() > 0) {
            messages.send(sender, "quest.info-reward-money", "money", reward.money());
        }
        if (reward.weaponId() != null && !reward.weaponId().isBlank()) {
            messages.send(sender, "quest.info-reward-weapon", "weaponId", reward.weaponId());
        }
        if (reward.accessoryId() != null && !reward.accessoryId().isBlank()) {
            messages.send(sender, "quest.info-reward-accessory", "accessoryId", reward.accessoryId());
        }
        if (reward.skillPoints() > 0) {
            messages.send(sender, "quest.info-reward-skillpoints", "points", reward.skillPoints());
        }
        if (reward.title() != null && !reward.title().isBlank()) {
            messages.send(sender, "quest.info-reward-title", "title", reward.title());
        }
        if (reward.vanillaMaterial() != null && !reward.vanillaMaterial().isBlank()) {
            messages.send(sender, "quest.info-reward-item", "material", reward.vanillaMaterial(), "amount", reward.vanillaAmount());
        }
    }

    private int parsePageOrDefault(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 1;
        }
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
        if (args.length == 2 && args[0].equalsIgnoreCase("info") && worldDebugApi != null) {
            return TabCompletions.matching(worldDebugApi.listQuestIds(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("defs")) {
            return TabCompletions.matching(List.of("search"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ids")) {
            return List.of();
        }
        if (args.length == 2) {
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
