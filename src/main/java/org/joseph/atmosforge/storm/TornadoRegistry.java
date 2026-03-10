package org.joseph.atmosforge.storm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Holds all active TornadoCell instances for a ServerLevel.
 * Managed per-world through WorldDataManager.
 */
public final class TornadoRegistry {

    private final List<TornadoCell> tornados = new ArrayList<>();

    public List<TornadoCell> getAll() {
        return tornados;
    }

    public void add(TornadoCell cell) {
        tornados.add(cell);
    }

    public void removeIf(Predicate<TornadoCell> pred) {
        tornados.removeIf(pred);
    }

    public int size() {
        return tornados.size();
    }

    public void clear() {
        tornados.clear();
    }
}