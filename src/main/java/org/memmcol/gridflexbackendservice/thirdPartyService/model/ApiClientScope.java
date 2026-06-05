package org.memmcol.gridflexbackendservice.thirdPartyService.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "api_client_scope")
@Getter
@Setter
public class ApiClientScope {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private String clientId;

    private String scope;
}
