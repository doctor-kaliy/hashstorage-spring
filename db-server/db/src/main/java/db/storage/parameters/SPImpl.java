package db.storage.parameters;

import db.storage.HashFunction;

import java.io.Serializable;
import java.util.Objects;

class SPImpl<K extends Serializable> implements StorageParameters<K> {
    private int size;
    private int capacity;
    final HashFunction<K> hash;
    private final Class<K> keyClass;

    SPImpl(int capacity, HashFunction<K> hash, Class<K> keyClass) {
        this.hash = hash;
        this.keyClass = keyClass;
        size = 0;
        this.capacity = capacity;
    }

    @Override
    public int hash(K key) {
        if (hash == null) {
            return Objects.hash(key);
        } else {
            return hash.hash(key);
        }
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public Class<K> getKeyClass() {
        return keyClass;
    }
}
