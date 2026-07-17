package rpg.debug.core;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import rpg.api.DebugApi;
import rpg.api.EconomyApi;
import rpg.api.GuiApi;
import rpg.api.StatusApi;
import rpg.core.command.AdminCommandRegistry;
import rpg.core.config.ConfigManager;
import rpg.core.message.MessageManager;
import rpg.debug.command.DebugAdminCommand;
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
        if (debugApi == null || guiApi == null || economyApi == null || statusApi == null) {
            getLogger().severe("OreliaCore's DebugApi/GuiApi/EconomyApi/StatusApi services were not found. "
                    + "Is OreliaCore installed and enabled before OreliaDebug?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Soft dependencies - null means that plugin simply isn't installed.
        WorldDebugApi worldDebugApi = getServer().getServicesManager().load(WorldDebugApi.class);
        ExtraDebugApi extraDebugApi = getServer().getServicesManager().load(ExtraDebugApi.class);

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(configManager.register("messages.yml"));

        DebugAdminCommand debugAdminCommand = new DebugAdminCommand(
                messageManager, debugApi, guiApi, economyApi, statusApi, worldDebugApi, extraDebugApi);
        adminCommandRegistration.getProvider().register("debug", debugAdminCommand,
                "テストプレイ支援コマンド（GUI強制表示・所持金操作・config編集など）。",
                "debug <gui|money|config|confighelp|quest|npc|manual> ...");

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
