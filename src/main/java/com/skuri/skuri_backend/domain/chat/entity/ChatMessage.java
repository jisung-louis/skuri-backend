package com.skuri.skuri_backend.domain.chat.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.chat.entity.converter.ChatAccountDataJsonConverter;
import com.skuri.skuri_backend.domain.chat.entity.converter.ChatArrivalDataJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_cursor", columnList = "chat_room_id, created_at, message_order, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "chat_room_id", nullable = false, length = 100)
    private String chatRoomId;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Column(name = "sender_name", length = 50)
    private String senderName;

    @Column(name = "message_order", updatable = false)
    private Long messageOrder;

    @Lob
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageType type;

    @Convert(converter = ChatAccountDataJsonConverter.class)
    @Column(name = "account_data", columnDefinition = "json")
    private ChatAccountData accountData;

    @Convert(converter = ChatArrivalDataJsonConverter.class)
    @Column(name = "arrival_data", columnDefinition = "json")
    private ChatArrivalData arrivalData;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChatMessageDirection direction;

    @Column(length = 20)
    private String source;

    @Column(name = "minecraft_uuid", length = 50)
    private String minecraftUuid;

    private ChatMessage(
            String chatRoomId,
            String senderId,
            String senderName,
            Long messageOrder,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData
    ) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.messageOrder = messageOrder;
        this.text = text;
        this.type = type;
        this.accountData = accountData;
        this.arrivalData = arrivalData;
    }

    public static ChatMessage create(
            String chatRoomId,
            String senderId,
            String senderName,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData
    ) {
        return create(
                chatRoomId,
                senderId,
                senderName,
                null,
                text,
                type,
                accountData,
                arrivalData
        );
    }

    public static ChatMessage create(
            String chatRoomId,
            String senderId,
            String senderName,
            Long messageOrder,
            String text,
            ChatMessageType type,
            ChatAccountData accountData,
            ChatArrivalData arrivalData
    ) {
        return new ChatMessage(chatRoomId, senderId, senderName, messageOrder, text, type, accountData, arrivalData);
    }

    public void updateArrivalData(ChatArrivalData arrivalData) {
        this.arrivalData = arrivalData;
    }
}
