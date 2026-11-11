package com.abetterday.dashboard.controller;

import com.abetterday.dashboard.dto.DashboardResponse;
import com.abetterday.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 화면 전체 조회 API. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 화면에 필요한 모든 데이터를 한 번에 조회한다. */
    @GetMapping
    public DashboardResponse get() {
        return dashboardService.getDashboard();
    }
}
