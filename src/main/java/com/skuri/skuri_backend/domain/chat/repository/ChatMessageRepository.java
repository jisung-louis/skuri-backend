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
                    or (
                        m.createdAt = :cursorCreatedAt
                        and (
                            (
                                :cursorMessageOrder is not null
                                and (
                                    m.messageOrder is null
                                    or m.messageOrder < :cursorMessageOrder
                                    or (m.messageOrder = :cursorMessageOrder and m.id < :cursorId)
                                )
                            )
                            or (:cursorMessageOrder is null and m.messageOrder is null and m.id < :cursorId)
                        )
                    )
                  )
            order by
                m.createdAt desc,
                case when m.messageOrder is null then 0 else 1 end desc,
                m.messageOrder desc,
                m.id desc
            """)
    List<ChatMessage> findByCursor(
            @Param("chatRoomId") String chatRoomId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") String cursorId,
            @Param("cursorMessageOrder") Long cursorMessageOrder,
            Pageable pageable
    );

    long countByChatRoomId(String chatRoomId);

    long countByChatRoomIdAndCreatedAtAfter(String chatRoomId, LocalDateTime createdAt);

    long deleteByChatRoomId(String chatRoomId);
}
