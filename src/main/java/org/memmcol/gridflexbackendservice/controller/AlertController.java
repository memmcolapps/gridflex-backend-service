package org.memmcol.gridflexbackendservice.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/service/alerts")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);

    @PostMapping
    public ResponseEntity<String> receiveAlert(@RequestBody Map<String, Object> payload) {
        logger.info("Received alert from Alertmanager: {}", payload);
        // Later: parse and save into DB here
        return new ResponseEntity<>("Alert received", HttpStatus.OK);
    }
}


//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/alerts")
//public class AlertController {
//
//    private final UptimeRepository repository;
//
//    public AlertController(UptimeRepository repository) {
//        this.repository = repository;
//    }
//
//    @PostMapping
//    public ResponseEntity<String> receiveAlert(@RequestBody Map<String, Object> alert) {
//        List<Map<String, Object>> alerts = (List<Map<String, Object>>) alert.get("alerts");
//
//        for (Map<String, Object> a : alerts) {
//            String status = (String) a.get("status"); // firing/resolved
//            Map<String, String> labels = (Map<String, String>) a.get("labels");
//            String alertName = labels.get("alertname");
//
//            if ("InstanceDown".equals(alertName) && "firing".equals(status)) {
//                repository.save(new UptimeLog(LocalDateTime.now(), false));
//            } else if ("InstanceUp".equals(alertName) && "firing".equals(status)) {
//                repository.save(new UptimeLog(LocalDateTime.now(), true));
//            }
//        }
//
//        return ResponseEntity.ok("Alert received");
//    }
//}
