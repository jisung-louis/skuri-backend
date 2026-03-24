package com.skuri.skuri_backend.domain.chat.repository;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    List<ChatRoom> findByType(ChatRoomType type);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert ignore into chat_rooms (
                id,
                name,
                type,
                department,
                description,
                created_by,
                is_public,
                max_members,
                member_count,
                message_count,
                created_at,
                updated_at
            ) values (
                :id,
                :name,
                :type,
                :department,
                :description,
                null,
                true,
                null,
                0,
                0,
                now(),
                now()
            )
            """, nativeQuery = true)
    int insertPublicSeedRoomIfAbsent(
            @Param("id") String id,
            @Param("name") String name,
            @Param("type") String type,
            @Param("department") String department,
            @Param("description") String description
    );
}
