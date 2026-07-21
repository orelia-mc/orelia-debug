package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.api.EconomyApi;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;

import java.util.List;
import java.util.UUID;

/**
 * {@code /oladmin money <give|set|take> [player] <amount>} - {@code player} defaults to the
 * sender when omitted.
 */
public final class MoneyDebugCommand implements CommandExecutor, TabCompleter {

    private final MessageManager messages;
    private final EconomyApi economyApi;

    public MoneyDebugCommand(MessageManager messages, EconomyApi economyApi) {
        this.messages = messages;
        this.economyApi = economyApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "usage.money");
            return true;
        }
        String playerName;
        String amountRaw;
        if (args.length >= 3) {
            playerName = args[1];
            amountRaw = args[2];
        } else {
            playerName = sender instanceof Player self ? self.getName() : null;
            amountRaw = args[1];
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
        double amount;
        try {
            amount = Double.parseDouble(amountRaw);
        } catch (NumberFormatException e) {
            messages.send(sender, "money.invalid-amount");
            return true;
        }
        UUID uuid = target.getUniqueId();
        switch (args[0].toLowerCase()) {
            case "give" -> {
                economyApi.deposit(uuid, amount);
                messages.send(sender, "money.given", "player", target.getName(), "amount", amount, "balance", economyApi.getBalance(uuid));
            }
            case "set" -> {
                economyApi.setBalance(uuid, amount);
                messages.send(sender, "money.set", "player", target.getName(), "amount", amount);
            }
            case "take" -> {
                if (economyApi.withdraw(uuid, amount)) {
                    messages.send(sender, "money.taken", "player", target.getName(), "amount", amount, "balance", economyApi.getBalance(uuid));
                } else {
                    messages.send(sender, "money.take-failed");
                }
            }
            default -> messages.send(sender, "usage.money");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return TabCompletions.matching(List.of("give", "set", "take"), args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
