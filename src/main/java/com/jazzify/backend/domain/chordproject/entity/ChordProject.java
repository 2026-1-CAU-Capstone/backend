package com.jazzify.backend.domain.chordproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_chord_project")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordProject extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MusicKey keySignature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private @Nullable Session session;

    @OneToMany(mappedBy = "chordProject", fetch = FetchType.LAZY)
    private List<ChordInfo> chordInfos = new ArrayList<>();

    @Builder
    public ChordProject(String title, MusicKey keySignature, User user, @Nullable Session session) {
        this.title = title;
        this.keySignature = keySignature;
        this.user = user;
        this.session = session;
    }

    public void update(String title, MusicKey key) {
        this.title = title;
        this.keySignature = key;
    }
}

