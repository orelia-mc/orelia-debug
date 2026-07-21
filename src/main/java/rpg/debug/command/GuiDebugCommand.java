package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.api.GuiApi;
import rpg.core.command.CommandArgs;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;
import rpg.extra.api.ExtraDebugApi;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /oladmin gui <status|equipment|skill|job|shop|warehouse|auction|mail|ranking> [player]}
 * - forces open the given GUI screen for {@code player} (defaults to the sender if omitted).
 * {@link #extraDebugApi} is {@code null} when OreliaExtra isn't installed (soft dependency).
 */
public final class GuiDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> CORE_GUI_SCREENS = List.of("status", "equipment", "skill", "job", "shop", "warehouse");
    private static final List<String> EXTRA_GUI_SCREENS = List.of("auction", "mail", "ranking");

    private final MessageManager messages;
    private final GuiApi guiApi;
    private final ExtraDebugApi extraDebugApi;

    public GuiDebugCommand(MessageManager messages, GuiApi guiApi, ExtraDebugApi extraDebugApi) {
        this.messages = messages;
        this.guiApi = guiApi;
        this.extraDebugApi = extraDebugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            messages.send(sender, "usage.gui");
            return true;
        }
        String playerName = CommandArgs.resolvePlayerName(sender, args, 1);
        Player target = playerName == null ? null : Bukkit.getPlayerExact(playerName);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", playerName == null ? "" : playerName);
            return true;
        }
        String screen = args[0].toLowerCase();
        if (CORE_GUI_SCREENS.contains(screen)) {
            switch (screen) {
                case "status" -> guiApi.openStatus(target);
                case "equipment" -> guiApi.openEquipment(target);
                case "skill" -> guiApi.openSkill(target);
                case "job" -> guiApi.openJobChange(target);
                case "warehouse" -> guiApi.openWarehouse(target);
                case "shop" -> guiApi.openShop(target, List.of());
                default -> throw new IllegalStateException("unreachable: " + screen);
            }
        } else if (EXTRA_GUI_SCREENS.contains(screen)) {
            if (extraDebugApi == null) {
                messages.send(sender, "gui.extra-not-installed");
                return true;
            }
            switch (screen) {
                case "auction" -> extraDebugApi.openAuction(target);
                case "mail" -> extraDebugApi.openMail(target);
                case "ranking" -> extraDebugApi.openRanking(target);
                default -> throw new IllegalStateException("unreachable: " + screen);
            }
        } else {
            messages.send(sender, "gui.unsupported", "screen", screen);
            return true;
        }
        messages.send(sender, "gui.opened", "player", target.getName(), "screen", screen);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            List<String> screens = new ArrayList<>(CORE_GUI_SCREENS);
            screens.addAll(EXTRA_GUI_SCREENS);
            return TabCompletions.matching(screens, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
