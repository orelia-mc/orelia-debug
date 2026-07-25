package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;
import rpg.extra.api.ExtraDebugApi;

import java.util.List;
import java.util.function.Consumer;

/**
 * {@code /oladmin trade <status|forcecancel> [player]} - {@code player} defaults to the
 * sender when omitted. Requires OreliaExtra (soft dependency). {@code forcecancel} is meant
 * to unstick a trade session (returns both sides' offered items), same effect as a
 * disconnect via {@code TradeService#forceCancelIfTrading}.
 */
public final class TradeDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("status", "forcecancel");

    private final MessageManager messages;
    private final ExtraDebugApi extraDebugApi;

    public TradeDebugCommand(MessageManager messages, ExtraDebugApi extraDebugApi) {
        this.messages = messages;
        this.extraDebugApi = extraDebugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (extraDebugApi == null) {
            messages.send(sender, "gui.extra-not-installed");
            return true;
        }
        if (args.length < 1) {
            messages.send(sender, "usage.trade");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "status" -> withTargetOnly(sender, args, target -> {
                var counterpart = extraDebugApi.getTradeCounterpart(target.getUniqueId());
                if (counterpart.isEmpty()) {
                    messages.send(sender, "trade.status-none", "player", target.getName());
                    return;
                }
                Player other = Bukkit.getPlayer(counterpart.get());
                messages.send(sender, "trade.status-active", "player", target.getName(),
                        "other", other != null ? other.getName() : counterpart.get().toString());
            });
            case "forcecancel" -> withTargetOnly(sender, args, target -> {
                boolean done = extraDebugApi.forceCancelTrade(target.getUniqueId());
                messages.send(sender, done ? "trade.force-cancelled" : "trade.force-cancel-failed", "player", target.getName());
            });
            default -> messages.send(sender, "usage.trade");
        }
        return true;
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
        if (args.length == 2) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
