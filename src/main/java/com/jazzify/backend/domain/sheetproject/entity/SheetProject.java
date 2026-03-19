package com.jazzify.backend.domain.sheetproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_sheet_project")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetProject extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column
    private @Nullable String key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private SheetFile sheetFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private @Nullable Session session;

    @OneToMany(mappedBy = "sheetProject", fetch = FetchType.LAZY)
    private List<ChordInfo> chordInfos = new ArrayList<>();

    @Builder
    public SheetProject(String title, @Nullable String key, User user,
                        SheetFile sheetFile, @Nullable Session session) {
        this.title = title;
        this.key = key;
        this.user = user;
        this.sheetFile = sheetFile;
        this.session = session;
    }
}

