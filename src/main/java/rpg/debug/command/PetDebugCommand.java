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
 * {@code /oladmin pet <unlock> [player] <petId>|list [player]|ids} - {@code player} defaults
 * to the sender when omitted (not applicable to {@code ids}). Requires OreliaExtra (soft
 * dependency). {@code unlock} bypasses the economy check {@code PetService#unlock} normally
 * enforces.
 */
public final class PetDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("unlock", "list", "ids");

    private final MessageManager messages;
    private final ExtraDebugApi extraDebugApi;

    public PetDebugCommand(MessageManager messages, ExtraDebugApi extraDebugApi) {
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
            messages.send(sender, "usage.pet");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("ids")) {
            messages.send(sender, "pet.ids-header");
            for (String petId : extraDebugApi.listPetIds()) {
                messages.send(sender, "pet.ids-entry", "pet", petId);
            }
            return true;
        }
        switch (sub) {
            case "unlock" -> withTargetAndPet(sender, args, (target, petId) -> {
                boolean done = extraDebugApi.forceUnlockPet(target.getUniqueId(), petId);
                messages.send(sender, done ? "pet.force-unlocked" : "pet.force-unlock-failed",
                        "player", target.getName(), "pet", petId);
            });
            case "list" -> withTargetOnly(sender, args, target -> {
                var unlocked = extraDebugApi.getUnlockedPets(target.getUniqueId());
                if (unlocked.isEmpty()) {
                    messages.send(sender, "pet.list-empty", "player", target.getName());
                    return;
                }
                messages.send(sender, "pet.list-header", "player", target.getName());
                for (String petId : unlocked) {
                    messages.send(sender, "pet.list-entry", "pet", petId);
                }
            });
            default -> messages.send(sender, "usage.pet");
        }
        return true;
    }

    private void withTargetAndPet(CommandSender sender, String[] args, BiConsumer<Player, String> action) {
        if (args.length < 2) {
            messages.send(sender, "usage.pet");
            return;
        }
        String playerName;
        String petId;
        if (args.length >= 3) {
            playerName = args[1];
            petId = args[2];
        } else {
            playerName = sender instanceof Player self ? self.getName() : null;
            petId = args[1];
        }
        Player target = resolveTarget(sender, playerName);
        if (target != null) {
            action.accept(target, petId);
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
        if (args.length == 3 && extraDebugApi != null && args[0].equalsIgnoreCase("unlock")) {
            return TabCompletions.matching(extraDebugApi.listPetIds(), args[2]);
        }
        return List.of();
    }
}
