package rpg.debug.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import rpg.api.DebugApi;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;
import rpg.extra.api.ExtraDebugApi;
import rpg.world.api.WorldDebugApi;

import java.util.List;

/** {@code /oladmin confighelp <core|world|extra> <file>} - lists every configurable key in {@code file}. */
public final class ConfigHelpDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> CONFIG_TARGETS = List.of("core", "world", "extra");

    private final MessageManager messages;
    private final DebugApi coreDebugApi;
    private final WorldDebugApi worldDebugApi;
    private final ExtraDebugApi extraDebugApi;

    public ConfigHelpDebugCommand(MessageManager messages, DebugApi coreDebugApi, WorldDebugApi worldDebugApi, ExtraDebugApi extraDebugApi) {
        this.messages = messages;
        this.coreDebugApi = coreDebugApi;
        this.worldDebugApi = worldDebugApi;
        this.extraDebugApi = extraDebugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "usage.confighelp");
            return true;
        }
        String target = args[0].toLowerCase();
        if (!CONFIG_TARGETS.contains(target)) {
            messages.send(sender, "config.unknown-target", "target", target);
            return true;
        }
        String file = args[1];
        List<String> keys = switch (target) {
            case "core" -> coreDebugApi.describeConfigKeys(file);
            case "world" -> worldDebugApi != null ? worldDebugApi.describeConfigKeys(file) : null;
            case "extra" -> extraDebugApi != null ? extraDebugApi.describeConfigKeys(file) : null;
            default -> null;
        };
        if (keys == null) {
            messages.send(sender, "world".equals(target) ? "gui.world-not-installed" : "gui.extra-not-installed");
            return true;
        }
        if (keys.isEmpty()) {
            messages.send(sender, "config.keys-empty");
            return true;
        }
        messages.send(sender, "config.keys-header", "target", target, "file", file);
        keys.forEach(key -> messages.sendRaw(sender, "config.file-list-entry", "file", key));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return TabCompletions.matching(CONFIG_TARGETS, args.length == 0 ? "" : args[0]);
        }
        return List.of();
    }
}
