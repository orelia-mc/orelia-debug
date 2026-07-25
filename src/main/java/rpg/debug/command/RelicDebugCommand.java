package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rpg.api.RelicApi;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;

import java.util.List;
import java.util.Optional;

/** {@code /oladmin relic give [player] <dungeonId>} - {@code player} defaults to the sender when omitted. */
public final class RelicDebugCommand implements CommandExecutor, TabCompleter {

    private final MessageManager messages;
    private final RelicApi relicApi;

    public RelicDebugCommand(MessageManager messages, RelicApi relicApi) {
        this.messages = messages;
        this.relicApi = relicApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("give")) {
            messages.send(sender, "usage.relic");
            return true;
        }
        String playerName;
        String dungeonId;
        if (args.length >= 3) {
            playerName = args[1];
            dungeonId = args[2];
        } else if (args.length == 2) {
            playerName = sender instanceof Player self ? self.getName() : null;
            dungeonId = args[1];
        } else {
            messages.send(sender, "usage.relic");
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
        Optional<ItemStack> relic = relicApi.generateRelic(dungeonId);
        if (relic.isEmpty()) {
            messages.send(sender, "relic.give-failed");
            return true;
        }
        target.getInventory().addItem(relic.get());
        messages.send(sender, "relic.given", "player", target.getName(), "dungeon", dungeonId);
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
