package com.janboerman.invsee.spigot;

import com.janboerman.invsee.metrics.Metrics;
import com.janboerman.invsee.paper.AsyncTabCompleter;
import com.janboerman.invsee.folia.FoliaScheduler;
import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.OfflinePlayerProvider;
import com.janboerman.invsee.spigot.api.Title;
import org.jetbrains.annotations.NotNull;
import com.janboerman.invsee.spigot.api.logging.LogGranularity;
import com.janboerman.invsee.spigot.api.logging.LogOptions;
import com.janboerman.invsee.spigot.api.logging.LogTarget;
import com.janboerman.invsee.spigot.api.placeholder.PlaceholderPalette;

import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import com.janboerman.invsee.spigot.internal.ConstantTitle;
import com.janboerman.invsee.spigot.internal.InvseePlatform;
import com.janboerman.invsee.spigot.internal.NamesAndUUIDs;
import com.janboerman.invsee.spigot.internal.OpenSpectatorsCache;
import com.janboerman.invsee.spigot.api.Scheduler;
import com.janboerman.invsee.spigot.internal.resolve.ResolveStrategyType;
import com.janboerman.invsee.spigot.perworldinventory.PerWorldInventoryHook;
import com.janboerman.invsee.spigot.perworldinventory.PerWorldInventorySeeApi;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class InvseePlusPlus extends JavaPlugin implements com.janboerman.invsee.spigot.api.InvseePlusPlus {

    public static final String TABCOMPLETION_PERMISSION = "invseeplusplus.tabcomplete";

    private final boolean asyncTabcompleteEvent;

    private InvseeAPI api;
    private InvseePlatform platform;

    private CreationOptions<PlayerInventorySlot> platformCreationOptionsMainInventory;
    private CreationOptions<EnderChestSlot> platformCreationOptionsEnderInventory;
    private boolean dirtyConfig = false;

    private Metrics metrics;

    public InvseePlusPlus() {
        boolean asyncTabCompleteEvent;
        try {
            Class.forName("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
            asyncTabCompleteEvent = true;
        } catch (ClassNotFoundException e) {
            asyncTabCompleteEvent = false;
        }
        this.asyncTabcompleteEvent = asyncTabCompleteEvent;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onEnable() {

        saveDefaultConfig();

        final Scheduler scheduler = makeScheduler(this);
        final NamesAndUUIDs lookup = new NamesAndUUIDs(this, scheduler);
        final OpenSpectatorsCache cache = new OpenSpectatorsCache();
        Setup setup = Setup.setup(this, scheduler, lookup, cache);
        platform = setup.platform();
        final OfflinePlayerProvider playerDatabase = setup.offlinePlayerProvider();

        this.platformCreationOptionsMainInventory = platform.defaultInventoryCreationOptions(this);
        this.platformCreationOptionsEnderInventory = platform.defaultEnderChestCreationOptions(this);

        PerWorldInventoryHook pwiHook;

        if (offlinePlayerSupport() && (pwiHook = new PerWorldInventoryHook(this)).trySetup()) {
            if (pwiHook.managesEitherInventory()) {
                this.api = new PerWorldInventorySeeApi(this, lookup, scheduler, cache, platform, pwiHook);
                getLogger().info("Enabled PerWorldInventory integration.");
            }
        }

        else {
            this.api = new InvseeAPI(this, platform, lookup, scheduler, cache);
        }

        assert this.api != null : "did not set the InvseeAPI instance!";

        FileConfiguration config = loadConfig();
        tabCompleteOfflinePlayers(config);
        api.setOfflinePlayerSupport(offlinePlayerSupport(config));
        api.setUnknownPlayerSupport(unknownPlayerSupport(config));
        api.setMainInventoryTitle(getTitleForInventory(config));
        api.setEnderInventoryTitle(getTitleForEnderChest(config));
        api.setMainInventoryMirror(getInventoryMirror(config));
        api.setEnderInventoryMirror(getEnderChestMirror(config));
        api.setLogOptions(getLogOptions(config));
        api.setPlaceholderPalette(getPlaceholderPalette(platform, config));
        List<ResolveStrategyType> uuidResolveStrategies = getUuidResolveStrategies(config);
        if (uuidResolveStrategies != null) lookup.setUuidResolveTypes(uuidResolveStrategies);
        List<ResolveStrategyType> nameResolveStrategies = getUsernameResolveStrategies(config);
        if (nameResolveStrategies != null) lookup.setNameResolveTypes(nameResolveStrategies);
        lookup.materialiseUsernameAndUniqueIdResolveStrategies();

        setupCommands();

        setupEvents(scheduler, playerDatabase);

        metrics = Metrics.enable(this);

        if (dirtyConfig) {
            try {
                config.save(getConfigFile());
                dirtyConfig = false;
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not update config file!", e);
            }
        }
    }

    private void setupCommands() {
        InvseeTabCompleter tabCompleter = new InvseeTabCompleter(this);

        PluginCommand invseeCommand = Objects.requireNonNull(getCommand("invsee"), "invsee command missing from plugin.yml");
        PluginCommand enderseeCommand = Objects.requireNonNull(getCommand("endersee"), "endersee command missing from plugin.yml");
        PluginCommand reloadCommand = Objects.requireNonNull(getCommand("invseeplusplusreload"), "invseeplusplusreload command missing from plugin.yml");

        invseeCommand.setExecutor(new InvseeCommandExecutor(this));
        enderseeCommand.setExecutor(new EnderseeCommandExecutor(this));
        reloadCommand.setExecutor(new ReloadCommandExecutor(this));

        invseeCommand.setTabCompleter(tabCompleter);
        enderseeCommand.setTabCompleter(tabCompleter);
    }

    private void setupEvents(Scheduler scheduler, OfflinePlayerProvider playerDatabase) {

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new SpectatorInventoryEditListener(), this);

        if (offlinePlayerSupport() && tabCompleteOfflinePlayers()) {
            if (asyncTabcompleteEvent) {
                pluginManager.registerEvents(new AsyncTabCompleter(this, scheduler, playerDatabase), this);
            }
        }
    }

	@Override
	public void onDisable() {
        if (api != null) {
            api.shutDown();
        }

        if (metrics != null) {
            metrics.disable();
            metrics = null;
        }
	}

    public InvseeAPI getApi() {
        return api;
    }

    public boolean tabCompleteOfflinePlayers() {
        return tabCompleteOfflinePlayers(getConfig());
    }

    public boolean tabCompleteOfflinePlayers(FileConfiguration config) {
        Object value = config.get("tabcomplete-offline-players");
        if (value instanceof Boolean bool) {
            return bool;
        } else {
            dirtyConfig = true;
            config.set("tabcomplete-offline-players", asyncTabcompleteEvent);
            return asyncTabcompleteEvent;
        }
    }

    public boolean offlinePlayerSupport() {
        return offlinePlayerSupport(getConfig());
    }

    public boolean offlinePlayerSupport(FileConfiguration config) {
        Object value = config.get("enable-offline-player-support");
        if (value instanceof Boolean bool) {
            return bool;
        } else {
            dirtyConfig = true;
            boolean offlinePlayerSupport = platformCreationOptionsMainInventory.isOfflinePlayerSupported();
            config.set("enable-offline-player-support", offlinePlayerSupport);
            return offlinePlayerSupport;
        }
    }

    public boolean unknownPlayerSupport() {
        return unknownPlayerSupport(getConfig());
    }

    public boolean unknownPlayerSupport(FileConfiguration config) {
        Object value = config.get("enable-unknown-player-support");
        if (value instanceof Boolean bool) {
            return bool;
        } else {
            dirtyConfig = true;
            boolean unknownPlayerSupport = platformCreationOptionsMainInventory.isUnknownPlayerSupported();
            config.set("enable-unknown-player-support", unknownPlayerSupport);
            return unknownPlayerSupport;
        }
    }

    public Title getTitleForInventory() {
        return getTitleForInventory(getConfig());
    }

    public Title getTitleForInventory(FileConfiguration config) {
        String configuredTitle = config.getString("titles.inventory");
        if (configuredTitle == null) {
            dirtyConfig = true;
            Title value = platformCreationOptionsMainInventory.getTitle();
            if (value == Title.defaultMainInventory())
                config.set("titles.inventory", "<player>'s inventory");
            else if (value instanceof ConstantTitle)
                config.set("titles.inventory", ((ConstantTitle) value).getTitle());
            return value;
        } else {
            return target -> configuredTitle.replace("<player>", target.toString());
        }
    }

    public Title getTitleForEnderChest() {
        return getTitleForEnderChest(getConfig());
    }

    public Title getTitleForEnderChest(FileConfiguration config) {
        String configuredTitle = config.getString("titles.enderchest");

        if (configuredTitle == null) {
            dirtyConfig = true;
            Title value = platformCreationOptionsEnderInventory.getTitle();
            if (value == Title.defaultEnderInventory())
                config.set("titles.enderchest", "<player>'s enderchest");
            else if (value instanceof ConstantTitle)
                config.set("titles.enderchest", ((ConstantTitle) value).getTitle());
            return value;
        } else {
            return target -> configuredTitle.replace("<player>", target.toString());
        }
    }

    public Mirror<PlayerInventorySlot> getInventoryMirror() {
        return getInventoryMirror(getConfig());
    }

    public Mirror<PlayerInventorySlot> getInventoryMirror(FileConfiguration config) {
        String template = config.getString("templates.inventory");
        if (template != null) {
            return Mirror.forInventory(template);
        } else {
            dirtyConfig = true;
            Mirror<PlayerInventorySlot> value = platformCreationOptionsMainInventory.getMirror();
            config.set("templates.inventory", Mirror.toInventoryTemplate(value));
            return value;
        }
    }

    public Mirror<EnderChestSlot> getEnderChestMirror() {
        return getEnderChestMirror(getConfig());
    }

    public Mirror<EnderChestSlot> getEnderChestMirror(FileConfiguration config) {
        String template = config.getString("templates.enderchest");
        if (template != null) {
            return Mirror.forEnderChest(template);
        } else {
            dirtyConfig = true;
            Mirror<EnderChestSlot> value = platformCreationOptionsEnderInventory.getMirror();
            config.set("templates.enderchest", Mirror.toEnderChestTemplate(value));
            return value;
        }
    }

    public LogOptions getLogOptions() {
        return getLogOptions(getConfig());
    }

    public LogOptions getLogOptions(FileConfiguration config) {
        ConfigurationSection loggingSection = config.getConfigurationSection("logging");
        if (loggingSection == null) {
            dirtyConfig = true;
            LogOptions value = platformCreationOptionsMainInventory.getLogOptions();
            loggingSection = config.createSection("logging");
            loggingSection.set("granularity", value.getGranularity().name());
            loggingSection.set("output", value.getTargets().stream().map(LogTarget::name).collect(Collectors.toList()));
            loggingSection.set("format-server-log-file", value.getFormat(LogTarget.SERVER_LOG_FILE));
            loggingSection.set("format-plugin-log-file", value.getFormat(LogTarget.PLUGIN_LOG_FILE));
            loggingSection.set("format-spectator-log-file", value.getFormat(LogTarget.SERVER_LOG_FILE));
            loggingSection.set("format-console", value.getFormat(LogTarget.CONSOLE));
            return value;
        } else {
            String granularity = loggingSection.getString("granularity", "LOG_ON_CLOSE");
            LogGranularity logGranularity = LogGranularity.valueOf(granularity);
            List<String> output = loggingSection.getStringList("output");
            EnumSet<LogTarget> logTargets = output.stream()
                    .map(LogTarget::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(LogTarget.class)));
            EnumMap<LogTarget, String> formats = new EnumMap<>(LogTarget.class);
            String formatServerLogFile = loggingSection.getString("format-server-log-file");
            if (formatServerLogFile != null) formats.put(LogTarget.SERVER_LOG_FILE, formatServerLogFile);
            String formatPluginLogFile = loggingSection.getString("format-plugin-log-file");
            if (formatPluginLogFile != null) formats.put(LogTarget.PLUGIN_LOG_FILE, formatPluginLogFile);
            String formatSpectatorLogFile = loggingSection.getString("format-spectator-log-file");
            if (formatSpectatorLogFile != null) formats.put(LogTarget.SPECTATOR_LOG_FILE, formatSpectatorLogFile);
            String formatConsole = loggingSection.getString("format-console");
            if (formatConsole != null) formats.put(LogTarget.CONSOLE, formatConsole);
            return LogOptions.of(logGranularity, logTargets, formats);
        }
    }

    public PlaceholderPalette getPlaceholderPalette() {
        return getPlaceholderPalette(platform, getConfig());
    }

    public PlaceholderPalette getPlaceholderPalette(InvseePlatform platform, FileConfiguration config) {
        String paletteName = config.getString("placeholder-palette");
        PlaceholderPalette palette;
        if (paletteName == null) {
            dirtyConfig = true;
            palette = platformCreationOptionsMainInventory.getPlaceholderPalette();
            config.set("placeholder-palette", palette.toString());
        } else {
            palette = platform.getPlaceholderPalette(paletteName);
        }
        return palette;
    }

    private File getConfigFile() {
        return new File(getDataFolder(), "config.yml");
    }

    private FileConfiguration loadConfig() {
        return YamlConfiguration.loadConfiguration(getConfigFile());
    }

    private static Scheduler makeScheduler(InvseePlusPlus plugin) {

        return new FoliaScheduler(plugin);
    }

    private static List<ResolveStrategyType> getUuidResolveStrategies(FileConfiguration config) {
        return toResolveStrategyTypes(config.getStringList("uuid-resolve-strategies"));
    }

    private static List<ResolveStrategyType> getUsernameResolveStrategies(FileConfiguration config) {
        return toResolveStrategyTypes(config.getStringList("username-resolve-strategies"));
    }

    private static List<ResolveStrategyType> toResolveStrategyTypes(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        List<ResolveStrategyType> result = new ArrayList<>(list.size());
        for (String strat : list) {
            result.add(ResolveStrategyType.fromString(strat));
        }
        return result;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Oh no! It looks like InvSee++ didn't start correctly!");
        sender.sendMessage(ChatColor.YELLOW + "Most likely this is a Minecraft/InvSee++ version mismatch.");
        sender.sendMessage(ChatColor.YELLOW + "Check your logs for more information.");
        return true;
    }

    public Path getJarFilePath() {
        return getFile().toPath();
    }
}
