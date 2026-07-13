package com.janboerman.invsee.spigot.internal;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.EnderSpectatorInventory;
import com.janboerman.invsee.spigot.api.EnderSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import com.janboerman.invsee.spigot.api.MainSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.Title;
import com.janboerman.invsee.spigot.api.logging.LogGranularity;
import com.janboerman.invsee.spigot.api.logging.LogOptions;
import com.janboerman.invsee.spigot.api.logging.LogTarget;
import com.janboerman.invsee.spigot.api.placeholder.PlaceholderPalette;
import com.janboerman.invsee.spigot.api.response.OpenResponse;
import com.janboerman.invsee.spigot.api.response.SaveResponse;
import com.janboerman.invsee.spigot.api.response.SpectateResponse;
import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface InvseePlatform {

    public abstract MainSpectatorInventory spectateInventory(HumanEntity target, CreationOptions<PlayerInventorySlot> options);

    public abstract CompletableFuture<SpectateResponse<MainSpectatorInventory>> createOfflineInventory(UUID playerId, String playerName, CreationOptions<PlayerInventorySlot> options);

    public abstract CompletableFuture<SaveResponse> saveInventory(MainSpectatorInventory inventory);

    public OpenResponse<MainSpectatorInventoryView> openMainSpectatorInventory(Player spectator, MainSpectatorInventory spectatorInventory, CreationOptions<PlayerInventorySlot> options);

    public abstract EnderSpectatorInventory spectateEnderChest(HumanEntity target, CreationOptions<EnderChestSlot> options);

    public abstract CompletableFuture<SpectateResponse<EnderSpectatorInventory>> createOfflineEnderChest(UUID playerId, String playerName, CreationOptions<EnderChestSlot> options);

    public abstract CompletableFuture<SaveResponse> saveEnderChest(EnderSpectatorInventory enderChest);

    public OpenResponse<EnderSpectatorInventoryView> openEnderSpectatorInventory(Player spectator, EnderSpectatorInventory spectatorInventory, CreationOptions<EnderChestSlot> options);

    public default PlaceholderPalette getPlaceholderPalette(String name) {
        return PlaceholderPalette.empty();
    }

    public default CreationOptions<PlayerInventorySlot> defaultInventoryCreationOptions(Plugin plugin) {
        return CreationOptions.of(plugin, Title.defaultMainInventory(), true, Mirror.defaultPlayerInventory(), true, false, LogOptions
                .of(LogGranularity.LOG_ON_CLOSE, Collections.singleton(LogTarget.PLUGIN_LOG_FILE), LogOptions.defaultLogFormats()), PlaceholderPalette.empty());
    }

    public default CreationOptions<EnderChestSlot> defaultEnderChestCreationOptions(Plugin plugin) {
        return CreationOptions.of(plugin, Title.defaultEnderInventory(), true, Mirror.defaultEnderChest(), true, false, LogOptions
                .of(LogGranularity.LOG_ON_CLOSE, Collections.singleton(LogTarget.PLUGIN_LOG_FILE), LogOptions.defaultLogFormats()), PlaceholderPalette.empty());
    }

    public default Stream<Material> materials() {
        return Arrays.stream(Material.values());
    }

}
