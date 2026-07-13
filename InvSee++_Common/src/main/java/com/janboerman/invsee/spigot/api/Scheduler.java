package com.janboerman.invsee.spigot.api;

import java.util.UUID;

public interface Scheduler {

    public void executeSyncPlayer(UUID playerId, Runnable task, Runnable retired);

    public void executeSyncGlobal(Runnable task);

    public void executeSyncGlobalRepeatedly(Runnable task, long ticksInitialDelay, long ticksPeriod);

    public void executeAsync(Runnable task);

    public void executeLaterGlobal(Runnable task, long delayTicks);

    public void executeLaterAsync(Runnable task, long delayTicks);
}
