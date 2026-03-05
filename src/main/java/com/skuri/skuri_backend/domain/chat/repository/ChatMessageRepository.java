package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("""
            select m
            from ChatMessage m
            where m.chatRoomId = :chatRoomId
              and (
                    :cursorCreatedAt is null
                    or m.createdAt < :cursorCreatedAt
                    or (m.createdAt = :cursorCreatedAt and m.id < :cursorId)
                  )
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findByCursor(
            @Param("chatRoomId") String chatRoomId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") String cursorId,
            Pageable pageable
    );

    long countByChatRoomId(String chatRoomId);

    long countByChatRoomIdAndCreatedAtAfter(String chatRoomId, LocalDateTime createdAt);

    long deleteByChatRoomId(String chatRoomId);
}
