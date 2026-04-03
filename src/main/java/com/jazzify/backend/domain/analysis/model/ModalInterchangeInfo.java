package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModalInterchangeInfo {
    private String sourceMode;
    private String borrowedDegree;
    private boolean isCommonBorrow;
    private List<ModalInterchangeMatch> allPossibleSources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModalInterchangeMatch {
        private String sourceMode;
        private String borrowedDegree;
        private boolean isCommonBorrow;
    }
}

