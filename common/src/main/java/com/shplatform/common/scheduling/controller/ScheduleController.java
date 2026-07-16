package com.shplatform.common.scheduling.controller;

import com.shplatform.common.scheduling.ScheduleConfig;
import com.shplatform.common.scheduling.ScheduleLog;
import com.shplatform.common.scheduling.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule", description = "스케줄 관리 API")
@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "스케줄 설정 목록 조회")
    @GetMapping("/configs")
    public ResponseEntity<List<ScheduleConfig>> getConfigs(
            @RequestParam(value = "moduleName", required = false) String moduleName) {
        if (moduleName != null) {
            return ResponseEntity.ok(scheduleService.getConfigsByModule(moduleName));
        }
        return ResponseEntity.ok(scheduleService.getEnabledConfigs());
    }

    @Operation(summary = "스케줄 설정 생성")
    @PostMapping("/configs")
    public ResponseEntity<ScheduleConfig> createConfig(@RequestBody ScheduleConfig config) {
        return ResponseEntity.ok(scheduleService.createConfig(config));
    }

    @Operation(summary = "스케줄 설정 수정")
    @PutMapping("/configs/{id}")
    public ResponseEntity<ScheduleConfig> updateConfig(
            @PathVariable(value = "id") Long id,
            @RequestBody ScheduleConfig config) {
        return ResponseEntity.ok(scheduleService.updateConfig(id, config));
    }

    @Operation(summary = "스케줄 설정 삭제")
    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable(value = "id") Long id) {
        scheduleService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "스케줄 실행 이력 조회")
    @GetMapping("/configs/{id}/logs")
    public ResponseEntity<List<ScheduleLog>> getLogs(@PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(scheduleService.getRecentLogs(id));
    }
}
