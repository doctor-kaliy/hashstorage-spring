package server.application.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class PutEntity {
    private final String storageName;
    private final Serializable key;
    private final Serializable value;
}
