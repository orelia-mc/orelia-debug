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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * {@code /oladmin house <grant> [player] <plotId>|clear [player]|status [player]|ids} -
 * {@code player} defaults to the sender when omitted (not applicable to {@code ids}).
 * Requires OreliaExtra (soft dependency). {@code grant} bypasses the economy check
 * {@code HousingService#purchase} normally enforces; {@code clear} releases whatever plot the
 * target owns so it can be re-purchased for testing.
 */
public final class HouseDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("grant", "clear", "status", "ids");

    private final MessageManager messages;
    private final ExtraDebugApi extraDebugApi;

    public HouseDebugCommand(MessageManager messages, ExtraDebugApi extraDebugApi) {
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
            messages.send(sender, "usage.house");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("ids")) {
            messages.send(sender, "house.ids-header");
            for (String plotId : extraDebugApi.listHousePlotIds()) {
                messages.send(sender, "house.ids-entry", "plot", plotId);
            }
            return true;
        }
        switch (sub) {
            case "grant" -> withTargetAndPlot(sender, args, (target, plotId) -> {
                boolean done = extraDebugApi.forceGrantPlot(target, plotId);
                messages.send(sender, done ? "house.force-granted" : "house.force-grant-failed",
                        "player", target.getName(), "plot", plotId);
            });
            case "clear" -> withTargetOnly(sender, args, target -> {
                boolean done = extraDebugApi.releasePlot(target.getUniqueId());
                messages.send(sender, done ? "house.cleared" : "house.clear-failed", "player", target.getName());
            });
            case "status" -> withTargetOnly(sender, args, target -> {
                var ownedPlotId = extraDebugApi.getOwnedPlotId(target.getUniqueId());
                messages.send(sender, ownedPlotId.isPresent() ? "house.status-owns" : "house.status-none",
                        "player", target.getName(), "plot", ownedPlotId.orElse(""));
            });
            default -> messages.send(sender, "usage.house");
        }
        return true;
    }

    private void withTargetAndPlot(CommandSender sender, String[] args, BiConsumer<Player, String> action) {
        if (args.length < 2) {
            messages.send(sender, "usage.house");
            return;
        }
        String playerName;
        String plotId;
        if (args.length >= 3) {
            playerName = args[1];
            plotId = args[2];
        } else {
            playerName = sender instanceof Player self ? self.getName() : null;
            plotId = args[1];
        }
        Player target = resolveTarget(sender, playerName);
        if (target != null) {
            action.accept(target, plotId);
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
        if (args.length == 3 && extraDebugApi != null && args[0].equalsIgnoreCase("grant")) {
            return TabCompletions.matching(extraDebugApi.listHousePlotIds(), args[2]);
        }
        return List.of();
    }
}
