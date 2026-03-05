package com.skuri.skuri_backend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMemberId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "chat_room_id", length = 100)
    private String chatRoomId;

    @Column(name = "member_id", length = 36)
    private String memberId;

    private ChatRoomMemberId(String chatRoomId, String memberId) {
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
    }

    public static ChatRoomMemberId of(String chatRoomId, String memberId) {
        return new ChatRoomMemberId(chatRoomId, memberId);
    }
}
