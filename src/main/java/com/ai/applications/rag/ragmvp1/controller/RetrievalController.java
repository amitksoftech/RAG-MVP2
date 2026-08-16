package com.ai.applications.rag.ragmvp1.controller;

import com.ai.applications.rag.ragmvp1.domain.dto.SearchResponse;
import com.ai.applications.rag.ragmvp1.service.retrieval.RetrievalService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/retrieve")
public class RetrievalController {

    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @GetMapping
    public String searchPage(@RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "topK", required = false) Integer topK,
                             Authentication authentication,
                             Model model) {
        model.addAttribute("query", query);
        model.addAttribute("topK", topK != null ? topK : 5);

        if (query != null && !query.isBlank()) {
            SearchResponse response = retrievalService.search(query, topK, authentication);
            model.addAttribute("results", response.results());
            model.addAttribute("queryLogId", response.queryLogId());
            model.addAttribute("resultCount", response.results().size());
        }

        return "retrieve/search";
    }

    @PostMapping("/feedback")
    public String recordFeedback(@RequestParam("queryLogId") UUID queryLogId,
                                 @RequestParam("positive") boolean positive,
                                 RedirectAttributes redirectAttributes) {
        retrievalService.recordFeedback(queryLogId, positive);
        redirectAttributes.addFlashAttribute("feedbackMessage",
                positive ? "Thanks — marked as helpful." : "Thanks — feedback recorded.");
        return "redirect:/retrieve";
    }
}
