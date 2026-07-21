package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.api.StatusApi;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;

import java.util.List;

/** {@code /oladmin exp give [player] <amount>} - {@code player} defaults to the sender when omitted. */
public final class ExpDebugCommand implements CommandExecutor, TabCompleter {

    private final MessageManager messages;
    private final StatusApi statusApi;

    public ExpDebugCommand(MessageManager messages, StatusApi statusApi) {
        this.messages = messages;
        this.statusApi = statusApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("give")) {
            messages.send(sender, "usage.exp");
            return true;
        }
        String playerName;
        String amountRaw;
        if (args.length >= 3) {
            playerName = args[1];
            amountRaw = args[2];
        } else if (args.length == 2) {
            playerName = sender instanceof Player self ? self.getName() : null;
            amountRaw = args[1];
        } else {
            messages.send(sender, "usage.exp");
            return true;
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
        long amount;
        try {
            amount = Long.parseLong(amountRaw);
        } catch (NumberFormatException e) {
            messages.send(sender, "exp.invalid-amount");
            return true;
        }
        statusApi.addExperience(target.getUniqueId(), amount);
        messages.send(sender, "exp.given", "player", target.getName(), "amount", amount);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return TabCompletions.matching(List.of("give"), args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
