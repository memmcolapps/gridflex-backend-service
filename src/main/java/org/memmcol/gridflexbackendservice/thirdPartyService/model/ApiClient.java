package org.memmcol.gridflexbackendservice.thirdPartyService.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_client")
@Getter
@Setter
public class ApiClient {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(unique = true)
    private String clientId;

    private String clientName;

    private String clientSecretHash;

    private UUID orgId;

    private Boolean status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public void setClientId(String clientId) {
        this.clientId = clientId != null ? clientId.toLowerCase() : null;
    }
    public void setClientName(String clientName) {
        this.clientName = clientName != null ? clientName.toLowerCase() : null;
    }
}
