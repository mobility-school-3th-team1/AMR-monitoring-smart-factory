package com.sfaas.amr_control_system.controller;

import com.sfaas.amr_control_system.dto.DemoScenarioRequestDto;
import com.sfaas.amr_control_system.service.DemoScenarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo/scenarios")
@RequiredArgsConstructor
public class DemoScenarioController {

    private final DemoScenarioService demoScenarioService;

    @PostMapping("/emergency")
    public ResponseEntity<Map<String, Boolean>> triggerEmergency(@RequestBody DemoScenarioRequestDto request) {
        demoScenarioService.triggerEmergencyError(request.getAmrId());
        return ResponseEntity.ok(Map.of("accepted", true));
    }

    @PostMapping("/charging")
    public ResponseEntity<Map<String, Boolean>> triggerCharging(@RequestBody DemoScenarioRequestDto request) {
        demoScenarioService.triggerLowBatteryCharge(request.getAmrId());
        return ResponseEntity.ok(Map.of("accepted", true));
    }
}
