package com.janboerman.invsee.spigot.api;

import com.janboerman.invsee.spigot.api.logging.LogGranularity;
import com.janboerman.invsee.spigot.api.logging.LogOptions;
import com.janboerman.invsee.spigot.api.placeholder.PlaceholderPalette;
import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class CreationOptions<Slot> implements Cloneable {

    private Plugin plugin;
    private Title title;
    private boolean offlinePlayerSupport = true;
    private Mirror<Slot> mirror;
    private boolean unknownPlayerSupport = true;
    private boolean bypassExempt = false;
    private LogOptions logOptions = new LogOptions();
    private PlaceholderPalette placeholderPalette = PlaceholderPalette.empty();

    CreationOptions(Plugin plugin, Title title, boolean offlinePlayerSupport, Mirror<Slot> mirror, boolean unknownPlayerSupport, boolean bypassExempt, LogOptions logOptions, PlaceholderPalette palette) {
        this.plugin = plugin;
        this.title = Objects.requireNonNull(title);
        this.offlinePlayerSupport = offlinePlayerSupport;
        this.mirror = Objects.requireNonNull(mirror);
        this.unknownPlayerSupport = unknownPlayerSupport;
        this.bypassExempt = bypassExempt;
        this.logOptions = Objects.requireNonNull(logOptions);
        this.placeholderPalette = palette;
    }

    public static <Slot> CreationOptions<Slot> of(Plugin plugin, Title title, boolean offlinePlayerSupport, Mirror<Slot> mirror, boolean unknownPlayerSupport, boolean bypassExempt, LogOptions logOptions, PlaceholderPalette placeholderPalette) {
        return new CreationOptions<>(plugin, title, offlinePlayerSupport, mirror, unknownPlayerSupport, bypassExempt, logOptions, placeholderPalette);
    }

    @Deprecated
    public static <Slot> CreationOptions<Slot> of(Title title, boolean offlinePlayerSupport, Mirror<Slot> mirror, boolean unknownPlayerSupport) throws Exception {
        Plugin plugin = JavaPlugin.getPlugin((Class<JavaPlugin>) Class.forName("com.janboerman.invsee.spigot.InvseePlusPlus"));
        return new CreationOptions<>(plugin, title, offlinePlayerSupport, mirror, unknownPlayerSupport, false, new LogOptions().withGranularity(LogGranularity.LOG_NEVER), PlaceholderPalette.empty());
    }

    @Deprecated
    public static <Slot> CreationOptions<Slot> of(Title title, boolean offlinePlayerSupport, Mirror<Slot> mirror, boolean unknownPlayerSupport, boolean bypassExempt) throws Exception {
        Plugin plugin = JavaPlugin.getPlugin((Class<JavaPlugin>) Class.forName("com.janboerman.invsee.spigot.InvseePlusPlus"));
        return new CreationOptions<>(plugin, title, offlinePlayerSupport, mirror, unknownPlayerSupport, bypassExempt, new LogOptions().withGranularity(LogGranularity.LOG_NEVER), PlaceholderPalette.empty());
    }

    @Deprecated
    public static <Slot> CreationOptions<Slot> of(Plugin plugin, Title title, boolean offlinePlayerSupport, Mirror<Slot> mirror, boolean unknownPlayerSupport, boolean bypassExempt, LogOptions logOptions) {
        return new CreationOptions<>(plugin, title, offlinePlayerSupport, mirror, unknownPlayerSupport, bypassExempt, logOptions, PlaceholderPalette.empty());
    }

    @Deprecated
    public static CreationOptions<PlayerInventorySlot> defaultMainInventory() {
        return defaultMainInventory(Bukkit.getPluginManager().getPlugin("InvseePlusPlus"));
    }

    public static CreationOptions<PlayerInventorySlot> defaultMainInventory(Plugin plugin) {
        return new CreationOptions<>(plugin, Title.defaultMainInventory(), true, Mirror.defaultPlayerInventory(), true, false, new LogOptions(), PlaceholderPalette.empty());
    }

    @Deprecated
    public static CreationOptions<EnderChestSlot> defaultEnderInventory() {
        return defaultEnderInventory(Bukkit.getPluginManager().getPlugin("InvseePlusPlus"));
    }

    public static CreationOptions<EnderChestSlot> defaultEnderInventory(Plugin plugin) {
        return new CreationOptions<>(plugin, Title.defaultEnderInventory(), true, Mirror.defaultEnderChest(), true, false, new LogOptions(), PlaceholderPalette.empty());
    }

    @Override
    public CreationOptions<Slot> clone() {
        return new CreationOptions<>(getPlugin(), getTitle(), isOfflinePlayerSupported(), getMirror(), isUnknownPlayerSupported(), canBypassExemptedPlayers(), getLogOptions().clone(), getPlaceholderPalette());
    }

    public CreationOptions<Slot> withPlugin(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        return this;
    }

    public CreationOptions<Slot> withTitle(Title title) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        return this;
    }

    public CreationOptions<Slot> withTitle(String title) {
        return withTitle(Title.of(title));
    }

    public CreationOptions<Slot> withOfflinePlayerSupport(boolean offlinePlayerSupport) {
        this.offlinePlayerSupport = offlinePlayerSupport;
        return this;
    }

    public CreationOptions<Slot> withMirror(Mirror<Slot> mirror) {
        this.mirror = Objects.requireNonNull(mirror, "mirror cannot be null");
        return this;
    }

    public CreationOptions<Slot> withUnknownPlayerSupport(boolean unknownPlayerSupport) {
        this.unknownPlayerSupport = unknownPlayerSupport;
        return this;
    }

    public CreationOptions<Slot> withBypassExemptedPlayers(boolean bypassExemptedPlayers) {
        this.bypassExempt = bypassExemptedPlayers;
        return this;
    }

    public CreationOptions<Slot> withLogOptions(LogOptions logOptions) {
        this.logOptions = Objects.requireNonNull(logOptions, "logOptions cannot be null");
        return this;
    }

    public CreationOptions<Slot> withPlaceholderPalette(PlaceholderPalette placeholderPalette) {
        this.placeholderPalette = placeholderPalette;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CreationOptions)) return false;

        CreationOptions<?> that = (CreationOptions<?>) o;
        return this.getPlugin().equals(that.getPlugin())
                && this.getTitle().equals(that.getTitle())
                && this.isOfflinePlayerSupported() == that.isOfflinePlayerSupported()
                && this.getMirror() == that.getMirror()
                && this.isUnknownPlayerSupported() == that.isUnknownPlayerSupported()
                && this.canBypassExemptedPlayers() == that.canBypassExemptedPlayers()
                && Objects.equals(this.getLogOptions(), that.getLogOptions())
                && Objects.equals(this.getPlaceholderPalette(), that.getPlaceholderPalette());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPlugin(), getTitle(), isOfflinePlayerSupported(), getMirror(), isUnknownPlayerSupported(), getLogOptions(), getPlaceholderPalette());
    }

    @Override
    public String toString() {
        return "CreationOptions"
                + "{plugin=" + getPlugin()
                + ",title=" + getTitle()
                + ",offlinePlayerSupport=" + isOfflinePlayerSupported()
                + ",mirror=" + getMirror()
                + ",unknownPlayerSupport=" + isUnknownPlayerSupported()
                + ",bypassExempt=" + canBypassExemptedPlayers()
                + ",logOptions=" + getLogOptions()
                + ",placeholderPalette=" + getPlaceholderPalette()
                + "}";
    }

    public Plugin getPlugin() {
        if (plugin == null) {
            try {
                plugin = JavaPlugin.getPlugin((Class<JavaPlugin>) Class.forName("com.janboerman.invsee.spigot.InvseePlusPlus"));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Woops, I screwed up severely. Please report this issue (including stack trace) at https://github.com/Jannyboy11/InvSee-plus-plus", e);
            }
        }
        return plugin;
    }

    public Title getTitle() {
        return title;
    }

    public boolean isOfflinePlayerSupported() {
        return offlinePlayerSupport;
    }

    public Mirror<Slot> getMirror() {
        return mirror;
    }

    public boolean isUnknownPlayerSupported() {
        return unknownPlayerSupport;
    }

    public boolean canBypassExemptedPlayers() {
        return bypassExempt;
    }

    public LogOptions getLogOptions() {
        return logOptions;
    }

    public PlaceholderPalette getPlaceholderPalette() {
        return placeholderPalette;
    }
}
