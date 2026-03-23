package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.constant.DepartmentCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicChatRoomSeedMigrationTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private PublicChatRoomSeedMigration seedMigration;

    @Test
    void seed_공개방이없으면_공식방과학과방을생성한다() {
        when(chatRoomRepository.existsById(anyString())).thenReturn(false);

        seedMigration.seed();

        verify(chatRoomRepository, times(DepartmentCatalog.DEPARTMENTS.size() + 2)).save(any(ChatRoom.class));
    }

    @Test
    void seed_이미존재하면_중복생성하지않는다() {
        when(chatRoomRepository.existsById(anyString())).thenReturn(true);

        seedMigration.seed();

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }
}
