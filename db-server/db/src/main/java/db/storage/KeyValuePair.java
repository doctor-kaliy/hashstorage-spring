package db.storage;

import java.io.Serializable;
import java.util.Objects;

public class KeyValuePair<A extends Serializable, B extends Serializable> implements Serializable {
    private final A key;
    private final B value;

    public KeyValuePair(A key, B value) {
        this.key = key;
        this.value = value;
    }

    public A getKey() {
        return key;
    }

    public B getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValuePair<A, B> that = (KeyValuePair<A, B>) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
