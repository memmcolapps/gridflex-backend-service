package org.memmcol.gridflexbackendservice.controller;

import org.memmcol.gridflexbackendservice.service.licence.LicenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/licence")
public class LicenceController {

    @Autowired
    private LicenceService licenceService;

    @GetMapping("/validate")
    public Map<String, Object> validate(@RequestParam UUID orgId) {
        return licenceService.validateLicence(orgId);
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam UUID orgId) {
        return licenceService.getLicence(orgId);
    }

    @GetMapping("/deactivate")
    public Map<String, Object> deactivate(@RequestParam UUID orgId) {
        return licenceService.deactivateLicence(orgId);
    }

    @PostMapping("/generate-fingerprint")
    public Map<String, Object> generateFingerprint(@RequestParam UUID orgId) {
        return licenceService.generateFingerprint(orgId);
    }

    @GetMapping("/fingerprint")
    public Map<String, Object> getFingerprint(@RequestParam UUID orgId) {
        return licenceService.getFingerprint(orgId);
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam UUID orgId,
            @RequestBody String licenceContent) {
        return licenceService.uploadLicence(orgId, licenceContent);
    }
}
