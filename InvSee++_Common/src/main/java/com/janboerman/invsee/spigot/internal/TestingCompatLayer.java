package com.janboerman.invsee.spigot.internal;

import java.util.UUID;

public interface TestingCompatLayer {

    Object loadPlayerSaveCompound(UUID playerId, String playerName);
}
