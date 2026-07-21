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

/**
 * {@code /oladmin quest complete [player] <questId>} - {@code player} defaults to the sender
 * when omitted. Requires OreliaWorld (soft dependency).
 */
public final class QuestDebugCommand implements CommandExecutor, TabCompleter {

    private final MessageManager messages;
    private final WorldDebugApi worldDebugApi;

    public QuestDebugCommand(MessageManager messages, WorldDebugApi worldDebugApi) {
        this.messages = messages;
        this.worldDebugApi = worldDebugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (worldDebugApi == null) {
            messages.send(sender, "gui.world-not-installed");
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("complete")) {
            messages.send(sender, "usage.quest");
            return true;
        }
        String playerName;
        String questId;
        if (args.length >= 3) {
            playerName = args[1];
            questId = args[2];
        } else {
            playerName = sender instanceof Player self ? self.getName() : null;
            questId = args[1];
        }
        if (playerName == null) {
            messages.send(sender, "command.player-only");
            return true;
        }
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", playerName);
            return true;
        }
        boolean done = worldDebugApi.forceCompleteQuestObjectives(target.getUniqueId(), questId);
        if (done) {
            messages.send(sender, "quest.force-completed", "player", target.getName(), "quest", questId);
        } else {
            messages.send(sender, "quest.force-complete-failed");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return TabCompletions.matching(List.of("complete"), args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
