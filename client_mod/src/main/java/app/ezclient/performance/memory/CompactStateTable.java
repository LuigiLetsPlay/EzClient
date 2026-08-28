package app.ezclient.performance.memory;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Array-backed state table for EzClient-owned immutable state domains.
 *
 * <p>Property identities receive a process-wide unsigned-byte id. A schema stores its property
 * ids and canonical value arrays once; each state stores only one value ordinal per property.
 * Neighbor transitions are a single flat array addressed by precomputed offsets. No map, entry,
 * node, iterator, or boxed index is allocated while reading a state.</p>
 *
 * <p>Minecraft 26.2's {@code StateHolder} already uses the same property/value-array strategy,
 * so this class intentionally does not replace that ABI-sensitive base-class storage.</p>
 */
public final class CompactStateTable<P, V> {
    private static final int MAX_BYTE_IDS = 256;
    private static final IdentityHashMap<Object, Integer> GLOBAL_PROPERTY_IDS = new IdentityHashMap<>();

    private final byte[] propertyIds;
    private final Object[][] allowedValues;
    private final int[] neighborOffsets;
    private final State<P, V>[] states;
    private final State<P, V>[] neighbors;

    @SuppressWarnings("unchecked")
    public CompactStateTable(List<P> properties, List<? extends List<V>> valuesByProperty) {
        if (properties.size() != valuesByProperty.size()) {
            throw new IllegalArgumentException("Every property requires one value domain");
        }
        if (properties.size() > MAX_BYTE_IDS) {
            throw new IllegalArgumentException("At most 256 properties are supported");
        }

        propertyIds = new byte[properties.size()];
        allowedValues = new Object[properties.size()][];
        neighborOffsets = new int[properties.size() + 1];
        long combinations = 1;
        int neighborWidth = 0;
        for (int i = 0; i < properties.size(); i++) {
            propertyIds[i] = (byte) globalId(Objects.requireNonNull(properties.get(i), "property"));
            List<V> domain = valuesByProperty.get(i);
            if (domain.isEmpty() || domain.size() > MAX_BYTE_IDS) {
                throw new IllegalArgumentException("A property domain must contain 1..256 values");
            }
            allowedValues[i] = domain.toArray();
            neighborOffsets[i] = neighborWidth;
            neighborWidth = Math.addExact(neighborWidth, domain.size());
            combinations = Math.multiplyExact(combinations, domain.size());
        }
        neighborOffsets[properties.size()] = neighborWidth;
        if (combinations > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("State cartesian product is too large");
        }

        states = (State<P, V>[]) new State[combinations == 0 ? 1 : (int) combinations];
        neighbors = (State<P, V>[]) new State[Math.multiplyExact(states.length, neighborWidth)];
        buildStates(0, new byte[propertyIds.length], new int[]{0});
        buildNeighbors(neighborWidth);
    }

    public int size() {
        return states.length;
    }

    public State<P, V> state(int flatIndex) {
        return states[flatIndex];
    }

    private void buildStates(int propertyIndex, byte[] ordinals, int[] outputIndex) {
        if (propertyIndex == propertyIds.length) {
            byte[] packed = ordinals.clone();
            states[outputIndex[0]] = new State<>(this, outputIndex[0]++, packed);
            return;
        }
        for (int ordinal = 0; ordinal < allowedValues[propertyIndex].length; ordinal++) {
            ordinals[propertyIndex] = (byte) ordinal;
            buildStates(propertyIndex + 1, ordinals, outputIndex);
        }
    }

    private void buildNeighbors(int neighborWidth) {
        for (State<P, V> state : states) {
            int base = state.flatIndex * neighborWidth;
            for (int property = 0; property < propertyIds.length; property++) {
                for (int ordinal = 0; ordinal < allowedValues[property].length; ordinal++) {
                    byte old = state.valueOrdinals[property];
                    state.valueOrdinals[property] = (byte) ordinal;
                    neighbors[base + neighborOffsets[property] + ordinal] = stateFor(state.valueOrdinals);
                    state.valueOrdinals[property] = old;
                }
            }
        }
    }

    private State<P, V> stateFor(byte[] ordinals) {
        int index = 0;
        for (int property = 0; property < ordinals.length; property++) {
            index = index * allowedValues[property].length + Byte.toUnsignedInt(ordinals[property]);
        }
        return states[index];
    }

    private int propertyIndex(P property) {
        int id = existingGlobalId(property);
        if (id < 0) {
            return -1;
        }
        for (int i = 0; i < propertyIds.length; i++) {
            if (Byte.toUnsignedInt(propertyIds[i]) == id) {
                return i;
            }
        }
        return -1;
    }

    private static synchronized int globalId(Object property) {
        Integer existing = GLOBAL_PROPERTY_IDS.get(property);
        if (existing != null) {
            return existing;
        }
        int id = GLOBAL_PROPERTY_IDS.size();
        if (id >= MAX_BYTE_IDS) {
            throw new IllegalStateException("Global compact property id space exhausted");
        }
        GLOBAL_PROPERTY_IDS.put(property, id);
        return id;
    }

    private static synchronized int existingGlobalId(Object property) {
        Integer id = GLOBAL_PROPERTY_IDS.get(property);
        return id == null ? -1 : id;
    }

    public static final class State<P, V> {
        private final CompactStateTable<P, V> table;
        private final int flatIndex;
        private final byte[] valueOrdinals;

        private State(CompactStateTable<P, V> table, int flatIndex, byte[] valueOrdinals) {
            this.table = table;
            this.flatIndex = flatIndex;
            this.valueOrdinals = valueOrdinals;
        }

        @SuppressWarnings("unchecked")
        public V get(P property) {
            int propertyIndex = table.propertyIndex(property);
            if (propertyIndex < 0) {
                return null;
            }
            return (V) table.allowedValues[propertyIndex]
                    [Byte.toUnsignedInt(valueOrdinals[propertyIndex])];
        }

        public State<P, V> with(P property, V value) {
            int propertyIndex = table.propertyIndex(property);
            if (propertyIndex < 0) {
                throw new IllegalArgumentException("Unknown property");
            }
            int ordinal = identityIndexOf(table.allowedValues[propertyIndex], value);
            if (ordinal < 0) {
                throw new IllegalArgumentException("Value is outside the property's domain");
            }
            int neighborWidth = table.neighborOffsets[table.propertyIds.length];
            return table.neighbors[flatIndex * neighborWidth
                    + table.neighborOffsets[propertyIndex] + ordinal];
        }

        public byte[] packedOrdinals() {
            return valueOrdinals.clone();
        }

        private static int identityIndexOf(Object[] values, Object wanted) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == wanted || Objects.equals(values[i], wanted)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public String toString() {
            return "CompactState" + Arrays.toString(valueOrdinals);
        }
    }
}
