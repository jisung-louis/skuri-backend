package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMemberId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

    @EntityGraph(attributePaths = {"chatRoom"})
    List<ChatRoomMember> findById_MemberId(String memberId);

    @EntityGraph(attributePaths = {"chatRoom"})
    Optional<ChatRoomMember> findById_ChatRoomIdAndId_MemberId(String chatRoomId, String memberId);

    List<ChatRoomMember> findById_ChatRoomId(String chatRoomId);

    boolean existsById_ChatRoomIdAndId_MemberId(String chatRoomId, String memberId);

    void deleteById_ChatRoomIdAndId_MemberId(String chatRoomId, String memberId);

    long deleteById_ChatRoomId(String chatRoomId);
}
