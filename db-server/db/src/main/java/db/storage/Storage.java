package db.storage;

import db.storage.exceptions.InvalidKeyTypeException;
import db.storage.parameters.StorageParameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Storage<A extends Serializable> implements AutoCloseable {
    private static final int BUFFER_SIZE = 0x800000;
    private static final String DATA = "data";
    private static final String PARAMETERS = "parameters";
    private static final String COPY = "copy";
    private static final String TMP = "temporary";

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object object) {
        return (T) object;
    }

    private static class Iteration<T> {
        private Object object;
        private boolean resume;
        private T value;

        private Iteration(T value) {
            this.object = null;
            this.resume = true;
            this.value = value;
        }
    }

    private interface IterationConsumer<T> {
        void apply(Iteration<T> iteration) throws IOException, ClassNotFoundException;
    }

    private interface IO {
        void apply(Path path, int i) throws IOException;
    }

    private interface Output {
        void apply(ObjectOutput output) throws IOException;
    }

    private interface Input {
        Object get(ObjectInput input) throws IOException, ClassNotFoundException;
    }

    private static void write(Output output, Path dst, OpenOption... options) throws IOException {
        try (ObjectOutput objectOutput =
             new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(dst, options), BUFFER_SIZE))) {
            output.apply(objectOutput);
        }
    }

    private static Object read(Input input, Path src, OpenOption... options) throws IOException, ClassNotFoundException {
        try (ObjectInput objectInput =
                     new ObjectInputStream(new BufferedInputStream(Files.newInputStream(src, options), BUFFER_SIZE))) {
            return input.get(objectInput);
        }
    }

    private static Object readParameters(Path parametersPath) throws IOException, ClassNotFoundException {
        return read(ObjectInput::readObject, parametersPath);
    }

    private static <K extends Serializable> void writeParameters(
        StorageParameters<K> parameters,
        Path dst
    ) throws IOException, InvalidKeyTypeException {
        write(output -> output.writeObject(parameters), dst);
    }

    private static void forEachBucket(int buckets, Path dataPath, IO io) throws IOException {
        for (int i = 0; i < buckets; i++) {
            Path bucket = nthBucket(dataPath, i);
            if (Files.exists(bucket)) {
                io.apply(bucket, i);
                Files.delete(bucket);
            }
        }
    }

    private static Path nthBucket(Path dataPath, int n) {
        return dataPath.resolve(Integer.toString(n));
    }

    public static <K extends Serializable> Storage<K> newStorage(
        Path path,
        StorageParameters<K> storageParameters
    ) throws IOException {
        Path parametersPath = path.resolve(PARAMETERS);
        Path dataPath = path.resolve(DATA);
        Files.createDirectory(path);
        Files.createDirectory(dataPath);
        Files.createFile(parametersPath);
        try {
            write(out -> out.writeObject(storageParameters), parametersPath);
            return new Storage<>(path, storageParameters);
        } catch (IOException e) {
            Files.deleteIfExists(parametersPath);
            Files.deleteIfExists(dataPath);
            Files.deleteIfExists(path);
            throw e;
        }
    }

    public static <K extends Serializable> Storage<K> loadStorage(
        Path path
    ) throws IOException, ClassNotFoundException {
        Path parametersPath = path.resolve(PARAMETERS);
        StorageParameters<K> storageParameters = cast(readParameters(parametersPath));
        return new Storage<>(path, storageParameters);
    }

    private final Path path;
    private final Path dataPath;
    private final Path parametersPath;

    private final StorageParameters<A> storageParameters;
    private final ReentrantReadWriteLock lock;

    private Storage(Path path, StorageParameters<A> storageParameters) {
        this.path = path;
        this.dataPath = path.resolve(DATA);
        this.parametersPath = path.resolve(PARAMETERS);
        this.storageParameters = storageParameters;
        this.lock = new ReentrantReadWriteLock(true);
    }

    private int getHash(A key) {
        return (int) (Integer.toUnsignedLong(storageParameters.hash(key)) % storageParameters.getCapacity());
    }

    private Path getBucket(A key) throws IOException {
        int hash = getHash(key);
        Path bucket = nthBucket(dataPath, hash);
        if (Files.notExists(bucket)) {
            Files.createFile(bucket);
        }
        return bucket;
    }

    private Storage<A> copyStorage(int capacity) throws IOException {
        Path otherPath = dataPath.resolve(COPY);
        StorageParameters<A> otherParameters = StorageParameters.copyOf(storageParameters);
        otherParameters.setCapacity(capacity);
        return Storage.newStorage(otherPath, otherParameters);
    }

    private void ensureCapacity() throws IOException {
        int size = storageParameters.getSize();
        int capacity = storageParameters.getCapacity();
        if (size == capacity) {
            Storage<A> other = copyStorage(capacity * 2);
            forEachBucket(capacity, dataPath, (bucket, i) ->
                    iterate(bucket, iteration -> {
                        KeyValuePair<A, Serializable> pair = cast(iteration.object);
                        try {
                            other.put(pair.getKey(), pair.getValue());
                        } catch (InvalidKeyTypeException ignored) {}
                    })
            );
            forEachBucket(capacity * 2, other.dataPath, (otherBucket, i) -> {
                Path bucket = nthBucket(dataPath, i);
                Files.createFile(bucket);
                Files.copy(otherBucket, bucket, StandardCopyOption.REPLACE_EXISTING);
            });
            Files.delete(other.dataPath);
            Files.delete(other.parametersPath);
            Files.delete(other.path);
            storageParameters.setCapacity(size * 2);
        }
    }

    private <T> T iterate(Path bucket, IterationConsumer<T> consumer) throws IOException {
        return iterate(bucket, consumer, null);
    }

    private <T> T iterate(Path bucket, IterationConsumer<T> consumer, T initialValue) throws IOException {
        Iteration<T> iteration = new Iteration<>(initialValue);
        try {
            return cast(read(input -> {
                while (true) {
                    iteration.object = input.readObject();
                    consumer.apply(iteration);
                    if (!iteration.resume) {
                        return iteration.value;
                    }
                }
            }, bucket));
        } catch (EOFException | ClassNotFoundException ignored) { }
        return iteration.value;
    }

    private <K extends A> void instanceCheck(K key) throws InvalidKeyTypeException {
        if (!storageParameters.getKeyClass().isInstance(key)) {
            throw new InvalidKeyTypeException("Invalid key type. Expected " +
                    storageParameters.getKeyClass().getName() +
                    ", found " +
                    key.getClass().getName()
            );
        }
    }

    public Serializable get(A key) throws IOException, InvalidKeyTypeException {
        instanceCheck(key);
        lock.readLock().lock();
        try {
            KeyValuePair<A, Serializable> pair = new KeyValuePair<>(key, null);
            return iterate(getBucket(key), iteration -> {
                if (pair.equals(iteration.object)) {
                    iteration.value = Storage.<KeyValuePair<A, Serializable>>cast(iteration.object).getValue();
                    iteration.resume = false;
                }
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    public <K extends A> void put(K key, Serializable value) throws IOException, InvalidKeyTypeException {
        instanceCheck(key);
        lock.writeLock().lock();
        try {
            ensureCapacity();
            KeyValuePair<K, Serializable> pair = new KeyValuePair<>(key, value);
            Path temporaryBucket = Files.createTempFile(path, TMP, "");
            Path bucket = getBucket(key);
            write(output -> {
                if (!iterate(bucket, iteration -> {
                    if (pair.equals(iteration.object)) {
                        output.writeObject(pair);
                        iteration.value = true;
                    } else {
                        output.writeObject(iteration.object);
                    }
                }, false)) {
                    output.writeObject(pair);
                    storageParameters.setSize(storageParameters.getSize() + 1);
                }
            }, temporaryBucket);

            Files.copy(temporaryBucket, bucket, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(temporaryBucket);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public StorageParameters<A> getStorageParameters() {
        return storageParameters;
    }

    @Override
    public void close() throws IOException {
        try {
            writeParameters(storageParameters, parametersPath);
        } catch (InvalidKeyTypeException ignored) {}
    }
}
