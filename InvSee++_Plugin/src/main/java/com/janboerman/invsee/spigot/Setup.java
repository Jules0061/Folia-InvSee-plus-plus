package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.OfflinePlayerProvider;
import com.janboerman.invsee.spigot.internal.InvseePlatform;
import com.janboerman.invsee.spigot.internal.version.*;
import com.janboerman.invsee.spigot.internal.NamesAndUUIDs;
import com.janboerman.invsee.spigot.internal.OpenSpectatorsCache;
import com.janboerman.invsee.spigot.api.Scheduler;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

public interface Setup {

    InvseePlatform platform();

    default OfflinePlayerProvider offlinePlayerProvider() {
        return OfflinePlayerProvider.Dummy.INSTANCE;
    }

    static Setup setup(Plugin plugin, Scheduler scheduler, NamesAndUUIDs lookup, OpenSpectatorsCache cache) {
        Server server = plugin.getServer();
        ServerSoftware serverSoftware = ServerSoftware.detect(server);
        plugin.getLogger().info("Detected server software: " + serverSoftware);

        if (serverSoftware == null)
            throw new RuntimeException(SupportedServerSoftware.getUnsupportedPlatformMessage(server));

        SetupProvider provider = SetupSupport.SUPPORTED.getImplementationProvider(serverSoftware);

        if (provider == null) {
            String supportedVersionsMessage = SetupSupport.SUPPORTED.getUnsupportedVersionMessage(serverSoftware.getPlatform(), server);
            String legacyVersionsMessage = LegacyVersions.getLegacyVersionMessage(serverSoftware.getVersion());

            if (legacyVersionsMessage != null) {
                plugin.getLogger().severe(legacyVersionsMessage);
            }

            throw new RuntimeException(supportedVersionsMessage);
        }

        return provider.provide(plugin, lookup, scheduler, cache);
    }

}

//we use separate classes per implementation, to prevent classloading of an incorrect version.

//This fork only targets Paper/Folia 1.21.11 - all other implementations were removed.
class Impl_Paper_1_21_11 extends SetupImpl {
    Impl_Paper_1_21_11(Plugin plugin, NamesAndUUIDs lookup, Scheduler scheduler, OpenSpectatorsCache cache) {
        super(new com.janboerman.invsee.paper.impl_1_21_11.InvseeImpl(plugin, lookup, scheduler, cache), new com.janboerman.invsee.paper.impl_1_21_11.KnownPlayersProvider(plugin, scheduler));
    }
}

//

//holds the version registry in its own class, so that neither SetupImpl nor its subclasses
//are referenced from a superclass static initializer (avoids class loading deadlock).
final class SetupSupport {

    static final SupportedServerSoftware<SetupProvider> SUPPORTED = new SupportedServerSoftware<>();
    static {
        SUPPORTED.registerSupportedVersion(Impl_Paper_1_21_11::new, ServerSoftware.PAPER_1_21_11);
    }

    private SetupSupport() {
    }
}

class SetupImpl implements Setup {

    private final InvseePlatform platform;
    private final OfflinePlayerProvider offlinePlayerProvider;

    SetupImpl(InvseePlatform platform, OfflinePlayerProvider offlinePlayerProvider) {
        this.platform = platform;
        this.offlinePlayerProvider = offlinePlayerProvider;
    }

    @Override
    public InvseePlatform platform() {
        return platform;
    }

    @Override
    public OfflinePlayerProvider offlinePlayerProvider() {
        return offlinePlayerProvider;
    }
}

interface SetupProvider {
    Setup provide(Plugin plugin, NamesAndUUIDs lookup, Scheduler scheduler, OpenSpectatorsCache cache);
}
