package db.storage.parameters;

import db.storage.HashFunction;

import java.io.Serializable;

public class StorageParametersBuilder<K extends Serializable> {
    private static final int DEFAULT_CAPACITY = 2;

    private int optionalCapacity = DEFAULT_CAPACITY;
    private HashFunction<K> hashFunction = null;
    private final Class<K> keyClass;

    StorageParametersBuilder(Class<K> keyClass) {
        this.keyClass = keyClass;
    }

    public StorageParametersBuilder<K> capacity(int capacity) {
        this.optionalCapacity = capacity;
        return this;
    }

    public StorageParametersBuilder<K> hash(HashFunction<K> hashFunction) {
        this.hashFunction = hashFunction;
        return this;
    }

    public StorageParameters<K> build() {
        return new SPImpl<>(optionalCapacity, hashFunction, keyClass);
    }
}