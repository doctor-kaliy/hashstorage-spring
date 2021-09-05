package db.storage;

import java.io.Serializable;

public interface HashFunction<T extends Serializable> extends Serializable {
    int hash(T value);
}
