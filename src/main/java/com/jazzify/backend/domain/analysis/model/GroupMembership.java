package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ii‑V‑I (or other) group membership for a chord.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMembership {
    private int groupId;
    private String groupType;
    private String role;
    private String variant;
}

