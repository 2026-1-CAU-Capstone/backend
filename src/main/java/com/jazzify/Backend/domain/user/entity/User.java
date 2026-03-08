package com.jazzify.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.shared.persistence.BaseEntity;

@Entity
@Table(name = "tb_user")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Builder
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
