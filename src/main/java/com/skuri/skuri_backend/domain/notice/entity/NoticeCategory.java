package com.skuri.skuri_backend.domain.notice.entity;

import java.util.Arrays;

public enum NoticeCategory {
    NEWS("새소식", 97),
    ACADEMIC("학사", 96),
    STUDENT("학생", 116),
    SCHOLARSHIP("장학/등록/학자금", 95),
    ADMISSION("입학", 94),
    CAREER("취업/진로개발/창업", 93),
    EVENT("공모/행사", 90),
    GLOBAL("교육/글로벌", 89),
    GENERAL("일반", 87),
    PROCUREMENT("입찰구매정보", 86),
    VOLUNTEER("사회봉사센터", 84),
    DISABILITY_SUPPORT("장애학생지원센터", 83),
    DORMITORY("생활관", 82),
    EXTRA_CURRICULAR("비교과", 80);

    private final String label;
    private final int categoryId;

    NoticeCategory(String label, int categoryId) {
        this.label = label;
        this.categoryId = categoryId;
    }

    public String label() {
        return label;
    }

    public int categoryId() {
        return categoryId;
    }

    public static NoticeCategory fromLabel(String label) {
        return Arrays.stream(values())
                .filter(value -> value.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 공지 카테고리입니다: " + label));
    }
}
