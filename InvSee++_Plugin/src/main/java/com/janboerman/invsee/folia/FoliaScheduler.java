package com.janboerman.invsee.folia;

import com.janboerman.invsee.spigot.InvseePlusPlus;
import com.janboerman.invsee.spigot.api.Scheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FoliaScheduler implements Scheduler {

    private final InvseePlusPlus plugin;

    public FoliaScheduler(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void executeSyncPlayer(UUID playerId, Runnable task, Runnable retired) {
        Server server = plugin.getServer();
        Player player = server.getPlayer(playerId);
        if (player != null) {
            EntityScheduler scheduler = player.getScheduler();
            scheduler.run(plugin, scheduledTask -> task.run(), retired);
        } else {
            executeSyncGlobal(task);
        }
    }

    @Override
    public void executeSyncGlobal(Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void executeSyncGlobalRepeatedly(Runnable task, long ticksInitialDelay, long ticksPeriod) {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), Math.max(1L, ticksInitialDelay), ticksPeriod);
    }

    @Override
    public void executeAsync(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public void executeLaterGlobal(Runnable task, long delayTicks) {
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    }

    @Override
    public void executeLaterAsync(Runnable task, long delayTicks) {
        plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks * 50, TimeUnit.MILLISECONDS);
    }
}
