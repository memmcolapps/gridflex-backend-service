package org.memmcol.gridflexbackendservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.memmcol.gridflexbackendservice.model.setup.ApiClientResponse;
import org.memmcol.gridflexbackendservice.model.setup.CreateApiClientRequest;
import org.memmcol.gridflexbackendservice.service.setup.ApiClientService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/client/setup")
@Tag(name = "Admin", description = "Admin Client Setup Management APIs")
@RequiredArgsConstructor
public class SetUpController {
    private final ApiClientService apiClientService;

    @PostMapping
    public Map<String, Object> create(@RequestBody CreateApiClientRequest request) {
        return apiClientService.createClient(request);
    }
}
