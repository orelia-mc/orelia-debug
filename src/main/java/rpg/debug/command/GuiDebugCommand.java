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
import rpg.world.api.WorldDebugApi;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /oladmin gui <status|equipment|skill|job|shop|warehouse|crafting|auction|mail|ranking|house|pet|achievement|dungeon> [player]}
 * - forces open the given GUI screen for {@code player} (defaults to the sender if omitted).
 * {@link #worldDebugApi}/{@link #extraDebugApi} are {@code null} when OreliaWorld/OreliaExtra
 * aren't installed (soft dependencies).
 */
public final class GuiDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> CORE_GUI_SCREENS =
            List.of("status", "equipment", "skill", "job", "shop", "warehouse", "crafting");
    private static final List<String> EXTRA_GUI_SCREENS =
            List.of("auction", "mail", "ranking", "house", "pet", "achievement");
    private static final List<String> WORLD_GUI_SCREENS = List.of("dungeon");

    private final MessageManager messages;
    private final GuiApi guiApi;
    private final WorldDebugApi worldDebugApi;
    private final ExtraDebugApi extraDebugApi;

    public GuiDebugCommand(MessageManager messages, GuiApi guiApi, WorldDebugApi worldDebugApi, ExtraDebugApi extraDebugApi) {
        this.messages = messages;
        this.guiApi = guiApi;
        this.worldDebugApi = worldDebugApi;
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
                case "crafting" -> guiApi.openCrafting(target);
                default -> throw new IllegalStateException("unreachable: " + screen);
            }
        } else if (WORLD_GUI_SCREENS.contains(screen)) {
            if (worldDebugApi == null) {
                messages.send(sender, "gui.world-not-installed");
                return true;
            }
            switch (screen) {
                case "dungeon" -> worldDebugApi.openDungeon(target);
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
                case "house" -> extraDebugApi.openHouse(target);
                case "pet" -> extraDebugApi.openPet(target);
                case "achievement" -> extraDebugApi.openAchievement(target);
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
            screens.addAll(WORLD_GUI_SCREENS);
            screens.addAll(EXTRA_GUI_SCREENS);
            return TabCompletions.matching(screens, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2) {
            return TabCompletions.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
