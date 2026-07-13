package com.janboerman.invsee.spigot.perworldinventory;

import static com.janboerman.invsee.utils.Compat.ifPresentOrElse;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.EnderSpectatorInventory;
import com.janboerman.invsee.spigot.api.EnderSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import com.janboerman.invsee.spigot.api.MainSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.SpectatorInventory;
import com.janboerman.invsee.spigot.api.response.NotCreatedReason;
import com.janboerman.invsee.spigot.api.response.OpenResponse;
import com.janboerman.invsee.spigot.api.response.SaveResponse;
import com.janboerman.invsee.spigot.api.response.SpectateResponse;
import com.janboerman.invsee.spigot.api.target.Target;
import com.janboerman.invsee.spigot.api.target.UniqueIdTarget;
import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import com.janboerman.invsee.spigot.api.template.Mirror;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import com.janboerman.invsee.spigot.internal.CompletedEmpty;
import com.janboerman.invsee.spigot.internal.InvseePlatform;
import com.janboerman.invsee.spigot.internal.NamesAndUUIDs;
import com.janboerman.invsee.spigot.internal.OpenSpectatorsCache;
import com.janboerman.invsee.spigot.api.Scheduler;
import com.janboerman.invsee.spigot.internal.inventory.Personal;
import com.janboerman.invsee.utils.Compat;
import me.ebonjaeger.perworldinventory.Group;
import me.ebonjaeger.perworldinventory.data.PlayerProfile;
import me.ebonjaeger.perworldinventory.data.ProfileKey;
import me.ebonjaeger.perworldinventory.event.InventoryLoadCompleteEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class PerWorldInventorySeeApi extends InvseeAPI implements InvseePlatform {

    private final InvseePlatform wrapped;
    private final PerWorldInventoryHook pwiHook;

    private final Map<ProfileKey, MainSpectatorInventory> inventories = new HashMap<>();
    private final Map<MainSpectatorInventory, ProfileKey> inventoryKeys = new HashMap<>();
    private final Map<ProfileKey, EnderSpectatorInventory> enderchests = new HashMap<>();
    private final Map<EnderSpectatorInventory, ProfileKey> enderchestKeys = new HashMap<>();

    private PwiEventListener pwiEventListener;
    private TiedInventoryListener tiedInventoryListener;
    private TiedPlayerListener tiedPlayerListener;

    private final OpenSpectatorsCache cache;
    private final Scheduler scheduler;

    public PerWorldInventorySeeApi(Plugin plugin,
                                   NamesAndUUIDs lookup, Scheduler scheduler, OpenSpectatorsCache cachedInventories,
                                   InvseePlatform wrapped, PerWorldInventoryHook pwiHook) {
        super(plugin, null, lookup, scheduler, cachedInventories);
        this.wrapped = Objects.requireNonNull(wrapped);
        this.pwiHook = Objects.requireNonNull(pwiHook);

        this.cache = Objects.requireNonNull(cachedInventories);
        this.scheduler = Objects.requireNonNull(scheduler);

        setMainInventoryTransferPredicate((spectatorInventory, player) -> {
            if (!pwiHook.pwiManagedInventories()) return true;

            ProfileKey profileKey = inventoryKeys.get(spectatorInventory);
            if (profileKey == null) return true;

            return pwiHook.isMatchedByProfile(player, profileKey);
        });
        setEnderChestTransferPredicate((spectatorInventory, player) -> {
            if (!pwiHook.pwiManagedEnderChests()) return true;

            ProfileKey profileKey = enderchestKeys.get(spectatorInventory);
            if (profileKey == null) return true;

            return pwiHook.isMatchedByProfile(player, profileKey);
        });
    }

    @Override
    protected InvseePlatform getPlatform() {
        return this;
    }

    public void registerListeners() {
        super.registerListeners();
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(pwiEventListener = new PwiEventListener(), plugin);
        pluginManager.registerEvents(tiedInventoryListener = new TiedInventoryListener(), plugin);
        pluginManager.registerEvents(tiedPlayerListener = new TiedPlayerListener(), plugin);
    }

    public void unregisterListeners() {
        HandlerList.unregisterAll(pwiEventListener);
        HandlerList.unregisterAll(tiedInventoryListener);
        HandlerList.unregisterAll(tiedPlayerListener);
        super.unregisterListeners();
    }

    public PerWorldInventoryHook getHook() {
        return pwiHook;
    }

    private final class TiedPlayerListener implements Listener {

        @EventHandler
        public void onTargetQuit(PlayerQuitEvent event) {

            Player player = event.getPlayer();
            ProfileKey key = pwiHook.getActiveProfileKey(player);

            MainSpectatorInventory mainSpectator = inventories.get(key);
            if (mainSpectator != null && mainSpectator.getViewers().isEmpty()) {
                inventories.remove(key);
                inventoryKeys.remove(mainSpectator, key);
            }

            EnderSpectatorInventory enderSpectator = enderchests.get(key);
            if (enderSpectator != null && enderSpectator.getViewers().isEmpty()) {
                enderchests.remove(key);
                enderchestKeys.remove(enderSpectator, key);
            }
        }
    }

    private final class TiedInventoryListener implements Listener {

        @EventHandler
        public void onSpectatorClose(InventoryCloseEvent event) {
            Inventory inventory = event.getInventory();
            if (inventory instanceof MainSpectatorInventory) {
                MainSpectatorInventory main = (MainSpectatorInventory) inventory;

                ProfileKey key = inventoryKeys.get(main);
                if (key != null) {
                    scheduler.executeLaterGlobal(() -> {

                        if (main.getViewers().isEmpty()) {
                            inventories.remove(key, main);
                            inventoryKeys.remove(main, key);
                        }
                    }, 20L * 5);
                }
            } else if (inventory instanceof EnderSpectatorInventory) {
                EnderSpectatorInventory ender = (EnderSpectatorInventory) inventory;

                ProfileKey key = enderchestKeys.get(ender);
                if (key != null) {
                    scheduler.executeLaterGlobal(() -> {

                        if (ender.getViewers().isEmpty()) {
                            enderchests.remove(key, ender);
                            enderchestKeys.remove(ender, key);
                        }
                    }, 20L * 5);
                }
            }
        }

        @EventHandler
        public void onTargetInventoryOpen(InventoryOpenEvent event) {
            HumanEntity player = event.getPlayer();

            ProfileKey activeProfileKey = pwiHook.getActiveProfileKey(player);
            MainSpectatorInventory mainSpectator = inventories.get(activeProfileKey);
            if (mainSpectator instanceof Personal) {
                ((Personal) mainSpectator).watch(event.getView());
            }

            MainSpectatorInventory spectator = cache.getMainSpectatorInventory(player.getUniqueId());
            if (spectator instanceof Personal) {
                ((Personal) spectator).watch(event.getView());
            }
        }

        @EventHandler
        public void onTargetInventoryClose(InventoryCloseEvent event) {
            HumanEntity player = event.getPlayer();

            ProfileKey activeProfileKey = pwiHook.getActiveProfileKey(player);
            MainSpectatorInventory mainSpectator = inventories.get(activeProfileKey);
            if (mainSpectator instanceof Personal) {
                ((Personal) mainSpectator).unwatch();
            }

            MainSpectatorInventory spectator = cache.getMainSpectatorInventory(player.getUniqueId());
            if (spectator instanceof Personal) {
                ((Personal) spectator).unwatch();
            }
        }
    }

    private final class PwiEventListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onTeleport(PlayerTeleportEvent event) {
            if (pwiHook.bypassesWorldChange(event.getPlayer())) return;

            World from = event.getFrom().getWorld(); assert from != null;
            World to = event.getTo().getWorld(); assert to != null;

            if (!from.equals(to) && !pwiHook.worldsShareInventory(from.toString(), to.toString())) {
                giveSnapshotInventoryToSpectators(pwiHook.getActiveProfileKey(event.getPlayer()));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onGameModeChange(PlayerGameModeChangeEvent event) {
            if (pwiHook.bypassesGameModeChange(event.getPlayer())) return;

            if (pwiHook.pwiInventoriesPerGameMode()) {
                giveSnapshotInventoryToSpectators(pwiHook.getActiveProfileKey(event.getPlayer()));
            }
        }

        private void giveSnapshotInventoryToSpectators(ProfileKey oldProfileKey) {

            MainSpectatorInventory mainSpectator = inventories.get(oldProfileKey);
            EnderSpectatorInventory enderSpectator = enderchests.get(oldProfileKey);

            if (mainSpectator != null) {
                List<HumanEntity> viewers = new ArrayList<>(mainSpectator.getViewers());
                ItemStack[] contents = mainSpectator.getContents();

                viewers.forEach(HumanEntity::closeInventory);

                CompletableFuture<Optional<MainSpectatorInventory>> snapshotFuture = asSnapShotInventory(mainSpectator);
                snapshotFuture.thenAccept(optional -> ifPresentOrElse(optional, newSpectatorInventory -> {
                    inventories.put(oldProfileKey, newSpectatorInventory);
                    inventoryKeys.put(newSpectatorInventory, oldProfileKey);
                    newSpectatorInventory.setContents(contents);
                    viewers.forEach(v -> v.openInventory(newSpectatorInventory));
                },  () -> inventories.remove(oldProfileKey)));
            }

            if (enderSpectator != null) {
                List<HumanEntity> viewers = new ArrayList<>(enderSpectator.getViewers());
                ItemStack[] contents = enderSpectator.getContents();

                CompletableFuture<Optional<EnderSpectatorInventory>> snapshotFuture = asSnapShotInventory(enderSpectator);
                snapshotFuture.thenAccept(optional -> ifPresentOrElse(optional, newSpectatorInventory -> {
                    enderchests.put(oldProfileKey, newSpectatorInventory);
                    enderchestKeys.put(newSpectatorInventory, oldProfileKey);
                    newSpectatorInventory.setContents(contents);
                    viewers.forEach(v -> v.openInventory(newSpectatorInventory));
                },  () -> enderchests.remove(oldProfileKey)));
            }
        }

        @EventHandler
        public void onPwiLoadComplete(InventoryLoadCompleteEvent event) {
            ProfileKey newProfileKey = new ProfileKey(event.getPlayer().getUniqueId(), event.getGroup(), event.getGameMode());
            giveLiveInventoryToSpectators(newProfileKey);
        }

        private void giveLiveInventoryToSpectators(ProfileKey newProfileKey) {

            MainSpectatorInventory mainSpectator = inventories.get(newProfileKey);
            EnderSpectatorInventory enderSpectator = enderchests.get(newProfileKey);

            if (mainSpectator != null) {

                List<HumanEntity> viewers = new ArrayList<>(mainSpectator.getViewers());
                ItemStack[] contents = mainSpectator.getContents();
                viewers.forEach(HumanEntity::closeInventory);

                Executor executor = runnable -> scheduler.executeSyncPlayer(newProfileKey.getUuid(), runnable, null);
                executor.execute(() -> {

                    Optional<MainSpectatorInventory> liveFuture = asLiveInventory(mainSpectator, false);
                    ifPresentOrElse(liveFuture, liveSpectator -> {
                        inventories.put(newProfileKey, liveSpectator);
                        inventoryKeys.put(liveSpectator, newProfileKey);
                        liveSpectator.setContents(contents);
                        viewers.forEach(v -> v.openInventory(liveSpectator));
                    },  () -> inventories.remove(newProfileKey));
                });
            }

            if (enderSpectator != null) {
                List<HumanEntity> viewers = new ArrayList<>(enderSpectator.getViewers());
                ItemStack[] contents = enderSpectator.getContents();
                viewers.forEach(HumanEntity::closeInventory);

                Executor executor = runnable -> scheduler.executeSyncPlayer(newProfileKey.getUuid(), runnable, null);
                executor.execute(() -> {

                    Optional<EnderSpectatorInventory> liveFuture = asLiveInventory(enderSpectator, false);
                    ifPresentOrElse(liveFuture, liveSpectator -> {
                        enderchests.put(newProfileKey, liveSpectator);
                        enderchestKeys.put(liveSpectator, newProfileKey);
                        liveSpectator.setContents(contents);
                        viewers.forEach(v -> v.openInventory(liveSpectator));
                    },  () -> inventories.remove(newProfileKey));
                });
            }
        }
    }

    @Override
    public OpenResponse<MainSpectatorInventoryView> openMainSpectatorInventory(Player spectator, MainSpectatorInventory spectatorInventory, CreationOptions<PlayerInventorySlot> options) {
        return wrapped.openMainSpectatorInventory(spectator, spectatorInventory, options);
    }

    @Override
    public MainSpectatorInventory spectateInventory(HumanEntity player, CreationOptions<PlayerInventorySlot> options) {
        return wrapped.spectateInventory(player, options);
    }

    public MainSpectatorInventory spectateInventory(HumanEntity player, CreationOptions<PlayerInventorySlot> options, ProfileKey profileKey) {

        MainSpectatorInventory spectatorInv = spectateInventory(player, options);
        inventories.put(profileKey, spectatorInv);
        inventoryKeys.put(spectatorInv, profileKey);
        return spectatorInv;
    }

    public final CompletableFuture<SpectateResponse<MainSpectatorInventory>> spectateInventory(UUID playerId, String playerName, CreationOptions<PlayerInventorySlot> options, ProfileId profileId) {
        Player player = plugin.getServer().getPlayer(playerId);
        ProfileKey profileKey = profileId.profileKey;
        if (player != null && getHook().isMatchedByProfile(player, profileKey))
            return CompletableFuture.completedFuture(SpectateResponse.succeed(spectateInventory(player, options, profileKey)));

        return createOfflineInventory(playerId, playerName, options, profileKey);
    }

    @Override
    public CompletableFuture<SpectateResponse<MainSpectatorInventory>> createOfflineInventory(UUID playerId, String playerName, CreationOptions<PlayerInventorySlot> options) {
        Location logoutLocation = pwiHook.getDataSource().getLogout(new FakePlayer(playerId, playerName, plugin.getServer()));
        World world = logoutLocation != null ? logoutLocation.getWorld() : plugin.getServer().getWorlds().get(0);
        Group group = pwiHook.getGroupForWorld(world.getName());
        ProfileKey profileKey = new ProfileKey(playerId, group, GameMode.SURVIVAL );
        return createOfflineInventory(playerId, playerName, options, profileKey, false);
    }

    @Override
    public CompletableFuture<SaveResponse> saveInventory(MainSpectatorInventory inventory) {
        ProfileKey profileKey = inventoryKeys.get(inventory);
        boolean saveVanilla = false;

        Location location = null;
        Player target = plugin.getServer().getPlayer(inventory.getSpectatedPlayerId());
        if (target != null) location = target.getLocation();
        if (location == null) location = pwiHook.getDataSource().getLogout(new FakePlayer(inventory.getSpectatedPlayerId(), inventory.getSpectatedPlayerName(), plugin.getServer()));
        World logoutWorld = location != null ? location.getWorld() : plugin.getServer().getWorlds().get(0); assert logoutWorld != null;

        if (profileKey == null) {
            saveVanilla = true;
            profileKey = new ProfileKey(inventory.getSpectatedPlayerId(), pwiHook.getGroupForWorld(logoutWorld.getName()), GameMode.SURVIVAL );
        } else if (!pwiHook.pwiLoadDataOnJoin() && profileKey.getGroup().containsWorld(logoutWorld.getName())) {

            saveVanilla = true;
        }

        return saveInventory(inventory, profileKey, saveVanilla);
    }

    @Override
    public OpenResponse<EnderSpectatorInventoryView> openEnderSpectatorInventory(Player spectator, EnderSpectatorInventory spectatorInventory, CreationOptions<EnderChestSlot> options) {
        return wrapped.openEnderSpectatorInventory(spectator, spectatorInventory, options);
    }

    @Override
    public EnderSpectatorInventory spectateEnderChest(HumanEntity player, CreationOptions<EnderChestSlot> options) {
        return wrapped.spectateEnderChest(player, options);
    }

    public EnderSpectatorInventory spectateEnderChest(HumanEntity player, CreationOptions<EnderChestSlot> options, ProfileKey profileKey) {
        EnderSpectatorInventory spectatorInv = spectateEnderChest(player, options);
        enderchests.put(profileKey, spectatorInv);
        enderchestKeys.put(spectatorInv, profileKey);
        return spectatorInv;
    }

    public final CompletableFuture<SpectateResponse<EnderSpectatorInventory>> spectateEnderChest(UUID playerId, String playerName, CreationOptions<EnderChestSlot> options, ProfileId profileId) {
        Player player = plugin.getServer().getPlayer(playerId);
        ProfileKey profileKey = profileId.profileKey;
        if (player != null && pwiHook.isMatchedByProfile(player, profileKey))
            return CompletableFuture.completedFuture(SpectateResponse.succeed(spectateEnderChest(player, options, profileKey)));

        return createOfflineEnderChest(playerId, playerName, options, profileKey);
    }

    @Override
    public CompletableFuture<SpectateResponse<EnderSpectatorInventory>> createOfflineEnderChest(UUID playerId, String playerName, CreationOptions<EnderChestSlot> options) {
        Optional<EnderSpectatorInventory> cached = Optional.ofNullable(cache.getEnderSpectatorInventory(playerId));
        if (cached.isPresent()) return CompletableFuture.completedFuture(SpectateResponse.succeed(cached.get()));

        Location logoutLocation = pwiHook.getDataSource().getLogout(new FakePlayer(playerId, playerName, plugin.getServer()));
        World world = logoutLocation != null ? logoutLocation.getWorld() : plugin.getServer().getWorlds().get(0);
        Group group = pwiHook.getGroupForWorld(world.getName());
        ProfileKey profileKey = new ProfileKey(playerId, group, GameMode.SURVIVAL );
        return createOfflineEnderChest(playerId, playerName, options, profileKey, false);
    }

    @Override
    public CompletableFuture<SaveResponse> saveEnderChest(EnderSpectatorInventory enderChest) {
        ProfileKey profileKey = inventoryKeys.get(enderChest);

        boolean saveVanilla = false;

        Location location = null;
        Player player = plugin.getServer().getPlayer(enderChest.getSpectatedPlayerId());
        if (player != null) location = player.getLocation();
        if (location == null) location = pwiHook.getDataSource().getLogout(new FakePlayer(enderChest.getSpectatedPlayerId(), enderChest.getSpectatedPlayerName(), plugin.getServer()));
        World logoutWorld = location != null ? location.getWorld() : plugin.getServer().getWorlds().get(0);

        if (profileKey == null) {
            saveVanilla = true;
            profileKey = new ProfileKey(enderChest.getSpectatedPlayerId(), pwiHook.getGroupForWorld(logoutWorld.getName()), GameMode.SURVIVAL );
        } else if (!pwiHook.pwiLoadDataOnJoin() && profileKey.getGroup().containsWorld(logoutWorld.getName())) {

            saveVanilla = true;
        }

        return saveEnderChest(enderChest, profileKey, saveVanilla);
    }

    private CompletableFuture<Boolean> isTargetExempted(ProfileKey profileKey, Target target, BiPredicate<Target, String> isExempted) {
        Set<String> worlds = Compat.setCopy(profileKey.getGroup().getWorlds());

        Executor executor =  plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")
                ? scheduler::executeAsync
                : target instanceof UniqueIdTarget
                        ? runnable -> scheduler.executeSyncPlayer(((UniqueIdTarget) target).getUniqueId(), runnable, () -> {})
                        : scheduler::executeSyncGlobal;
        return CompletableFuture.supplyAsync(() -> worlds.stream().anyMatch(world -> isExempted.test(target, world)), executor);
    }

    public CompletableFuture<SpectateResponse<MainSpectatorInventory>> createOfflineInventory(UUID playerId, String playerName, CreationOptions<PlayerInventorySlot> options, ProfileKey profileKey) {
        return createOfflineInventory(playerId, playerName, options, profileKey, pwiHook.isGroupManagedByPWI(profileKey.getGroup()));
    }

    private CompletableFuture<SpectateResponse<MainSpectatorInventory>> createOfflineInventory(UUID playerId, String playerName, CreationOptions<PlayerInventorySlot> options, ProfileKey profileKey, boolean tieToProfile) {

        final Target target = Target.byGameProfile(playerId, playerName);
        CompletableFuture<Boolean> exempted = isTargetExempted(profileKey, target, exempt::isExemptedFromHavingMainInventorySpectated);

        CompletableFuture<SpectateResponse<MainSpectatorInventory>> fromVanillaStorageOfflineInv = exempted.thenCompose(isExempted -> {
            if (isExempted) return CompletableFuture.completedFuture(SpectateResponse.fail(NotCreatedReason.targetHasExemptPermission(target)));
            else return wrapped.createOfflineInventory(playerId, playerName, options);
        });
        if (!pwiHook.pwiManagedInventories()) return fromVanillaStorageOfflineInv;

        FakePlayer player = new FakePlayer(playerId, playerName, plugin.getServer());
        PlayerInventory playerInv = player.getInventory();

        AtomicBoolean loadFromPWI = new AtomicBoolean(true);
        Location location = pwiHook.getDataSource().getLogout(player);
        World logoutWorld = location != null ? location.getWorld() : plugin.getServer().getWorlds().get(0);
        if (!pwiHook.pwiLoadDataOnJoin() && profileKey.getGroup().containsWorld(logoutWorld.getName())) {
            loadFromPWI.set(false);
        }

        return fromVanillaStorageOfflineInv.thenApplyAsync(optionalSpectatorInv -> {

            optionalSpectatorInv.ifSuccess(spectatorInv -> {

                playerInv.setStorageContents(spectatorInv.getStorageContents());
                playerInv.setArmorContents(spectatorInv.getArmourContents());
                playerInv.setExtraContents(spectatorInv.getOffHandContents());
                player.setItemOnCursor(spectatorInv.getCursorContents());

                if (loadFromPWI.get()) {

                    PlayerProfile profile = pwiHook.getOrCreateProfile(player, profileKey);

                    spectatorInv.setStorageContents(Arrays.copyOf(profile.getInventory(), 36));
                    spectatorInv.setArmourContents(Arrays.copyOfRange(profile.getInventory(), 36, 40));
                    spectatorInv.setOffHandContents(Arrays.copyOfRange(profile.getInventory(), 40, 41));

                }

                if (tieToProfile) {
                    inventoryKeys.put(spectatorInv, profileKey);
                    inventories.put(profileKey, spectatorInv);
                }
            });

            return optionalSpectatorInv;
        }, runnable -> scheduler.executeSyncPlayer(playerId, runnable, null));
    }

    public CompletableFuture<SaveResponse> saveInventory(MainSpectatorInventory inventory, ProfileKey profileKey, boolean saveVanilla) {

        if (!pwiHook.pwiManagedInventories()) {

            return wrapped.saveInventory(inventory);

        } else {
            Player player = plugin.getServer().getPlayer(inventory.getSpectatedPlayerId());
            if (player == null) player = new FakePlayer(inventory.getSpectatedPlayerId(), inventory.getSpectatedPlayerName(), plugin.getServer());
            PlayerInventory playerInv = player.getInventory();

            playerInv.setStorageContents(inventory.getStorageContents());
            playerInv.setArmorContents(inventory.getArmourContents());
            playerInv.setItemInOffHand(inventory.getOffHandContents()[0]);
            player.setItemOnCursor(inventory.getCursorContents());

            PlayerProfile profile = pwiHook.getOrCreateProfile(player, profileKey);

            ItemStack[] profileArmour = inventory.getArmourContents();
            ItemStack[] profileInventory = new ItemStack[41];
            System.arraycopy(inventory.getStorageContents(), 0, profileInventory, 0, 36);
            System.arraycopy(inventory.getArmourContents(), 0, profileInventory, 36, 4);
            System.arraycopy(inventory.getOffHandContents(), 0, profileInventory, 40, 1);

            PlayerProfile updatedProfile = profile.copy(
                    profileArmour,
                    profile.getEnderChest(),
                    profileInventory,
                    profile.getAllowFlight(),
                    profile.getDisplayName(),
                    profile.getExhaustion(),
                    profile.getExperience(),
                    profile.isFlying(),
                    profile.getFoodLevel(),
                    profile.getMaxHealth(),
                    profile.getHealth(),
                    profile.getGameMode(),
                    profile.getLevel(),
                    profile.getSaturation(),
                    profile.getPotionEffects(),
                    profile.getFallDistance(),
                    profile.getFireTicks(),
                    profile.getMaximumAir(),
                    profile.getRemainingAir(),
                    profile.getBalance());
            pwiHook.getProfileCache().put(profileKey, updatedProfile);

            final CompletableFuture<SaveResponse> vanillaTask;
            CompletableFuture<Void> saveTask = CompletableFuture.runAsync(() -> pwiHook.getDataSource().savePlayer(profileKey, updatedProfile), scheduler::executeAsync);
            if (saveVanilla) {
                vanillaTask = wrapped.saveInventory(inventory);
                saveTask = CompletableFuture.allOf(saveTask, vanillaTask);
            } else {
                vanillaTask = null;
            }
            return saveTask.thenApply(_void -> vanillaTask == null ? SaveResponse.saved(inventory) : vanillaTask.join());
        }
    }

    public CompletableFuture<SpectateResponse<EnderSpectatorInventory>> createOfflineEnderChest(UUID playerId, String playerName, CreationOptions<EnderChestSlot> options, ProfileKey profileKey) {
        return createOfflineEnderChest(playerId, playerName, options, profileKey, pwiHook.isGroupManagedByPWI(profileKey.getGroup()));
    }

    private CompletableFuture<SpectateResponse<EnderSpectatorInventory>> createOfflineEnderChest(UUID playerId, String playerName, CreationOptions<EnderChestSlot> options, ProfileKey profileKey, boolean tieToProfile) {

        final Target target = Target.byGameProfile(playerId, playerName);
        CompletableFuture<Boolean> exempted = isTargetExempted(profileKey, target, exempt::isExemptedFromHavingEnderchestSpectated);

        CompletableFuture<SpectateResponse<EnderSpectatorInventory>> nonPwiEnderSpectatorFuture = exempted.thenCompose(isExempted -> {
            if (isExempted) return CompletableFuture.completedFuture(SpectateResponse.fail(NotCreatedReason.targetHasExemptPermission(target)));
            else return wrapped.createOfflineEnderChest(playerId, playerName, options);
        });
        if (!pwiHook.pwiManagedEnderChests()) return nonPwiEnderSpectatorFuture;

        FakePlayer player = new FakePlayer(playerId, playerName, plugin.getServer());
        Inventory enderInv = player.getEnderChest();

        AtomicBoolean loadFromPWI = new AtomicBoolean(true);
        Location location = pwiHook.getDataSource().getLogout(player);
        World logoutWorld = location != null ? location.getWorld() : plugin.getServer().getWorlds().get(0);
        if (!pwiHook.pwiLoadDataOnJoin() && profileKey.getGroup().containsWorld(logoutWorld.getName())) {
            loadFromPWI.set(false);
        }

        return nonPwiEnderSpectatorFuture.thenApplyAsync(optionalSpectatorInv -> {
            optionalSpectatorInv.ifSuccess(spectatorInv -> {

                enderInv.setStorageContents(spectatorInv.getStorageContents());

                if (loadFromPWI.get()) {

                    PlayerProfile profile = pwiHook.getOrCreateProfile(player, profileKey);

                    spectatorInv.setStorageContents(profile.getEnderChest());
                }

                if (tieToProfile) {
                    enderchestKeys.put(spectatorInv, profileKey);
                    enderchests.put(profileKey, spectatorInv);
                }
            });

            return optionalSpectatorInv;
        }, runnable -> scheduler.executeSyncPlayer(playerId, runnable, null));
    }

    public CompletableFuture<SaveResponse> saveEnderChest(EnderSpectatorInventory enderChest, ProfileKey profileKey, boolean saveVanilla) {

        if (!pwiHook.pwiManagedEnderChests()) {

            return wrapped.saveEnderChest(enderChest);
        }

        else {
            FakePlayer fakePlayer = new FakePlayer(enderChest.getSpectatedPlayerId(), enderChest.getSpectatedPlayerName(), plugin.getServer());
            Inventory playerEC = fakePlayer.getEnderChest();

            playerEC.setStorageContents(enderChest.getStorageContents());
            PlayerProfile profile = pwiHook.getOrCreateProfile(fakePlayer, profileKey);

            ItemStack[] profileEnderChest = enderChest.getStorageContents();

            PlayerProfile updatedProfile = profile.copy(
                    profile.getArmor(),
                    profileEnderChest,
                    profile.getInventory(),
                    profile.getAllowFlight(),
                    profile.getDisplayName(),
                    profile.getExhaustion(),
                    profile.getExperience(),
                    profile.isFlying(),
                    profile.getFoodLevel(),
                    profile.getMaxHealth(),
                    profile.getHealth(),
                    profile.getGameMode(),
                    profile.getLevel(),
                    profile.getSaturation(),
                    profile.getPotionEffects(),
                    profile.getFallDistance(),
                    profile.getFireTicks(),
                    profile.getMaximumAir(),
                    profile.getRemainingAir(),
                    profile.getBalance());
            pwiHook.getProfileCache().put(profileKey, updatedProfile);

            final CompletableFuture<SaveResponse> vanillaTask;
            CompletableFuture<Void> saveTask = CompletableFuture.runAsync(() -> pwiHook.getDataSource().savePlayer(profileKey, updatedProfile), scheduler::executeAsync);
            if (saveVanilla) {
                vanillaTask = wrapped.saveEnderChest(enderChest);
                saveTask = CompletableFuture.allOf(saveTask, vanillaTask);
            } else {
                vanillaTask = null;
            }
            return saveTask.thenApply(_void -> vanillaTask == null ? SaveResponse.saved(enderChest) : vanillaTask.join());
        }
    }

    private <Slot, S extends SpectatorInventory<Slot>> Optional<S> asLiveInventory(S snapshotInventory, boolean transferToLiveInventory) {
        assert plugin.getServer().isPrimaryThread() : "can't call asLiveInventory asynchronously";

        Player player = plugin.getServer().getPlayer(snapshotInventory.getSpectatedPlayerId());
        if (player == null) return Optional.empty();

        String title = snapshotInventory.getTitle();

        S live = null;

        if (snapshotInventory instanceof MainSpectatorInventory) {
            Mirror<PlayerInventorySlot> mirror = (Mirror<PlayerInventorySlot>) snapshotInventory.getMirror();
            live = (S) spectateInventory(player, title, mirror);
        } else if (snapshotInventory instanceof EnderSpectatorInventory) {
            Mirror<EnderChestSlot> mirror = (Mirror<EnderChestSlot>) snapshotInventory.getMirror();
            live = (S) spectateEnderChest(player, title, mirror);
        } else {
            throw new RuntimeException("Unreachable");
        }

        if (transferToLiveInventory) {
            live.setContents(snapshotInventory.getContents());
        }

        return Optional.of(live);
    }

    private <S extends SpectatorInventory> CompletableFuture<Optional<S>> asSnapShotInventory(S liveSpectatorInventory) {
        assert plugin.getServer().isPrimaryThread() : "can't call asSnapShotInventory asynchronously";

        final UUID id = liveSpectatorInventory.getSpectatedPlayerId();
        final String name = liveSpectatorInventory.getSpectatedPlayerName();
        final Player player = plugin.getServer().getPlayer(id);

        if (player == null) {
            return CompletableFuture.completedFuture(Optional.of(liveSpectatorInventory));
        }

        ProfileKey profileKey = inventoryKeys.get(liveSpectatorInventory);
        if (profileKey != null && !pwiHook.isMatchedByProfile(player, profileKey)) {
            return CompletableFuture.completedFuture(Optional.of(liveSpectatorInventory));
        }

        if (profileKey == null) {
            profileKey = pwiHook.getActiveProfileKey(player);
        }

        final Target target = Target.byPlayer(player);
        final String title = liveSpectatorInventory.getTitle();

        if (liveSpectatorInventory instanceof MainSpectatorInventory) {
            MainSpectatorInventory liveSpectator = (MainSpectatorInventory) liveSpectatorInventory;
            Mirror<PlayerInventorySlot> mirror = liveSpectator.getMirror();
            CreationOptions<PlayerInventorySlot> options = CreationOptions.defaultMainInventory().withTitle(title).withMirror(mirror);

            return (CompletableFuture<Optional<S>>) (Object) createOfflineInventory(id, name, options, profileKey).thenApplyAsync(Function.identity(), runnable -> scheduler.executeSyncPlayer(id, runnable, null));

        } else if (liveSpectatorInventory instanceof EnderSpectatorInventory) {
            EnderSpectatorInventory liveSpectator = (EnderSpectatorInventory) liveSpectatorInventory;
            Mirror<EnderChestSlot> mirror = liveSpectator.getMirror();
            CreationOptions<EnderChestSlot> options = CreationOptions.defaultEnderInventory().withTitle(title).withMirror(mirror);

            return (CompletableFuture<Optional<S>>) (Object) createOfflineEnderChest(id, name, options, profileKey).thenApplyAsync(Function.identity(), runnable -> scheduler.executeSyncPlayer(id, runnable, null));
        }

        assert false : "Unreachable: liveSpectatorInventory is neither a MainSpectatorInventory nor EnderSpectatorInventory";
        return CompletedEmpty.the();
    }

}
