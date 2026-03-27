package com.jazzify.backend.domain.analysis.controller;

import com.jazzify.backend.domain.analysis.dto.request.AnalysisRequest;
import com.jazzify.backend.domain.analysis.service.HarmonicAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final HarmonicAnalysisService analysisService;

    /**
     * Analyze a chord progression.
     *
     * <pre>
     * POST /api/analysis
     * {
     *   "text": "Dm7 | G7 | Cmaj7",
     *   "key": "C",
     *   "title": "Example",
     *   "timeSignature": "4/4"
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> analyze(@Valid @RequestBody AnalysisRequest request) {
        Map<String, Object> result = analysisService.analyze(
                request.text(),
                request.key(),
                request.title(),
                request.timeSignature()
        );
        return ResponseEntity.ok(result);
    }
}

