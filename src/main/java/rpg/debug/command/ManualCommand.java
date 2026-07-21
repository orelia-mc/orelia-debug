package rpg.debug.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** {@code /oladmin manual [page]} - paginated in-game command reference. */
public final class ManualCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DebugManual.send(sender, args.length >= 1 ? args[0] : "1");
        return true;
    }
}
