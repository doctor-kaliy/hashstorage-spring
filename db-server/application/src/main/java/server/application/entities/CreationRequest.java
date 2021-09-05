package server.application.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreationRequest {
    private final String storageName;
    private final String keyType;
}
