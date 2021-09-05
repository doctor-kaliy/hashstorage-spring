package server.application.services;

import db.storage.Storage;
import db.storage.exceptions.InvalidKeyTypeException;
import db.storage.parameters.StorageParameters;
import org.springframework.stereotype.Service;
import server.application.exceptions.InvalidStorageNameException;
import server.application.exceptions.NoSuchKeyTypeException;
import server.application.exceptions.NoSuchStorageException;
import server.application.exceptions.StorageAlreadyExistsException;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class StorageService {

    private static final int DEFAULT_CAPACITY = 4;
    private static final String lineSeparator = System.lineSeparator();

    private boolean exists(final String storageName) {
        return Files.exists(Path.of(storageName));
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Serializable> getKeyToken(final String keyType) throws NoSuchKeyTypeException {
        try {
            return (Class<? extends Serializable>) Class.forName(keyType);
        } catch (ClassNotFoundException ignored) {
            throw new NoSuchKeyTypeException("No such key type: " + keyType);
        }
    }

    public Storage<? extends Serializable> newStorage(
        final String storageName,
        final String keyType
    ) throws NoSuchKeyTypeException, InvalidStorageNameException, IOException, StorageAlreadyExistsException {
        if (exists(storageName)) {
            throw new StorageAlreadyExistsException("Storage " + storageName + " already exists");
        }
        if (storageName.contains(lineSeparator)) {
            throw new InvalidStorageNameException("Storage name can't contain " + lineSeparator);
        }
        Class<? extends Serializable> keyToken = getKeyToken(keyType);
        return Storage.newStorage(
            Path.of(storageName),
            StorageParameters.newBuilder(keyToken).capacity(DEFAULT_CAPACITY).build()
        );
    }

    public void createStorage(
        final String storageName,
        final String keyType
    ) throws InvalidStorageNameException, NoSuchKeyTypeException, IOException, StorageAlreadyExistsException {
        newStorage(storageName, keyType).close();
    }

    public Storage<Serializable> getExistingStorage(
        final String storageName
    ) throws IOException, ClassNotFoundException, NoSuchStorageException {
        if (!exists(storageName)) {
            throw new NoSuchStorageException("Storage " + storageName + " doesn't exist");
        }
        return Storage.loadStorage(Path.of(storageName));
    }

    public void put(
        final Storage<Serializable> storage,
        final Serializable key,
        final Serializable value
    ) throws IOException, InvalidKeyTypeException {
        storage.put(key, value);
    }

    public Serializable get(
        final Storage<Serializable> storage,
        final Serializable key
    ) throws IOException, InvalidKeyTypeException {
        return storage.get(key);
    }
}
