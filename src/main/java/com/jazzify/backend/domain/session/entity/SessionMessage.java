package com.jazzify.backend.domain.session.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.shared.persistence.BaseEntity;

@Entity
@Table(name = "tb_session_message")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionMessage extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Builder
    public SessionMessage(String role, String content, int sortOrder, Session session) {
        this.role = role;
        this.content = content;
        this.sortOrder = sortOrder;
        this.session = session;
    }
}

