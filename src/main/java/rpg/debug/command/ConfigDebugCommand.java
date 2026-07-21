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
import java.util.Optional;
import java.util.Set;

/**
 * {@code /oladmin config <core|world|extra> <list|get <file> <path>|set <file> <path> <value>|save <file>>}
 */
public final class ConfigDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> CONFIG_TARGETS = List.of("core", "world", "extra");

    private final MessageManager messages;
    private final DebugApi coreDebugApi;
    private final WorldDebugApi worldDebugApi;
    private final ExtraDebugApi extraDebugApi;

    public ConfigDebugCommand(MessageManager messages, DebugApi coreDebugApi, WorldDebugApi worldDebugApi, ExtraDebugApi extraDebugApi) {
        this.messages = messages;
        this.coreDebugApi = coreDebugApi;
        this.worldDebugApi = worldDebugApi;
        this.extraDebugApi = extraDebugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "usage.config");
            return true;
        }
        String target = args[0].toLowerCase();
        if (!CONFIG_TARGETS.contains(target)) {
            messages.send(sender, "config.unknown-target", "target", target);
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> configList(sender, target);
            case "get" -> {
                if (args.length < 4) {
                    messages.send(sender, "usage.config");
                    return true;
                }
                configGet(sender, target, args[2], args[3]);
            }
            case "set" -> {
                if (args.length < 5) {
                    messages.send(sender, "usage.config");
                    return true;
                }
                configSet(sender, target, args[2], args[3], args[4]);
            }
            case "save" -> {
                if (args.length < 3) {
                    messages.send(sender, "usage.config");
                    return true;
                }
                configSave(sender, target, args[2]);
            }
            default -> messages.send(sender, "usage.config");
        }
        return true;
    }

    private void configList(CommandSender sender, String target) {
        Set<String> files = switch (target) {
            case "core" -> coreDebugApi.listConfigFiles();
            case "world" -> worldDebugApi != null ? worldDebugApi.listConfigFiles() : null;
            case "extra" -> extraDebugApi != null ? extraDebugApi.listConfigFiles() : null;
            default -> null;
        };
        if (files == null) {
            reportMissingPlugin(sender, target);
            return;
        }
        messages.send(sender, "config.file-list-header", "target", target);
        files.stream().sorted().forEach(f -> messages.sendRaw(sender, "config.file-list-entry", "file", f));
    }

    private void configGet(CommandSender sender, String target, String file, String path) {
        Optional<String> value = switch (target) {
            case "core" -> coreDebugApi.getConfigValue(file, path);
            case "world" -> worldDebugApi != null ? worldDebugApi.getConfigValue(file, path) : null;
            case "extra" -> extraDebugApi != null ? extraDebugApi.getConfigValue(file, path) : null;
            default -> null;
        };
        if (value == null) {
            reportMissingPlugin(sender, target);
            return;
        }
        if (value.isEmpty()) {
            messages.send(sender, "config.value-not-found", "file", file, "path", path);
            return;
        }
        messages.send(sender, "config.value", "file", file, "path", path, "value", value.get());
    }

    private void configSet(CommandSender sender, String target, String file, String path, String rawValue) {
        Boolean success = switch (target) {
            case "core" -> coreDebugApi.setConfigValue(file, path, rawValue);
            case "world" -> worldDebugApi != null ? worldDebugApi.setConfigValue(file, path, rawValue) : null;
            case "extra" -> extraDebugApi != null ? extraDebugApi.setConfigValue(file, path, rawValue) : null;
            default -> null;
        };
        if (success == null) {
            reportMissingPlugin(sender, target);
            return;
        }
        if (success) {
            messages.send(sender, "config.set-success", "file", file, "path", path, "value", rawValue);
        } else {
            messages.send(sender, "config.set-failed", "file", file);
        }
    }

    private void configSave(CommandSender sender, String target, String file) {
        switch (target) {
            case "core" -> coreDebugApi.saveConfig(file);
            case "world" -> {
                if (worldDebugApi == null) {
                    reportMissingPlugin(sender, target);
                    return;
                }
                worldDebugApi.saveConfig(file);
            }
            case "extra" -> {
                if (extraDebugApi == null) {
                    reportMissingPlugin(sender, target);
                    return;
                }
                extraDebugApi.saveConfig(file);
            }
            default -> {
                return;
            }
        }
        messages.send(sender, "config.saved", "file", file);
    }

    private void reportMissingPlugin(CommandSender sender, String target) {
        messages.send(sender, "world".equals(target) ? "gui.world-not-installed" : "gui.extra-not-installed");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return TabCompletions.matching(CONFIG_TARGETS, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2) {
            return TabCompletions.matching(List.of("list", "get", "set", "save"), args[1]);
        }
        return List.of();
    }
}
