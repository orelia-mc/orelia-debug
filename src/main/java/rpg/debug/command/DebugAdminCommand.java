package rpg.debug.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import rpg.api.DebugApi;
import rpg.api.EconomyApi;
import rpg.api.GuiApi;
import rpg.api.StatusApi;
import rpg.core.message.MessageManager;
import rpg.extra.api.ExtraDebugApi;
import rpg.world.api.WorldDebugApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /oladmin debug <gui|money|config|confighelp|quest|npc|exp|manual> ...} - the single
 * entry point for every testplay/debug shortcut this plugin offers. {@link #worldDebugApi}/
 * {@link #extraDebugApi} are {@code null} when OreliaWorld/OreliaExtra aren't installed
 * (soft dependencies) - every branch that needs them reports "not installed" instead of NPE.
 */
public final class DebugAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("gui", "money", "config", "confighelp", "quest", "npc", "exp", "manual");
    private static final List<String> CORE_GUI_SCREENS = List.of("status", "equipment", "skill", "job", "shop", "warehouse");
    private static final List<String> EXTRA_GUI_SCREENS = List.of("auction", "mail", "ranking");
    private static final List<String> CONFIG_TARGETS = List.of("core", "world", "extra");

    private final MessageManager messages;
    private final DebugApi coreDebugApi;
    private final GuiApi guiApi;
    private final EconomyApi economyApi;
    private final StatusApi statusApi;
    private final WorldDebugApi worldDebugApi;
    private final ExtraDebugApi extraDebugApi;

    public DebugAdminCommand(MessageManager messages, DebugApi coreDebugApi, GuiApi guiApi, EconomyApi economyApi,
                              StatusApi statusApi, WorldDebugApi worldDebugApi, ExtraDebugApi extraDebugApi) {
        this.messages = messages;
        this.coreDebugApi = coreDebugApi;
        this.guiApi = guiApi;
        this.economyApi = economyApi;
        this.statusApi = statusApi;
        this.worldDebugApi = worldDebugApi;
        this.extraDebugApi = extraDebugApi;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "usage.root");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "gui" -> gui(sender, args);
            case "money" -> money(sender, args);
            case "config" -> config(sender, args);
            case "confighelp" -> confighelp(sender, args);
            case "quest" -> quest(sender, args);
            case "npc" -> npc(sender, args);
            case "exp" -> exp(sender, args);
            case "manual" -> DebugManual.send(sender, messages, args.length >= 2 ? args[1] : "1");
            default -> messages.send(sender, "usage.root");
        }
        return true;
    }

    private void gui(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "usage.gui");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", args[2]);
            return;
        }
        String screen = args[1].toLowerCase();
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
                return;
            }
            switch (screen) {
                case "auction" -> extraDebugApi.openAuction(target);
                case "mail" -> extraDebugApi.openMail(target);
                case "ranking" -> extraDebugApi.openRanking(target);
                default -> throw new IllegalStateException("unreachable: " + screen);
            }
        } else {
            messages.send(sender, "gui.unsupported", "screen", screen);
            return;
        }
        messages.send(sender, "gui.opened", "player", target.getName(), "screen", screen);
    }

    private void money(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "usage.money");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", args[2]);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            messages.send(sender, "money.invalid-amount");
            return;
        }
        UUID uuid = target.getUniqueId();
        switch (args[1].toLowerCase()) {
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
    }

    private void config(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "usage.config");
            return;
        }
        String target = args[1].toLowerCase();
        if (!CONFIG_TARGETS.contains(target)) {
            messages.send(sender, "config.unknown-target", "target", target);
            return;
        }
        switch (args[2].toLowerCase()) {
            case "list" -> configList(sender, target);
            case "get" -> {
                if (args.length < 5) {
                    messages.send(sender, "usage.config");
                    return;
                }
                configGet(sender, target, args[3], args[4]);
            }
            case "set" -> {
                if (args.length < 6) {
                    messages.send(sender, "usage.config");
                    return;
                }
                configSet(sender, target, args[3], args[4], args[5]);
            }
            case "save" -> {
                if (args.length < 4) {
                    messages.send(sender, "usage.config");
                    return;
                }
                configSave(sender, target, args[3]);
            }
            default -> messages.send(sender, "usage.config");
        }
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

    private void confighelp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "usage.confighelp");
            return;
        }
        String target = args[1].toLowerCase();
        if (!CONFIG_TARGETS.contains(target)) {
            messages.send(sender, "config.unknown-target", "target", target);
            return;
        }
        String file = args[2];
        List<String> keys = switch (target) {
            case "core" -> coreDebugApi.describeConfigKeys(file);
            case "world" -> worldDebugApi != null ? worldDebugApi.describeConfigKeys(file) : null;
            case "extra" -> extraDebugApi != null ? extraDebugApi.describeConfigKeys(file) : null;
            default -> null;
        };
        if (keys == null) {
            reportMissingPlugin(sender, target);
            return;
        }
        if (keys.isEmpty()) {
            messages.send(sender, "config.keys-empty");
            return;
        }
        messages.send(sender, "config.keys-header", "target", target, "file", file);
        keys.forEach(key -> messages.sendRaw(sender, "config.file-list-entry", "file", key));
    }

    private void quest(CommandSender sender, String[] args) {
        if (worldDebugApi == null) {
            messages.send(sender, "gui.world-not-installed");
            return;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("complete")) {
            messages.send(sender, "usage.quest");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", args[2]);
            return;
        }
        boolean done = worldDebugApi.forceCompleteQuestObjectives(target.getUniqueId(), args[3]);
        if (done) {
            messages.send(sender, "quest.force-completed", "player", target.getName(), "quest", args[3]);
        } else {
            messages.send(sender, "quest.force-complete-failed");
        }
    }

    private void npc(CommandSender sender, String[] args) {
        if (worldDebugApi == null) {
            messages.send(sender, "gui.world-not-installed");
            return;
        }
        List<String> ids = worldDebugApi.listNpcIds();
        if (ids.isEmpty()) {
            messages.send(sender, "npc.list-empty");
            return;
        }
        messages.send(sender, "npc.list-header");
        ids.forEach(id -> messages.sendRaw(sender, "npc.list-entry", "id", id));
    }

    private void exp(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            messages.send(sender, "usage.exp");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "command.player-not-found", "player", args[2]);
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            messages.send(sender, "exp.invalid-amount");
            return;
        }
        statusApi.addExperience(target.getUniqueId(), amount);
        messages.send(sender, "exp.given", "player", target.getName(), "amount", amount);
    }

    private void reportMissingPlugin(CommandSender sender, String target) {
        messages.send(sender, "world".equals(target) ? "gui.world-not-installed" : "gui.extra-not-installed");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return matching(SUBCOMMANDS, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("gui")) {
            List<String> screens = new ArrayList<>(CORE_GUI_SCREENS);
            screens.addAll(EXTRA_GUI_SCREENS);
            return matching(screens, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("money")) {
            return matching(List.of("give", "set", "take"), args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("config") || args[0].equalsIgnoreCase("confighelp"))) {
            return matching(CONFIG_TARGETS, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("config")) {
            return matching(List.of("list", "get", "set", "save"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("quest")) {
            return matching(List.of("complete"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("exp")) {
            return matching(List.of("give"), args[1]);
        }
        return List.of();
    }

    private List<String> matching(Iterable<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
