package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.api.SkillApi;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;

import java.util.List;
import java.util.UUID;

/**
 * {@code /oladmin skillpoints <give|set|take> [player] <amount>} - {@code player} defaults to
 * the sender when omitted. Manages the "スキル習得ポイント" balance spent in the weapon-skill
 * GUI to learn/upgrade skills (not the in-combat SP a skill costs to cast - see
 * {@code rpg.skill.model.SkillData#getSpCost}).
 */
public final class SkillPointsDebugCommand implements CommandExecutor, TabCompleter {

    private final MessageManager messages;
    private final SkillApi skillApi;

    public SkillPointsDebugCommand(MessageManager messages, SkillApi skillApi) {
        this.messages = messages;
        this.skillApi = skillApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "usage.skillpoints");
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
        int amount;
        try {
            amount = Integer.parseInt(amountRaw);
        } catch (NumberFormatException e) {
            messages.send(sender, "skillpoints.invalid-amount");
            return true;
        }
        UUID uuid = target.getUniqueId();
        switch (args[0].toLowerCase()) {
            case "give" -> {
                skillApi.grantSkillPoints(uuid, amount);
                messages.send(sender, "skillpoints.given", "player", target.getName(), "amount", amount, "balance", skillApi.getSkillPoints(uuid));
            }
            case "set" -> {
                skillApi.setSkillPoints(uuid, amount);
                messages.send(sender, "skillpoints.set", "player", target.getName(), "amount", amount);
            }
            case "take" -> {
                if (skillApi.takeSkillPoints(uuid, amount)) {
                    messages.send(sender, "skillpoints.taken", "player", target.getName(), "amount", amount, "balance", skillApi.getSkillPoints(uuid));
                } else {
                    messages.send(sender, "skillpoints.take-failed");
                }
            }
            default -> messages.send(sender, "usage.skillpoints");
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
