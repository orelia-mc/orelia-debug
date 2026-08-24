package rpg.debug.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import rpg.api.DebugApi;
import rpg.core.command.Pagination;
import rpg.core.command.TabCompletions;
import rpg.core.message.MessageManager;
import rpg.extra.api.ExtraDebugApi;
import rpg.util.ColorUtil;
import rpg.world.api.WorldDebugApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /oladmin config <core|world|extra> <list|view [file] [path]|get <file> <path>|set <file> <path> <value>|save <file>>}
 */
public final class ConfigDebugCommand implements CommandExecutor, TabCompleter {

    private static final List<String> CONFIG_TARGETS = List.of("core", "world", "extra");
    private static final int VIEW_PAGE_SIZE = 15;

    /**
     * Target-agnostic view of one {@code DebugApi.ConfigTreeEntry}/{@code WorldDebugApi.ConfigTreeEntry}/
     * {@code ExtraDebugApi.ConfigTreeEntry} node - the three are independent record types (each
     * plugin's own {@code rpg.*.api} package, no shared supertype), so {@link #configView} maps
     * whichever one it got into this common shape before rendering.
     */
    private record ViewEntry(String path, int depth, String label, String value, boolean isLeaf) {
    }

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
            case "view" -> {
                if (args.length < 3) {
                    messages.send(sender, "usage.config");
                    return true;
                }
                String path = args.length >= 4 && !isPageNumber(args[3]) ? args[3] : null;
                int pageArgIndex = path != null ? 4 : 3;
                int page = args.length > pageArgIndex ? parsePageOrDefault(args[pageArgIndex]) : 1;
                configView(sender, target, args[2], path, page);
            }
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

    /**
     * Human-readable indented tree view of {@code file} (or just the subtree under {@code path}
     * if given), each leaf clickable to pre-fill {@code /oladmin config <target> set <file>
     * <path> <value> } in the viewer's chat bar (same {@code ClickEvent.suggestCommand} pattern
     * as {@code AdminCommand}'s {@code /oladmin spawnpoint list}), each section header clickable
     * to drill into just that subtree. Unlike {@link #configList}'s flat sorted dot-path dump
     * ({@code describeConfigKeys}), this walks {@code listConfigTree} in on-disk order so
     * siblings under the same parent stay grouped together.
     */
    private void configView(CommandSender sender, String target, String file, String pathFilter, int page) {
        List<ViewEntry> tree = switch (target) {
            case "core" -> mapCore(coreDebugApi.listConfigTree(file));
            case "world" -> worldDebugApi != null ? mapWorld(worldDebugApi.listConfigTree(file)) : null;
            case "extra" -> extraDebugApi != null ? mapExtra(extraDebugApi.listConfigTree(file)) : null;
            default -> null;
        };
        if (tree == null) {
            reportMissingPlugin(sender, target);
            return;
        }
        if (tree.isEmpty()) {
            messages.send(sender, "config.keys-empty");
            return;
        }
        List<ViewEntry> scoped = pathFilter == null ? tree : tree.stream()
                .filter(e -> e.path().equals(pathFilter) || e.path().startsWith(pathFilter + "."))
                .toList();
        if (scoped.isEmpty()) {
            messages.send(sender, "config.value-not-found", "file", file, "path", pathFilter);
            return;
        }
        List<Component> lines = new ArrayList<>();
        for (ViewEntry entry : scoped) {
            lines.add(renderViewEntry(target, file, entry));
        }
        String baseCommand = "/oladmin config " + target + " view " + file + (pathFilter != null ? " " + pathFilter : "");
        Pagination.send(sender, "&%6&lconfig: " + file + (pathFilter != null ? " &%7(" + pathFilter + ")" : "") + "&%7 ({page}/{total}ページ)",
                lines, VIEW_PAGE_SIZE, page, baseCommand);
    }

    private Component renderViewEntry(String target, String file, ViewEntry entry) {
        String indent = "  ".repeat(entry.depth());
        if (entry.isLeaf()) {
            String display = indent + "&%f" + entry.label() + "&%7: &%e" + entry.value();
            String editCommand = "/oladmin config " + target + " set " + file + " " + entry.path() + " " + entry.value() + " ";
            return ColorUtil.componentWithSuggestCommand(display, editCommand)
                    .hoverEvent(HoverEvent.showText(ColorUtil.component("&%7クリックして編集コマンドを入力欄に挿入")));
        }
        String display = indent + "&%6&l" + entry.label() + "&%7:";
        String drillCommand = "/oladmin config " + target + " view " + file + " " + entry.path();
        return ColorUtil.componentWithCommand(display, drillCommand)
                .hoverEvent(HoverEvent.showText(ColorUtil.component("&%7クリックしてこの階層だけ表示")));
    }

    private List<ViewEntry> mapCore(List<DebugApi.ConfigTreeEntry> entries) {
        return entries.stream().map(e -> new ViewEntry(e.path(), e.depth(), e.label(), e.value(), e.isLeaf())).toList();
    }

    private List<ViewEntry> mapWorld(List<WorldDebugApi.ConfigTreeEntry> entries) {
        return entries.stream().map(e -> new ViewEntry(e.path(), e.depth(), e.label(), e.value(), e.isLeaf())).toList();
    }

    private List<ViewEntry> mapExtra(List<ExtraDebugApi.ConfigTreeEntry> entries) {
        return entries.stream().map(e -> new ViewEntry(e.path(), e.depth(), e.label(), e.value(), e.isLeaf())).toList();
    }

    private boolean isPageNumber(String raw) {
        try {
            Integer.parseInt(raw);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int parsePageOrDefault(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 1;
        }
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
            return TabCompletions.matching(List.of("list", "view", "get", "set", "save"), args[1]);
        }
        if (args.length == 3 && List.of("view", "get", "set", "save").contains(args[1].toLowerCase())) {
            Set<String> files = switch (args[0].toLowerCase()) {
                case "core" -> coreDebugApi.listConfigFiles();
                case "world" -> worldDebugApi != null ? worldDebugApi.listConfigFiles() : Set.<String>of();
                case "extra" -> extraDebugApi != null ? extraDebugApi.listConfigFiles() : Set.<String>of();
                default -> Set.<String>of();
            };
            return TabCompletions.matching(files.stream().sorted().toList(), args[2]);
        }
        return List.of();
    }
}
