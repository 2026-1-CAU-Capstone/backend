package com.jazzify.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_user")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<SheetProject> sheetProjects = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<ChordProject> chordProjects = new ArrayList<>();

    @Builder
    public User(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
    }
}
