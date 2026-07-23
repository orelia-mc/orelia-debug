package rpg.debug.core;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import rpg.api.DebugApi;
import rpg.api.EconomyApi;
import rpg.api.GuiApi;
import rpg.api.SkillApi;
import rpg.api.StatusApi;
import rpg.core.command.AdminCommandRegistry;
import rpg.core.config.ConfigManager;
import rpg.core.message.MessageManager;
import rpg.debug.command.ConfigDebugCommand;
import rpg.debug.command.ConfigHelpDebugCommand;
import rpg.debug.command.ExpDebugCommand;
import rpg.debug.command.GuiDebugCommand;
import rpg.debug.command.ManualCommand;
import rpg.debug.command.MoneyDebugCommand;
import rpg.debug.command.QuestDebugCommand;
import rpg.debug.command.SkillPointsDebugCommand;
import rpg.extra.api.ExtraDebugApi;
import rpg.world.api.WorldDebugApi;

/**
 * Plugin entry point for the orelia-debug repo/jar: testplay/debug tooling for the other
 * three Orelia plugins. Requires OreliaCore (hard dependency, plugin.yml {@code depend}) -
 * OreliaWorld/OreliaExtra are soft dependencies (plugin.yml {@code softdepend}), so every
 * {@link WorldDebugApi}/{@link ExtraDebugApi} lookup below is null-guarded and features that
 * need them simply report "not installed" rather than failing plugin startup.
 *
 * <p>This plugin owns no gameplay state of its own - it only reaches into the other three
 * plugins through their published {@code rpg.api}/{@code rpg.world.api}/{@code rpg.extra.api}
 * interfaces (Bukkit {@code ServicesManager}), exactly like orelia-world/orelia-extra do.
 */
public final class OreliaDebugPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<AdminCommandRegistry> adminCommandRegistration =
                getServer().getServicesManager().getRegistration(AdminCommandRegistry.class);
        if (adminCommandRegistration == null) {
            getLogger().severe("OreliaCore's AdminCommandRegistry service was not found. "
                    + "Is OreliaCore installed and enabled before OreliaDebug?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DebugApi debugApi = getServer().getServicesManager().load(DebugApi.class);
        GuiApi guiApi = getServer().getServicesManager().load(GuiApi.class);
        EconomyApi economyApi = getServer().getServicesManager().load(EconomyApi.class);
        StatusApi statusApi = getServer().getServicesManager().load(StatusApi.class);
        SkillApi skillApi = getServer().getServicesManager().load(SkillApi.class);
        if (debugApi == null || guiApi == null || economyApi == null || statusApi == null || skillApi == null) {
            getLogger().severe("OreliaCore's DebugApi/GuiApi/EconomyApi/StatusApi/SkillApi services were not found. "
                    + "Is OreliaCore installed and enabled before OreliaDebug?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Soft dependencies - null means that plugin simply isn't installed.
        WorldDebugApi worldDebugApi = getServer().getServicesManager().load(WorldDebugApi.class);
        ExtraDebugApi extraDebugApi = getServer().getServicesManager().load(ExtraDebugApi.class);

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(configManager.register("messages.yml"));

        AdminCommandRegistry adminCommandRegistry = adminCommandRegistration.getProvider();
        adminCommandRegistry.register("gui", new GuiDebugCommand(messageManager, guiApi, extraDebugApi),
                "指定プレイヤー(省略時は自分)に各種GUIを強制表示します。",
                "gui <status|equipment|skill|job|shop|warehouse|auction|mail|ranking> [player]");
        adminCommandRegistry.register("money", new MoneyDebugCommand(messageManager, economyApi),
                "指定プレイヤー(省略時は自分)の所持金を付与・設定・引き出しします。",
                "money <give|set|take> [player] <amount>");
        adminCommandRegistry.register("config", new ConfigDebugCommand(messageManager, debugApi, worldDebugApi, extraDebugApi),
                "各プラグインの設定ファイルを確認・編集します。",
                "config <core|world|extra> <list|get <file> <path>|set <file> <path> <value>|save <file>>");
        adminCommandRegistry.register("confighelp", new ConfigHelpDebugCommand(messageManager, debugApi, worldDebugApi, extraDebugApi),
                "設定ファイルの全キー一覧を表示します。", "confighelp <core|world|extra> <file>");
        adminCommandRegistry.register("quest", new QuestDebugCommand(messageManager, worldDebugApi),
                "指定プレイヤー(省略時は自分)のクエストの目標を強制達成します（要OreliaWorld）。",
                "quest complete [player] <questId>");
        adminCommandRegistry.register("exp", new ExpDebugCommand(messageManager, statusApi),
                "指定プレイヤー(省略時は自分)に経験値を付与します。", "exp give [player] <amount>");
        adminCommandRegistry.register("skillpoints", new SkillPointsDebugCommand(messageManager, skillApi),
                "指定プレイヤー(省略時は自分)のスキル習得ポイントを付与・設定・引き出しします。",
                "skillpoints <give|set|take> [player] <amount>");
        adminCommandRegistry.register("manual", new ManualCommand(),
                "OreliaDebugのコマンド一覧を表示します。", "manual [page]");

        getLogger().info("OreliaDebug enabled" + (worldDebugApi == null ? " (OreliaWorld not detected)" : "")
                + (extraDebugApi == null ? " (OreliaExtra not detected)" : "") + ".");
    }

    @Override
    public void onDisable() {
    }

    public void reload() {
        configManager.reloadAll();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
