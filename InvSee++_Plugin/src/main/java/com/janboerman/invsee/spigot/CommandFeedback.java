package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.SpectatorInventory;
import com.janboerman.invsee.spigot.api.response.ImplementationFault;
import com.janboerman.invsee.spigot.api.response.InventoryNotCreated;
import com.janboerman.invsee.spigot.api.response.InventoryOpenEventCancelled;
import com.janboerman.invsee.spigot.api.response.NotCreatedReason;
import com.janboerman.invsee.spigot.api.response.NotOpenedReason;
import com.janboerman.invsee.spigot.api.response.OfflineSupportDisabled;
import com.janboerman.invsee.spigot.api.response.OpenResponse;
import com.janboerman.invsee.spigot.api.response.SpectateResponse;
import com.janboerman.invsee.spigot.api.response.TargetDoesNotExist;
import com.janboerman.invsee.spigot.api.response.TargetHasExemptPermission;
import com.janboerman.invsee.spigot.api.response.UnknownTarget;
import com.janboerman.invsee.spigot.api.target.Target;
import com.janboerman.invsee.spigot.perworldinventory.PerWorldInventorySeeApi;
import com.janboerman.invsee.spigot.perworldinventory.ProfileId;
import com.janboerman.invsee.spigot.perworldinventory.PwiCommandArgs;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** Shared bits of /invsee and /endersee. */
final class CommandFeedback {

    private CommandFeedback() {
    }

    /** @return the parsed UUID, or null if the input is not a UUID. */
    static UUID tryParseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static CompletableFuture<String> fetchUserNameOrDefault(InvseeAPI api, UUID uuid) {
        return api.fetchUserName(uuid).thenApply(o -> o.orElse("InvSee++ Player")).exceptionally(t -> "InvSee++ Player");
    }

    @FunctionalInterface
    interface PwiSpectateFunction<T extends SpectatorInventory<?>> {
        CompletableFuture<SpectateResponse<T>> spectate(UUID uniqueId, String playerName, ProfileId profileId);
    }

    /** Resolves the target's unique id and username, then spectates through the PerWorldInventory api. */
    static <T extends SpectatorInventory<?>> CompletableFuture<SpectateResponse<T>> pwiSpectate(InvseeAPI api, PerWorldInventorySeeApi pwiApi,
                                                                  UUID uuid, String playerNameOrUUID, Target target,
                                                                  PwiCommandArgs pwiOptions, PwiSpectateFunction<T> spectate) {
        final boolean isUuid = uuid != null;
        CompletableFuture<Optional<UUID>> uuidFuture = isUuid
                ? CompletableFuture.completedFuture(Optional.of(uuid))
                : pwiApi.fetchUniqueId(playerNameOrUUID);

        return uuidFuture.thenCompose(optId -> {
            if (optId.isPresent()) {
                UUID uniqueId = optId.get();
                ProfileId profileId = new ProfileId(pwiApi.getHook(), pwiOptions, uniqueId);
                CompletableFuture<String> userNameFuture = isUuid
                        ? fetchUserNameOrDefault(api, uniqueId)
                        : CompletableFuture.completedFuture(playerNameOrUUID);
                return userNameFuture.thenCompose(playerName -> spectate.spectate(uniqueId, playerName, profileId));
            } else {
                return CompletableFuture.completedFuture(SpectateResponse.fail(NotCreatedReason.targetDoesNotExists(target)));
            }
        });
    }

    //ImplementationFault is deprecated upstream, but implementations still return it.
    @SuppressWarnings("deprecation")
    static void reportResponse(InvseePlusPlus plugin, Player player, String targetName,
                               String noun, String nounPlural, String logContext,
                               OpenResponse<?> response, Throwable throwable) {
        if (throwable != null) {
            player.sendMessage(ChatColor.RED + "An error occurred while trying to open " + targetName + "'s " + noun + ".");
            plugin.getLogger().log(Level.SEVERE, "Error while trying to create " + logContext + " spectator inventory", throwable);
        } else if (!response.isOpen()) {
            NotOpenedReason notOpenedReason = response.getReason();
            if (notOpenedReason instanceof InventoryOpenEventCancelled) {
                player.sendMessage(ChatColor.RED + "Another plugin prevented you from spectating " + targetName + "'s " + noun + ".");
            } else if (notOpenedReason instanceof InventoryNotCreated inventoryNotCreated) {
                player.sendMessage(ChatColor.RED + switch (inventoryNotCreated.getNotCreatedReason()) {
                    case TargetDoesNotExist ignored -> "Player " + targetName + " does not exist.";
                    case UnknownTarget ignored -> "Player " + targetName + " has not logged onto the server yet.";
                    case TargetHasExemptPermission ignored -> "Player " + targetName + " is exempted from being spectated.";
                    case ImplementationFault ignored -> "An internal fault occurred when trying to load " + targetName + "'s " + noun + ".";
                    case OfflineSupportDisabled ignored -> "Spectating offline players' " + nounPlural + " is disabled.";
                    case null, default -> "Could not create " + targetName + "'s " + noun + " for an unknown reason.";
                });
            } else {
                player.sendMessage(ChatColor.RED + "Could not open " + targetName + "'s " + noun + " for an unknown reason.");
            }
        } //else: it opened successfully: nothing to do here!
    }
}
