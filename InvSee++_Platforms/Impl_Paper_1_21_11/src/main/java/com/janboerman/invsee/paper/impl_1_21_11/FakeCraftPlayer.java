package com.janboerman.invsee.paper.impl_1_21_11;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.Optional;

public class FakeCraftPlayer extends CraftPlayer {
    public FakeCraftPlayer(CraftServer server, FakeEntityPlayer entity) {
        super(server, entity);
    }

    @Override
    public void setExtraData(ValueOutput tag) {
        super.setExtraData(tag);

        Optional<ValueInput> maybeFreshlyLoaded = loadPlayerTag();
        if (maybeFreshlyLoaded.isPresent()) {
            ValueInput freshlyLoaded = maybeFreshlyLoaded.get();

            Optional<ValueInput> readBukkit = freshlyLoaded.child("bukkit");
            Optional<ValueInput> readPaper = freshlyLoaded.child("Paper");

            Optional<ValueInput> readRootVehicle = freshlyLoaded.child("RootVehicle");

            CompoundTag writeTag = ((TagValueOutput) tag).buildResult();
            CompoundTag writeBukkit = writeTag.getCompoundOrEmpty("bukkit");
            CompoundTag writePaper = writeTag.getCompoundOrEmpty("Paper");

            copyLong(readBukkit, writeBukkit, "lastPlayed");
            copyLong(readPaper, writePaper, "LastSeen");
            copyCompound(readRootVehicle, writeTag, "RootVehicle");
        }
    }

    private static void copyLong(Optional<ValueInput> from, CompoundTag writeTag, String key) {
        from.flatMap(valueInput -> valueInput.getLong(key)).ifPresent(longValue -> writeTag.putLong(key, longValue));
    }

    private static void copyCompound(Optional<ValueInput> from, CompoundTag writeTag, String key) {
        if (from.isPresent() && from.get() instanceof TagValueInput tagValueInput) {
            writeTag.put(key, tagValueInput.input);
        }
    }

    private Optional<ValueInput> loadPlayerTag() {
        return HybridServerSupport.load(server.getHandle().playerIo, getName(), getUniqueId().toString(), ThrowingProblemReporter.INSTANCE, getHandle().registryAccess());
    }

    @Override
    public FakeEntityPlayer getHandle() {

        return (FakeEntityPlayer) this.entity;
    }

}
