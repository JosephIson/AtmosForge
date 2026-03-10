package org.joseph.atmosforge.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class AtmosSavedData extends SavedData {

    public static final String DATA_NAME = "atmosforge_data";

    private final Map<String, Double> pressureMap = new HashMap<>();

    public AtmosSavedData() {
    }

    public static AtmosSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        AtmosSavedData data = new AtmosSavedData();

        CompoundTag pressures = tag.getCompound("Pressures");
        for (String key : pressures.getAllKeys()) {
            data.pressureMap.put(key, pressures.getDouble(key));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag pressures = new CompoundTag();

        for (Map.Entry<String, Double> entry : pressureMap.entrySet()) {
            pressures.putDouble(entry.getKey(), entry.getValue());
        }

        tag.put("Pressures", pressures);
        return tag;
    }

    public void setPressure(int x, int z, double value) {
        pressureMap.put(key(x, z), value);
        setDirty();
    }

    public double getPressure(int x, int z) {
        return pressureMap.getOrDefault(key(x, z), AtmoConfig.STANDARD_PRESSURE);
    }

    private String key(int x, int z) {
        return x + "," + z;
    }
}
