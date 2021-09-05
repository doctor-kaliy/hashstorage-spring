package db.storage.parameters;

import java.io.Serializable;

public interface StorageParameters<K extends Serializable> extends Serializable {
    int hash(K key);
    void setSize(int size);
    void setCapacity(int capacity);
    int getSize();
    int getCapacity();
    Class<K> getKeyClass();

    static <K extends Serializable> StorageParametersBuilder<K> newBuilder(Class<K> keyClass) {
        return new StorageParametersBuilder<>(keyClass);
    }

    static <K extends Serializable> StorageParameters<K> copyOf(
        StorageParameters<K> other
    ) {
        return StorageParameters.newBuilder(other.getKeyClass())
            .capacity(other.getCapacity())
            .hash(((SPImpl<K>) other).hash)
            .build();
    }
}
