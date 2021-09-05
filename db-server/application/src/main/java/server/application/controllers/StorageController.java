package server.application.controllers;

import db.storage.Storage;
import db.storage.exceptions.InvalidKeyTypeException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.application.entities.CreationRequest;
import server.application.entities.GetEntity;
import server.application.entities.PutEntity;
import server.application.exceptions.InvalidStorageNameException;
import server.application.exceptions.NoSuchKeyTypeException;
import server.application.exceptions.NoSuchStorageException;
import server.application.exceptions.StorageAlreadyExistsException;
import server.application.services.StorageService;

import java.io.IOException;
import java.io.Serializable;

@AllArgsConstructor
@RestController
public class StorageController {

    private final static String CREATE = "/create";
    private final static String GET = "/get";
    private final static String PUT = "/put";

    private final StorageService storageService;

    @PostMapping(CREATE)
    public ResponseEntity<String> create(
        @RequestBody CreationRequest creation
    ) throws IOException, InvalidStorageNameException, NoSuchKeyTypeException, StorageAlreadyExistsException {
        storageService.createStorage(creation.getStorageName(), creation.getKeyType());
        return ResponseEntity.ok("Storage " + creation.getStorageName() + " created successfully");
    }

    @PostMapping(PUT)
    public ResponseEntity<String> put(
        @RequestBody PutEntity putEntity
    ) throws IOException, InvalidKeyTypeException, ClassNotFoundException, NoSuchStorageException {
        try (Storage<Serializable> storage = storageService.getExistingStorage(putEntity.getStorageName())) {
            storageService.put(storage, putEntity.getKey(), putEntity.getValue());
            return ResponseEntity.ok("Object successfully mapped to given key");
        }
    }

    @GetMapping(GET)
    public ResponseEntity<Serializable> get(
        @RequestBody GetEntity getEntity
    ) throws IOException, ClassNotFoundException, InvalidKeyTypeException, NoSuchStorageException {
        try (Storage<Serializable> storage = storageService.getExistingStorage(getEntity.getStorageName())) {
            return ResponseEntity.ok(storageService.get(storage, getEntity.getKey()));
        }
    }

    @ExceptionHandler(InvalidStorageNameException.class)
    public ResponseEntity<String> handleInvalidStorageNameException(
        InvalidStorageNameException exception
    ) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidKeyTypeException.class)
    public ResponseEntity<String> handleInvalidKeyTypeException(
        InvalidKeyTypeException exception
    ) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchStorageException.class)
    public ResponseEntity<String> handleNoSuchStorageException(
        NoSuchStorageException exception
    ) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoSuchKeyTypeException.class)
    public ResponseEntity<String> handleNoSuchKeyTypeException(
        NoSuchKeyTypeException exception
    ) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StorageAlreadyExistsException.class)
    public ResponseEntity<String> handleNoSuchKeyTypeException(
        StorageAlreadyExistsException exception
    ) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.CONFLICT);
    }
}
