package com.skuri.skuri_backend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_room_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @EmbeddedId
    private ChatRoomMemberId id;

    @MapsId("chatRoomId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(nullable = false)
    private boolean muted;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    private ChatRoomMember(ChatRoom chatRoom, String memberId, LocalDateTime joinedAt) {
        this.id = ChatRoomMemberId.of(null, memberId);
        this.chatRoom = chatRoom;
        this.joinedAt = joinedAt;
        this.muted = false;
        this.lastReadAt = null;
    }

    public static ChatRoomMember create(ChatRoom chatRoom, String memberId, LocalDateTime joinedAt) {
        return new ChatRoomMember(chatRoom, memberId, joinedAt);
    }

    public String getChatRoomId() {
        return id.getChatRoomId();
    }

    public String getMemberId() {
        return id.getMemberId();
    }

    public boolean advanceLastReadAt(LocalDateTime requestedLastReadAt) {
        if (requestedLastReadAt == null) {
            return false;
        }
        if (this.lastReadAt == null || requestedLastReadAt.isAfter(this.lastReadAt)) {
            this.lastReadAt = requestedLastReadAt;
            return true;
        }
        return false;
    }

    public void updateMuted(boolean muted) {
        this.muted = muted;
    }
}
