package com.jazzify.backend.shared.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

import com.jazzify.backend.shared.persistence.converter.UuidBinaryConverter;

@Getter
@NullMarked
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Convert(converter = UuidBinaryConverter.class)
    @Column(nullable = false, unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private @Nullable UUID publicId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private @Nullable LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private @Nullable LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
    }
}
