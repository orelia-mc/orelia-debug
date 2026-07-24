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

/**
 * {@code /oladmin title <list|grant|equip|unequip> [player] [title]} - {@code player} defaults
 * to the sender when omitted. Requires OreliaWorld (soft dependency).
 */
public final class TitleDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("list", "grant", "equip", "unequip");

    private final MessageManager messages;
    private final WorldDebugApi worldDebugApi;
    private final QuestApi questApi;

    public TitleDebugCommand(MessageManager messages, WorldDebugApi worldDebugApi, QuestApi questApi) {
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
            messages.send(sender, "usage.title");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> {
                String playerName = args.length >= 2 ? args[1] : (sender instanceof Player self ? self.getName() : null);
                Player target = resolveTarget(sender, playerName);
                if (target == null) {
                    return true;
                }
                var earned = questApi.getEarnedTitles(target.getUniqueId());
                String equipped = questApi.getEquippedTitle(target.getUniqueId()).orElse(null);
                if (earned.isEmpty()) {
                    messages.send(sender, "title.list-empty", "player", target.getName());
                    return true;
                }
                messages.send(sender, "title.list-header", "player", target.getName());
                for (String title : earned) {
                    boolean isEquipped = title.equals(equipped);
                    messages.send(sender, isEquipped ? "title.list-entry-equipped" : "title.list-entry", "title", title);
                }
            }
            case "grant" -> withTargetAndTitle(sender, args, (target, title) -> {
                boolean done = worldDebugApi.grantTitle(target.getUniqueId(), title);
                messages.send(sender, done ? "title.granted" : "title.grant-failed",
                        "player", target.getName(), "title", title);
            });
            case "equip" -> withTargetAndTitle(sender, args, (target, title) -> {
                boolean done = worldDebugApi.forceEquipTitle(target.getUniqueId(), title);
                messages.send(sender, done ? "title.equipped" : "title.equip-failed",
                        "player", target.getName(), "title", title);
            });
            case "unequip" -> {
                String playerName = args.length >= 2 ? args[1] : (sender instanceof Player self ? self.getName() : null);
                Player target = resolveTarget(sender, playerName);
                if (target == null) {
                    return true;
                }
                boolean done = worldDebugApi.unequipTitle(target.getUniqueId());
                messages.send(sender, done ? "title.unequipped" : "title.unequip-failed", "player", target.getName());
            }
            default -> messages.send(sender, "usage.title");
        }
        return true;
    }

    private void withTargetAndTitle(CommandSender sender, String[] args, BiConsumer<Player, String> action) {
        if (args.length < 2) {
            messages.send(sender, "usage.title");
            return;
        }
        String playerName;
        String title;
        if (args.length >= 3) {
            playerName = args[1];
            title = args[2];
        } else {
            playerName = sender instanceof Player self ? self.getName() : null;
            title = args[1];
        }
        Player target = resolveTarget(sender, playerName);
        if (target != null) {
            action.accept(target, title);
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
        if (args.length == 2) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
