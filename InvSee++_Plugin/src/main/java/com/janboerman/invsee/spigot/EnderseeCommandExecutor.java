package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.CreationOptions;
import com.janboerman.invsee.spigot.api.EnderSpectatorInventory;
import com.janboerman.invsee.spigot.api.EnderSpectatorInventoryView;
import com.janboerman.invsee.spigot.api.Exempt;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.response.NotOpenedReason;
import com.janboerman.invsee.spigot.api.response.OpenResponse;
import com.janboerman.invsee.spigot.api.response.SpectateResponse;
import com.janboerman.invsee.spigot.api.target.Target;
import com.janboerman.invsee.spigot.api.template.EnderChestSlot;
import com.janboerman.invsee.spigot.perworldinventory.PerWorldInventorySeeApi;
import com.janboerman.invsee.spigot.perworldinventory.PwiCommandArgs;
import com.janboerman.invsee.utils.Either;
import com.janboerman.invsee.utils.StringHelper;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EnderseeCommandExecutor implements CommandExecutor {

    private final InvseePlusPlus plugin;

    public EnderseeCommandExecutor(InvseePlusPlus plugin) {
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

        final CreationOptions<EnderChestSlot> creationOptions = CreationOptions.defaultEnderInventory(plugin)
                .withTitle(plugin.getTitleForEnderChest())
                .withMirror(plugin.getEnderChestMirror())
                .withOfflinePlayerSupport(plugin.offlinePlayerSupport())
                .withUnknownPlayerSupport(plugin.unknownPlayerSupport())
                .withBypassExemptedPlayers(player.hasPermission(Exempt.BYPASS_EXEMPT_ENDERCHEST))
                .withLogOptions(plugin.getLogOptions())
                .withPlaceholderPalette(plugin.getPlaceholderPalette());

        CompletableFuture<SpectateResponse<EnderSpectatorInventory>> pwiFuture = null;

        if (args.length > 1 && api instanceof PerWorldInventorySeeApi pwiApi) {
            String pwiArgument = StringHelper.joinArray(" ", 1, args);

            Either<String, PwiCommandArgs> either = PwiCommandArgs.parse(pwiArgument, pwiApi.getHook());
            if (either.isLeft()) {
                player.sendMessage(ChatColor.RED + either.getLeft());
                return true;
            }

            PwiCommandArgs pwiOptions = either.getRight();
            pwiFuture = CommandFeedback.pwiSpectate(api, pwiApi, uuid, playerNameOrUUID, target, pwiOptions,
                    (uniqueId, playerName, profileId) -> pwiApi.spectateEnderChest(uniqueId, playerName, creationOptions, profileId));
        }

        CompletableFuture<OpenResponse<EnderSpectatorInventoryView>> fut;

        if (pwiFuture != null) {
            fut = pwiFuture.thenApply(response -> response.isSuccess()
                    ? ((PerWorldInventorySeeApi) api).openEnderSpectatorInventory(player, response.getInventory(), creationOptions)
                    : OpenResponse.closed(NotOpenedReason.notCreated(response.getReason())));
        } else {

            if (isUuid) {
                fut = CommandFeedback.fetchUserNameOrDefault(api, uuid)
                        .thenCompose(userName -> api.spectateEnderChest(player, uuid, userName, creationOptions));
            } else {
                fut = api.spectateEnderChest(player, playerNameOrUUID, creationOptions);
            }
        }

        fut.whenComplete((openResponse, throwable) -> CommandFeedback.reportResponse(
                plugin, player, playerNameOrUUID, "enderchest", "enderchests", "ender-chest", openResponse, throwable));

        return true;
    }

}
