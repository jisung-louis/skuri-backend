package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.UpsertCafeteriaMenuReactionRequest;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuReactionResponse;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenu;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenuReaction;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuEntryMetadata;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuIdCodec;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuReactionRepository;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeteriaMenuReactionServiceTest {

    @Mock
    private CafeteriaMenuRepository cafeteriaMenuRepository;

    @Mock
    private CafeteriaMenuReactionRepository cafeteriaMenuReactionRepository;

    @InjectMocks
    private CafeteriaMenuReactionService cafeteriaMenuReactionService;

    @Test
    void upsertReaction_좋아요등록시_likeCount가증가한다() {
        String menuId = CafeteriaMenuIdCodec.encode("2026-W08", "rollNoodles", "존슨부대찌개");
        mockWeeklyMenu("존슨부대찌개");
        when(cafeteriaMenuReactionRepository.findById_MemberIdAndId_MenuId("user-1", menuId)).thenReturn(Optional.empty());
        when(cafeteriaMenuReactionRepository.summarizeCounts("2026-W08", Set.of(menuId)))
                .thenReturn(List.of(countProjection(menuId, 5, 2)));

        CafeteriaMenuReactionResponse response = cafeteriaMenuReactionService.upsertReaction(
                "user-1",
                menuId,
                new UpsertCafeteriaMenuReactionRequest(CafeteriaMenuReactionType.LIKE)
        );

        ArgumentCaptor<CafeteriaMenuReaction> captor = ArgumentCaptor.forClass(CafeteriaMenuReaction.class);
        verify(cafeteriaMenuReactionRepository).save(captor.capture());
        assertEquals("user-1", captor.getValue().getId().getMemberId());
        assertEquals(CafeteriaMenuReactionType.LIKE, captor.getValue().getReaction());
        assertEquals(CafeteriaMenuReactionType.LIKE, response.myReaction());
        assertEquals(5, response.likeCount());
        assertEquals(2, response.dislikeCount());
    }

    @Test
    void upsertReaction_싫어요등록시_dislikeCount가증가한다() {
        String menuId = CafeteriaMenuIdCodec.encode("2026-W08", "rollNoodles", "존슨부대찌개");
        mockWeeklyMenu("존슨부대찌개");
        when(cafeteriaMenuReactionRepository.findById_MemberIdAndId_MenuId("user-1", menuId)).thenReturn(Optional.empty());
        when(cafeteriaMenuReactionRepository.summarizeCounts("2026-W08", Set.of(menuId)))
                .thenReturn(List.of(countProjection(menuId, 3, 4)));

        CafeteriaMenuReactionResponse response = cafeteriaMenuReactionService.upsertReaction(
                "user-1",
                menuId,
                new UpsertCafeteriaMenuReactionRequest(CafeteriaMenuReactionType.DISLIKE)
        );

        verify(cafeteriaMenuReactionRepository).save(any(CafeteriaMenuReaction.class));
        assertEquals(CafeteriaMenuReactionType.DISLIKE, response.myReaction());
        assertEquals(3, response.likeCount());
        assertEquals(4, response.dislikeCount());
    }

    @Test
    void upsertReaction_좋아요에서싫어요로전환하면_집계가정확히이동한다() {
        String menuId = CafeteriaMenuIdCodec.encode("2026-W08", "rollNoodles", "존슨부대찌개");
        CafeteriaMenuReaction existing = CafeteriaMenuReaction.create(
                "user-1",
                menuId,
                "2026-W08",
                "rollNoodles",
                "존슨부대찌개",
                CafeteriaMenuReactionType.LIKE
        );
        mockWeeklyMenu("존슨부대찌개");
        when(cafeteriaMenuReactionRepository.findById_MemberIdAndId_MenuId("user-1", menuId)).thenReturn(Optional.of(existing));
        when(cafeteriaMenuReactionRepository.summarizeCounts("2026-W08", Set.of(menuId)))
                .thenReturn(List.of(countProjection(menuId, 2, 6)));

        CafeteriaMenuReactionResponse response = cafeteriaMenuReactionService.upsertReaction(
                "user-1",
                menuId,
                new UpsertCafeteriaMenuReactionRequest(CafeteriaMenuReactionType.DISLIKE)
        );

        verify(cafeteriaMenuReactionRepository, never()).save(any(CafeteriaMenuReaction.class));
        assertEquals(CafeteriaMenuReactionType.DISLIKE, existing.getReaction());
        assertEquals(CafeteriaMenuReactionType.DISLIKE, response.myReaction());
        assertEquals(2, response.likeCount());
        assertEquals(6, response.dislikeCount());
    }

    @Test
    void upsertReaction_reactionNull이면_기존반응을취소한다() {
        String menuId = CafeteriaMenuIdCodec.encode("2026-W08", "rollNoodles", "존슨부대찌개");
        CafeteriaMenuReaction existing = CafeteriaMenuReaction.create(
                "user-1",
                menuId,
                "2026-W08",
                "rollNoodles",
                "존슨부대찌개",
                CafeteriaMenuReactionType.LIKE
        );
        mockWeeklyMenu("존슨부대찌개");
        when(cafeteriaMenuReactionRepository.findById_MemberIdAndId_MenuId("user-1", menuId)).thenReturn(Optional.of(existing));
        when(cafeteriaMenuReactionRepository.summarizeCounts("2026-W08", Set.of(menuId)))
                .thenReturn(List.of(countProjection(menuId, 4, 2)));

        CafeteriaMenuReactionResponse response = cafeteriaMenuReactionService.upsertReaction(
                "user-1",
                menuId,
                new UpsertCafeteriaMenuReactionRequest(null)
        );

        verify(cafeteriaMenuReactionRepository).delete(existing);
        assertEquals(null, response.myReaction());
        assertEquals(4, response.likeCount());
        assertEquals(2, response.dislikeCount());
    }

    @Test
    void upsertReaction_menuId형식이잘못되면_400() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cafeteriaMenuReactionService.upsertReaction(
                        "user-1",
                        "invalid-menu-id",
                        new UpsertCafeteriaMenuReactionRequest(CafeteriaMenuReactionType.LIKE)
                )
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("menuId 형식이 올바르지 않습니다.", exception.getMessage());
    }

    private void mockWeeklyMenu(String title) {
        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of("2026-02-16", Map.of("rollNoodles", List.of(title))),
                Map.of(
                        "2026-02-16",
                        Map.of(
                                "rollNoodles",
                                List.of(new CafeteriaMenuEntryMetadata(title, List.of(), 0, 0))
                        )
                )
        )));
    }

    private CafeteriaMenuReactionRepository.CafeteriaMenuReactionCountProjection countProjection(
            String menuId,
            long likeCount,
            long dislikeCount
    ) {
        return new CafeteriaMenuReactionRepository.CafeteriaMenuReactionCountProjection() {
            @Override
            public String getMenuId() {
                return menuId;
            }

            @Override
            public long getLikeCount() {
                return likeCount;
            }

            @Override
            public long getDislikeCount() {
                return dislikeCount;
            }
        };
    }
}
