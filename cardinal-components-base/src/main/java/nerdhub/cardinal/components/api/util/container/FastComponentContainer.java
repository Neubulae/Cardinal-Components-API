/*
 * Cardinal-Components-API
 * Copyright (C) 2019-2021 OnyxStudios
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nerdhub.cardinal.components.api.util.container;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import dev.onyxstudios.cca.internal.base.ComponentRegistryImpl;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.Component;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A {@link Component} container with a fast, small-footprint implementation
 * based on {@link Int2ObjectMap}
 *
 * <p> <b>Note that this implementation is not synchronized.</b>
 * If multiple threads access an indexed container concurrently, and at least one of the threads
 * modifies the container structurally, it must be synchronized externally.
 * (A structural modification is any operation that adds one or more mappings;
 * merely changing the value associated with a key that an instance already contains is not
 * a structural modification.) This is typically accomplished by synchronizing on some object
 * that naturally encapsulates the container.
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "3.0.0")
public class FastComponentContainer<C extends Component> extends AbstractComponentContainer<C> {
    private final BitSet containedTypes;
    private final Int2ObjectLinkedOpenHashMap<C> vals;

    public FastComponentContainer() {
        this(Hash.DEFAULT_INITIAL_SIZE);
    }

    /**
     * @param expected the expected number of <em>dynamically added</em> elements in the container
     */
    public FastComponentContainer(int expected) {
        this.containedTypes = new BitSet(((ComponentRegistryImpl) ComponentRegistry.INSTANCE).size());
        this.vals = new Int2ObjectLinkedOpenHashMap<>(expected, Hash.VERY_FAST_LOAD_FACTOR);
    }

    @SuppressWarnings("unused") // called by generated subclasses
    protected void addContainedType(ComponentType<?> type) {
        this.containedTypes.set(type.getRawId());
    }

    /**
     * Returns the number of components held by this container.
     *
     * @return the number of components in this container
     */
    @Override
    public int size() {
        return this.containedTypes.cardinality();
    }

    @SuppressWarnings("unused") // called by generated factories to adjust the initial size of future containers
    public final int dynamicSize() {
        return this.vals.size();
    }

    @Override
    public boolean containsKey(ComponentType<?> key) {
        return this.containedTypes.get(key.getRawId());
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override   // overridden by generated subclasses
    @SuppressWarnings("unchecked")
    public <T extends Component> T get(ComponentType<T> key) {
        return (T) this.vals.get(key.getRawId());
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public C put(ComponentType<?> key, C value) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(value);
        Preconditions.checkArgument(key.getComponentClass().isInstance(value), value + " is not of type " + key);
        Preconditions.checkState(this.canBeAssigned(key), "Component type " + key + " was already defined with value " + this.get(key) + ", cannot replace with " + value);
        this.containedTypes.set(key.getRawId());
        // Invalidate the key set in case it was assigned to the static keys
        if (this.vals.isEmpty()) this.keySet = null;
        return this.vals.put(key.getRawId(), value);
    }

    // overridden by generated subclasses
    protected boolean canBeAssigned(ComponentType<?> key) {
        return !this.containsKey(key);
    }

    // Views

    /**
     * This field is initialized to contain an instance of the entry set
     * view the first time this view is requested.  The view is stateless,
     * so there's no reason to create more than one.
     */
    private Set<Map.Entry<ComponentType<?>, C>> entrySet;
    private Set<ComponentType<?>> keySet;
    private Collection<C> values;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The returned set obeys the general contract outlined in
     * {@link Map#keySet()}.  The set's iterator will return the keys
     * in their registration order.
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<ComponentType<?>> keySet() {
        Set<ComponentType<?>> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        return this.keySet = this.vals.isEmpty() ? this.staticKeySet() : new KeySet();
    }

    // Overridden by generated subclasses
    protected Set<ComponentType<?>> staticKeySet() {
        return Collections.emptySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The returned collection obeys the general contract outlined in
     * {@link Map#values()}.  The collection's iterator will return the
     * values in the order their corresponding keys appear in map,
     * which is their natural order (the order in which the enum constants
     * are declared).
     *
     * @return a collection view of the values contained in this map
     */
    @Override
    public Collection<C> values() {
        Collection<C> vs = this.values;
        if (vs != null) {
            return vs;
        }
        return this.values = new Values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The returned set obeys the general contract outlined in
     * {@link Map#keySet()}.  The set's iterator will return the
     * mappings in the order their keys appear in map, which is their
     * natural order (the order in which the enum constants are declared).
     *
     * @return a set view of the mappings contained in this enum map
     */
    @Override
    public Set<Map.Entry<ComponentType<?>, C>> entrySet() {
        Set<Map.Entry<ComponentType<?>, C>> es = this.entrySet;
        if (es != null) {
            return es;
        }
        return this.entrySet = new EntrySet();
    }

    private class EntrySet extends AbstractSet<Map.Entry<ComponentType<?>, C>> {
        @Override
        public Iterator<Map.Entry<ComponentType<?>, C>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            Object key = entry.getKey();
            return key instanceof ComponentType && Objects.equals(entry.getValue(), FastComponentContainer.this.get(key));
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return FastComponentContainer.this.size();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    private class KeySet extends AbstractSet<ComponentType<?>> {
        @Override
        public Iterator<ComponentType<?>> iterator() {
            Iterator<ComponentType<?>> i1 = FastComponentContainer.this.staticKeySet().iterator();
            Iterator<ComponentType<?>> i2 = new KeyIterator();
            return Iterators.concat(i1, i2);
        }

        @Override
        public int size() {
            return FastComponentContainer.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return FastComponentContainer.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }
    }

    private class Values extends AbstractCollection<C> {
        @Override
        public Iterator<C> iterator() {
            @SuppressWarnings("unchecked") Iterator<C> i1 = (Iterator<C>) Iterators.<ComponentType<?>, Component>transform(FastComponentContainer.this.staticKeySet().iterator(), FastComponentContainer.this::get);
            ObjectIterator<C> i2 = FastComponentContainer.this.vals.values().iterator();
            return Iterators.concat(i1, i2);
        }

        @Override
        public int size() {
            return FastComponentContainer.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return FastComponentContainer.this.containsValue(o);
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }
    }

    private final class KeyIterator implements Iterator<ComponentType<?>> {
        private final IntIterator wrapped = FastComponentContainer.this.vals.keySet().iterator();

        @Override
        public boolean hasNext() {
            return this.wrapped.hasNext();
        }

        @Override
        public ComponentType<?> next() {
            return ComponentRegistryImpl.byRawId(this.wrapped.nextInt());
        }
    }

    private final class EntryIterator implements Iterator<Entry<ComponentType<?>, C>> {
        private final Iterator<ComponentType<?>> staticWrapped = FastComponentContainer.this.staticKeySet().iterator();
        private final ObjectBidirectionalIterator<Int2ObjectMap.Entry<C>> dynamicWrapped = FastComponentContainer.this.vals.int2ObjectEntrySet().fastIterator();

        @Override
        public boolean hasNext() {
            return this.staticWrapped.hasNext() || this.dynamicWrapped.hasNext();
        }

        @Override
        public Entry next() {
            if (this.staticWrapped.hasNext()) {
                @SuppressWarnings("unchecked") ComponentType<? extends C> next = (ComponentType<? extends C>) this.staticWrapped.next();
                return new Entry(next, next.getFromContainer(FastComponentContainer.this));
            } else if (this.dynamicWrapped.hasNext()) {
                Int2ObjectMap.Entry<C> e = this.dynamicWrapped.next();
                return new Entry(ComponentRegistryImpl.byRawId(e.getIntKey()), e.getValue());
            } else {
                throw new NoSuchElementException();
            }
        }

        private final class Entry implements Map.Entry<ComponentType<?>, C> {
            private final ComponentType<?> key;
            private final C value;

            private Entry(ComponentType<?> key, C value) {
                this.key = key;
                this.value = value;
            }

            @Override
            public ComponentType<?> getKey() {
                return this.key;
            }

            @Override
            public C getValue() {
                return this.value;
            }

            @Nullable
            @Override
            public C setValue(C value) {
                return FastComponentContainer.this.put(this.getKey(), value);
            }
        }
    }
}
