package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ChatMessageRepositoryDataJpaTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByCursor_같은createdAt이면_messageOrder로저장순서를안정적으로정렬한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 25, 18, 0, 0);
        String joinMessageId = insertChatMessage("party:party-1", "김철수님이 파티에 합류했어요.", createdAt);
        String closedMessageId = insertChatMessage("party:party-1", "모집이 마감되었어요.", createdAt);
        entityManager.flush();
        entityManager.clear();

        List<ChatMessage> messages = chatMessageRepository.findByCursor(
                "party:party-1",
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertEquals(List.of(closedMessageId, joinMessageId), messages.stream().map(ChatMessage::getId).toList());
        assertNotNull(messages.get(0).getMessageOrder());
        assertNotNull(messages.get(1).getMessageOrder());
    }

    @Test
    void findByCursor_같은createdAt커서페이지네이션도_messageOrder를사용한다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 25, 18, 0, 0);
        insertChatMessage("party:party-1", "김철수님이 파티에 합류했어요.", createdAt);
        insertChatMessage("party:party-1", "모집이 마감되었어요.", createdAt);
        entityManager.flush();
        entityManager.clear();

        List<ChatMessage> firstPage = chatMessageRepository.findByCursor(
                "party:party-1",
                null,
                null,
                null,
                PageRequest.of(0, 1)
        );

        ChatMessage cursor = firstPage.get(0);
        List<ChatMessage> secondPage = chatMessageRepository.findByCursor(
                "party:party-1",
                cursor.getCreatedAt(),
                cursor.getId(),
                cursor.getMessageOrder(),
                PageRequest.of(0, 1)
        );

        assertEquals(1, firstPage.size());
        assertEquals(1, secondPage.size());
        assertEquals("모집이 마감되었어요.", firstPage.get(0).getText());
        assertEquals("김철수님이 파티에 합류했어요.", secondPage.get(0).getText());
    }

    private String insertChatMessage(String chatRoomId, String text, LocalDateTime createdAt) {
        String id = UUID.randomUUID().toString();
        entityManager.createNativeQuery("""
                insert into chat_messages (
                    id, chat_room_id, sender_id, sender_name, text, type, created_at, updated_at
                ) values (
                    :id, :chatRoomId, :senderId, :senderName, :text, :type, :createdAt, :updatedAt
                )
                """)
                .setParameter("id", id)
                .setParameter("chatRoomId", chatRoomId)
                .setParameter("senderId", "leader-1")
                .setParameter("senderName", "파티 리더")
                .setParameter("text", text)
                .setParameter("type", "SYSTEM")
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .executeUpdate();
        return id;
    }
}
