package com.example.soloxray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

/** Keeps the glowing markers in sync with the latest scan. */
public final class Markers {
    private Markers() {}

    public static final int MAX_MARKERS = 256;

    private static final Map<Long, Entity> ACTIVE = new HashMap<>();
    private static int nextFakeId = -1_500_000; // negative ids can't clash with real entities

    public static void sync(ClientLevel level, List<Hit> hits) {
        Set<Long> wanted = new HashSet<>();
        for (int i = 0; i < Math.min(MAX_MARKERS, hits.size()); i++) {
            Hit hit = hits.get(i);
            long key = hit.pos().asLong();
            wanted.add(key);
            if (!ACTIVE.containsKey(key)) {
                ACTIVE.put(key, Compat.spawnMarker(level, hit, nextFakeId--));
            }
        }
        Iterator<Map.Entry<Long, Entity>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Entity> e = it.next();
            if (!wanted.contains(e.getKey())) {
                Compat.removeMarker(level, e.getValue());
                it.remove();
            }
        }
    }

    /** Remove everything from a level that still exists. */
    public static void clear(ClientLevel level) {
        if (level != null) {
            for (Entity e : ACTIVE.values()) Compat.removeMarker(level, e);
        }
        ACTIVE.clear();
    }

    /** Level is gone (disconnect/world change): just forget the references. */
    public static void forget() {
        ACTIVE.clear();
    }
}
