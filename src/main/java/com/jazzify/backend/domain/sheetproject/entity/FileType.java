package com.jazzify.backend.domain.sheetproject.entity;

import com.jazzify.backend.shared.exception.code.SheetProjectErrorCode;

public enum FileType {
    PDF,
    IMAGE,
    MIDI,
    MUSICXML;

    public static FileType fromFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            throw SheetProjectErrorCode.UNSUPPORTED_FILE_TYPE.toException("확장자가 없습니다: " + fileName);
        }
        String ext = fileName.substring(dotIndex).toLowerCase();
        return switch (ext) {
            case ".pdf" -> PDF;
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp" -> IMAGE;
            case ".mid", ".midi" -> MIDI;
            case ".xml", ".musicxml", ".mxl" -> MUSICXML;
            default -> throw SheetProjectErrorCode.UNSUPPORTED_FILE_TYPE.toException("지원하지 않는 확장자: " + ext);
        };
    }
}
