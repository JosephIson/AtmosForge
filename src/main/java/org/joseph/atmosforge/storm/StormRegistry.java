package org.joseph.atmosforge.storm;

import org.joseph.atmosforge.data.RegionPos;

import java.util.HashMap;
import java.util.Map;

public final class StormRegistry {

    private final Map<RegionPos, StormCell> storms = new HashMap<>();

    public StormCell getOrCreate(RegionPos pos) {
        return storms.computeIfAbsent(pos, p -> new StormCell());
    }

    public StormCell get(RegionPos pos) {
        return storms.get(pos);
    }

    public Map<RegionPos, StormCell> getAll() {
        return storms;
    }

    public void remove(RegionPos pos) {
        storms.remove(pos);
    }

    public int size() {
        return storms.size();
    }

    public void clear() {
        storms.clear();
    }
}
