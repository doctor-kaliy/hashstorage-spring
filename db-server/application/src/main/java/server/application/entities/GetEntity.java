package server.application.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
public class GetEntity {
    private final String storageName;
    private final Serializable key;
}
