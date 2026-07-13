package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.Exempt;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import com.janboerman.invsee.spigot.api.MainSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.response.*;
import com.janboerman.invsee.spigot.api.target.Target;
import com.janboerman.invsee.spigot.api.template.PlayerInventorySlot;
import com.janboerman.invsee.spigot.perworldinventory.PerWorldInventorySeeApi;
import com.janboerman.invsee.spigot.perworldinventory.ProfileId;
import com.janboerman.invsee.spigot.perworldinventory.PwiCommandArgs;
import com.janboerman.invsee.utils.Either;
import com.janboerman.invsee.utils.StringHelper;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InvseeCommandExecutor implements CommandExecutor {

    private final InvseePlusPlus plugin;

    public InvseeCommandExecutor(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        final String playerNameOrUUID = args[0];
        final UUID uuid = CommandFeedback.tryParseUuid(playerNameOrUUID);
        final boolean isUuid = uuid != null;

        final InvseeAPI api = plugin.getApi();
        final Target target = isUuid ? Target.byUniqueId(uuid) : Target.byUsername(playerNameOrUUID);
        //TODO why not just: plugin.getInventoryCreationOptions() ?
        final CreationOptions<PlayerInventorySlot> creationOptions = CreationOptions.defaultMainInventory(plugin)
                .withTitle(plugin.getTitleForInventory())
                .withMirror(plugin.getInventoryMirror())
                .withOfflinePlayerSupport(plugin.offlinePlayerSupport())
                .withUnknownPlayerSupport(plugin.unknownPlayerSupport())
                .withBypassExemptedPlayers(player.hasPermission(Exempt.BYPASS_EXEMPT_INVENTORY))
                .withLogOptions(plugin.getLogOptions())
                .withPlaceholderPalette(plugin.getPlaceholderPalette());

        CompletableFuture<SpectateResponse<MainSpectatorInventory>> pwiFuture = null;

        if (args.length > 1 && api instanceof PerWorldInventorySeeApi pwiApi) {
            String pwiArgument = StringHelper.joinArray(" ", 1, args);

            Either<String, PwiCommandArgs> either = PwiCommandArgs.parse(pwiArgument, pwiApi.getHook());
            if (either.isLeft()) {
                player.sendMessage(ChatColor.RED + either.getLeft());
                return true;
            }

            PwiCommandArgs pwiOptions = either.getRight();
            CompletableFuture<Optional<UUID>> uuidFuture = isUuid
                    ? CompletableFuture.completedFuture(Optional.of(uuid))
                    : pwiApi.fetchUniqueId(playerNameOrUUID);

            pwiFuture = uuidFuture.thenCompose(optId -> {
                if (optId.isPresent()) {
                    UUID uniqueId = optId.get();
                    ProfileId profileId = new ProfileId(pwiApi.getHook(), pwiOptions, uniqueId);
                    CompletableFuture<String> userNameFuture = isUuid
                            ? CommandFeedback.fetchUserNameOrDefault(api, uniqueId)
                            : CompletableFuture.completedFuture(playerNameOrUUID);
                    return userNameFuture.thenCompose(playerName -> pwiApi.spectateInventory(uniqueId, playerName, creationOptions, profileId));
                } else {
                    return CompletableFuture.completedFuture(SpectateResponse.fail(NotCreatedReason.targetDoesNotExists(target)));
                }
            });
        }

        CompletableFuture<OpenResponse<MainSpectatorInventoryView>> fut;

        if (pwiFuture != null) {
            //PWI future is not null - open the inventory!
            fut = pwiFuture.thenApply(response -> response.isSuccess()
                        ? ((PerWorldInventorySeeApi) api).openMainSpectatorInventory(player, response.getInventory(), creationOptions)
                        : OpenResponse.closed(NotOpenedReason.notCreated(response.getReason())));
        } else {
            //No PWI argument - just continue with the regular method
            if (isUuid) {
                //convert UUID to username, then spectate the inventory!
                fut = CommandFeedback.fetchUserNameOrDefault(api, uuid)
                        .thenCompose(userName -> api.spectateInventory(player, uuid, userName, creationOptions));
            } else {
                //spectate the target's inventory!
                fut = api.spectateInventory(player, playerNameOrUUID, creationOptions);
            }
        }

        //Gracefully handle failure and faults.
        fut.whenComplete((response, throwable) -> CommandFeedback.reportResponse(
                plugin, player, playerNameOrUUID, "inventory", "inventories", "main-inventory", response, throwable));

        return true;
    }

}
