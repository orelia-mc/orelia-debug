package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.api.DebugApi;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;

import java.util.List;

/**
 * {@code /oladmin debugmode <on|off|toggle> [player]} - {@code player} defaults to the
 * sender when omitted. Flips {@link DebugApi#setDebugMode}, bypassing weapon/skill
 * requirement checks for that player while enabled.
 */
public final class DebugModeCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("on", "off", "toggle");

    private final MessageManager messages;
    private final DebugApi debugApi;

    public DebugModeCommand(MessageManager messages, DebugApi debugApi) {
        this.messages = messages;
        this.debugApi = debugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "usage.debugmode");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (!sub.equals("on") && !sub.equals("off") && !sub.equals("toggle")) {
            messages.send(sender, "usage.debugmode");
            return true;
        }

        String playerName = args.length >= 2 ? args[1] : (sender instanceof Player self ? self.getName() : null);
        if (playerName == null) {
            messages.send(sender, "command.player-only");
            return true;
        }
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", playerName);
            return true;
        }

        boolean enabled = switch (sub) {
            case "on" -> true;
            case "off" -> false;
            default -> !debugApi.isDebugMode(target.getUniqueId());
        };
        boolean done = debugApi.setDebugMode(target.getUniqueId(), enabled);
        if (!done) {
            messages.send(sender, "debugmode.failed", "player", target.getName());
            return true;
        }
        messages.send(sender, enabled ? "debugmode.enabled" : "debugmode.disabled", "player", target.getName());
        return true;
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
