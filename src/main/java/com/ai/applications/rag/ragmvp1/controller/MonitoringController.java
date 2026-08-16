package com.ai.applications.rag.ragmvp1.controller;

import com.ai.applications.rag.ragmvp1.service.monitoring.MonitoringService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping
    public String monitoringDashboard(Model model) {
        model.addAttribute("stats", monitoringService.getStatistics());
        return "admin/monitoring";
    }
}
