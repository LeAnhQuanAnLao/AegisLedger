package com.aegisledger.web.controller;

import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.eod.domain.DailyAccountingBalanceSheet;
import com.aegisledger.eod.dto.EodReportDto;
import com.aegisledger.eod.service.EodReconciliationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API controller for End-Of-Day (EOD) reports and reconciliation operations.
 */
@RestController
@RequestMapping("/api/v1/eod")
public class EodController {

    private final EodReconciliationService reconciliationService;

    public EodController(EodReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<EodReportDto>> triggerReconciliation(
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reconciliationDate = date != null ? date : LocalDate.now();
        DailyAccountingBalanceSheet sheet = reconciliationService.runReconciliation(reconciliationDate);
        return ResponseEntity.ok(ApiResponse.ok(EodReportDto.fromEntity(sheet), "EOD reconciliation completed"));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<EodReportDto>>> getRecentReports() {
        List<EodReportDto> reports = reconciliationService.getRecentReports().stream()
            .map(EodReportDto::fromEntity)
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    @GetMapping("/reports/{date}")
    public ResponseEntity<ApiResponse<EodReportDto>> getReportByDate(
        @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reconciliationService.getReportByDate(date)
            .map(sheet -> ResponseEntity.ok(ApiResponse.ok(EodReportDto.fromEntity(sheet))))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
