package com.ai.applications.rag.ragmvp1.controller;

import com.ai.applications.rag.ragmvp1.domain.dto.IngestionStatistics;
import com.ai.applications.rag.ragmvp1.service.document.AdminDocumentService;
import com.ai.applications.rag.ragmvp1.service.monitoring.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StatisticsService statisticsService;
    private final AdminDocumentService adminDocumentService;

    public AdminController(StatisticsService statisticsService, AdminDocumentService adminDocumentService) {
        this.statisticsService = statisticsService;
        this.adminDocumentService = adminDocumentService;
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        IngestionStatistics statistics = statisticsService.getStatistics();
        model.addAttribute("statistics", statistics);
        return "admin/statistics";
    }

    @PostMapping("/documents/{documentId}/delete")
    public String deleteDocument(@PathVariable UUID documentId, RedirectAttributes redirectAttributes) {
        adminDocumentService.deleteDocument(documentId);
        redirectAttributes.addFlashAttribute("message", "Document deleted successfully.");
        return "redirect:/documents";
    }
}
