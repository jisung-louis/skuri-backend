package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.UpsertCafeteriaMenuReactionRequest;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuReactionResponse;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenu;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenuReaction;
import com.skuri.skuri_backend.domain.support.exception.CafeteriaMenuEntryNotFoundException;
import com.skuri.skuri_backend.domain.support.exception.CafeteriaMenuNotFoundException;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuIdCodec;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuIdParts;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuReactionRepository;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CafeteriaMenuReactionService {

    private final CafeteriaMenuRepository cafeteriaMenuRepository;
    private final CafeteriaMenuReactionRepository cafeteriaMenuReactionRepository;

    @Transactional
    public CafeteriaMenuReactionResponse upsertReaction(
            String memberId,
            String menuId,
            UpsertCafeteriaMenuReactionRequest request
    ) {
        ResolvedCafeteriaMenuTarget target = resolveTarget(menuId);
        CafeteriaMenuReaction existing = cafeteriaMenuReactionRepository
                .findById_MemberIdAndId_MenuId(memberId, target.menuId())
                .orElse(null);

        if (request.reaction() == null) {
            if (existing != null) {
                cafeteriaMenuReactionRepository.delete(existing);
            }
            return summarize(target.menuId(), target.weekId(), null);
        }

        if (existing == null) {
            cafeteriaMenuReactionRepository.save(CafeteriaMenuReaction.create(
                    memberId,
                    target.menuId(),
                    target.weekId(),
                    target.category(),
                    target.title(),
                    request.reaction()
            ));
        } else if (existing.getReaction() != request.reaction()) {
            existing.updateReaction(request.reaction());
        }

        return summarize(target.menuId(), target.weekId(), request.reaction());
    }

    private CafeteriaMenuReactionResponse summarize(
            String menuId,
            String weekId,
            CafeteriaMenuReactionType myReaction
    ) {
        CafeteriaMenuReactionRepository.CafeteriaMenuReactionCountProjection count = cafeteriaMenuReactionRepository
                .summarizeCounts(weekId, Set.of(menuId))
                .stream()
                .findFirst()
                .orElse(null);
        return new CafeteriaMenuReactionResponse(
                menuId,
                myReaction,
                count == null ? 0 : Math.toIntExact(count.getLikeCount()),
                count == null ? 0 : Math.toIntExact(count.getDislikeCount())
        );
    }

    private ResolvedCafeteriaMenuTarget resolveTarget(String menuId) {
        CafeteriaMenuIdParts parts;
        try {
            parts = CafeteriaMenuIdCodec.parse(menuId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "menuId 형식이 올바르지 않습니다.");
        }

        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findByWeekIdForUpdate(parts.weekId())
                .orElseThrow(CafeteriaMenuNotFoundException::new);

        for (String title : collectTitles(cafeteriaMenu, parts.category())) {
            if (CafeteriaMenuIdCodec.matches(menuId, parts.weekId(), parts.category(), title)) {
                return new ResolvedCafeteriaMenuTarget(menuId, parts.weekId(), parts.category(), title);
            }
        }
        throw new CafeteriaMenuEntryNotFoundException();
    }

    private List<String> collectTitles(CafeteriaMenu cafeteriaMenu, String category) {
        Set<String> titles = new LinkedHashSet<>();
        if (!cafeteriaMenu.getMenuEntries().isEmpty()) {
            cafeteriaMenu.getMenuEntries().values().forEach(categories ->
                    categories.getOrDefault(category, List.of()).forEach(item -> titles.add(item.title()))
            );
        } else {
            cafeteriaMenu.getMenus().values().forEach(categories ->
                    titles.addAll(categories.getOrDefault(category, List.of()))
            );
        }
        return List.copyOf(titles);
    }

    private record ResolvedCafeteriaMenuTarget(
            String menuId,
            String weekId,
            String category,
            String title
    ) {
    }
}
