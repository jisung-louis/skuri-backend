package com.skuri.skuri_backend.domain.support.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.support.entity.converter.LegalDocumentBannerLineListJsonConverter;
import com.skuri.skuri_backend.domain.support.entity.converter.LegalDocumentSectionListJsonConverter;
import com.skuri.skuri_backend.domain.support.entity.converter.LegalDocumentStringListJsonConverter;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentBannerLine;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "legal_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalDocument extends BaseTimeEntity {

    @Id
    @Column(name = "document_key", length = 40)
    private String documentKey;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "banner_icon_key", nullable = false, length = 20)
    private LegalDocumentBannerIconKey bannerIconKey;

    @Column(name = "banner_title", nullable = false, length = 200)
    private String bannerTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "banner_tone", nullable = false, length = 20)
    private LegalDocumentBannerTone bannerTone;

    @Convert(converter = LegalDocumentBannerLineListJsonConverter.class)
    @Column(name = "banner_lines", nullable = false, columnDefinition = "json")
    private List<LegalDocumentBannerLine> bannerLines = new ArrayList<>();

    @Convert(converter = LegalDocumentSectionListJsonConverter.class)
    @Column(nullable = false, columnDefinition = "json")
    private List<LegalDocumentSection> sections = new ArrayList<>();

    @Convert(converter = LegalDocumentStringListJsonConverter.class)
    @Column(name = "footer_lines", nullable = false, columnDefinition = "json")
    private List<String> footerLines = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    private LegalDocument(
            String documentKey,
            String title,
            LegalDocumentBannerIconKey bannerIconKey,
            String bannerTitle,
            LegalDocumentBannerTone bannerTone,
            List<LegalDocumentBannerLine> bannerLines,
            List<LegalDocumentSection> sections,
            List<String> footerLines,
            boolean isActive
    ) {
        this.documentKey = documentKey;
        this.title = title;
        this.bannerIconKey = bannerIconKey;
        this.bannerTitle = bannerTitle;
        this.bannerTone = bannerTone;
        this.bannerLines = new ArrayList<>(bannerLines);
        this.sections = new ArrayList<>(sections);
        this.footerLines = new ArrayList<>(footerLines);
        this.isActive = isActive;
    }

    public static LegalDocument create(
            String documentKey,
            String title,
            LegalDocumentBannerIconKey bannerIconKey,
            String bannerTitle,
            LegalDocumentBannerTone bannerTone,
            List<LegalDocumentBannerLine> bannerLines,
            List<LegalDocumentSection> sections,
            List<String> footerLines,
            boolean isActive
    ) {
        return new LegalDocument(
                documentKey,
                title,
                bannerIconKey,
                bannerTitle,
                bannerTone,
                bannerLines,
                sections,
                footerLines,
                isActive
        );
    }

    public void update(
            String title,
            LegalDocumentBannerIconKey bannerIconKey,
            String bannerTitle,
            LegalDocumentBannerTone bannerTone,
            List<LegalDocumentBannerLine> bannerLines,
            List<LegalDocumentSection> sections,
            List<String> footerLines,
            boolean isActive
    ) {
        this.title = title;
        this.bannerIconKey = bannerIconKey;
        this.bannerTitle = bannerTitle;
        this.bannerTone = bannerTone;
        this.bannerLines = new ArrayList<>(bannerLines);
        this.sections = new ArrayList<>(sections);
        this.footerLines = new ArrayList<>(footerLines);
        this.isActive = isActive;
    }
}
