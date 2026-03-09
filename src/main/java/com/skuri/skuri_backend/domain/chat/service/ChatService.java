package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatAccountDataResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatArrivalDataResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageCursorResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomLastMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSettingsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryEventResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatArrivalData;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.exception.MemberNotFoundException;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;
    private static final int MAX_MESSAGE_PAGE_SIZE = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final PartyMessageService partyMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Transactional
    public void createPartyChatRoom(Party party) {
        syncPartyChatRoomMembers(party);
    }

    @Transactional
    public void syncPartyChatRoomMembers(Party party) {
        String chatRoomId = "party:" + party.getId();
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.createPartyRoom(party.getId())));
        Map<String, ChatRoomMember> currentMembers = chatRoomMemberRepository.findById_ChatRoomId(chatRoomId).stream()
                .collect(Collectors.toMap(ChatRoomMember::getMemberId, Function.identity()));
        Set<String> expectedMembers = new LinkedHashSet<>(party.getMemberIds());

        for (ChatRoomMember member : currentMembers.values()) {
            if (!expectedMembers.contains(member.getMemberId())) {
                chatRoomMemberRepository.delete(member);
            }
        }

        LocalDateTime joinedAt = LocalDateTime.now();
        for (String memberId : expectedMembers) {
            if (!currentMembers.containsKey(memberId)) {
                chatRoomMemberRepository.save(ChatRoomMember.create(room, memberId, joinedAt));
            }
        }

        room.updateMemberCount(expectedMembers.size());
        chatRoomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> getChatRooms(String memberId, ChatRoomType type, Boolean joinedOnly) {
        List<ChatRoom> rooms = type == null ? chatRoomRepository.findAll() : chatRoomRepository.findByType(type);
        Map<String, ChatRoomMember> membershipMap = chatRoomMemberRepository.findById_MemberId(memberId).stream()
                .collect(Collectors.toMap(ChatRoomMember::getChatRoomId, Function.identity()));

        return rooms.stream()
                .filter(room -> room.isPublic() || membershipMap.containsKey(room.getId()))
                .filter(room -> !Boolean.TRUE.equals(joinedOnly) || membershipMap.containsKey(room.getId()))
                .sorted(chatRoomComparator())
                .map(room -> toSummaryResponse(room, membershipMap.get(room.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomDetailResponse getChatRoomDetail(String memberId, String chatRoomId) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        ChatRoomMember member = chatRoomMemberRepository
                .findById_ChatRoomIdAndId_MemberId(chatRoomId, memberId)
                .orElse(null);
        if (!room.isPublic() && member == null) {
            throw new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER);
        }

        long unreadCount = calculateUnreadCount(room, member);
        return new ChatRoomDetailResponse(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getDescription(),
                room.isPublic(),
                room.getMemberCount(),
                member != null,
                member != null && member.isMuted(),
                member != null ? member.getLastReadAt() : null,
                unreadCount
        );
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(
            String memberId,
            String chatRoomId,
            LocalDateTime cursorCreatedAt,
            String cursorId,
            Integer size
    ) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        requireChatRoomMember(chatRoomId, memberId);
        validateCursor(cursorCreatedAt, cursorId);

        int pageSize = size == null ? DEFAULT_MESSAGE_PAGE_SIZE : size;
        if (pageSize < 1 || pageSize > MAX_MESSAGE_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "size는 1 이상 100 이하여야 합니다.");
        }

        List<ChatMessage> fetched = chatMessageRepository.findByCursor(
                chatRoomId,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = fetched.size() > pageSize;
        List<ChatMessage> page = hasNext ? fetched.subList(0, pageSize) : fetched;
        List<ChatMessageResponse> messages = page.stream()
                .map(this::toMessageResponse)
                .toList();

        ChatMessageCursorResponse nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            ChatMessage last = page.get(page.size() - 1);
            nextCursor = new ChatMessageCursorResponse(last.getCreatedAt(), last.getId());
        }

        return new ChatMessagePageResponse(messages, hasNext, nextCursor);
    }

    @Transactional
    public ChatReadUpdateResponse markAsRead(String memberId, String chatRoomId, LocalDateTime lastReadAt) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        ChatRoomMember member = requireChatRoomMember(chatRoomId, memberId);
        boolean updated = member.advanceLastReadAt(clampLastReadAt(room, lastReadAt));
        if (updated) {
            chatRoomMemberRepository.save(member);
        }
        return new ChatReadUpdateResponse(chatRoomId, member.getLastReadAt(), updated);
    }

    @Transactional
    public ChatRoomSettingsResponse updateSettings(String memberId, String chatRoomId, boolean muted) {
        findRoomOrThrow(chatRoomId);
        ChatRoomMember member = requireChatRoomMember(chatRoomId, memberId);
        member.updateMuted(muted);
        chatRoomMemberRepository.save(member);
        return new ChatRoomSettingsResponse(chatRoomId, member.isMuted());
    }

    @Transactional
    public ChatMessageResponse sendMessage(String chatRoomId, String senderId, SendChatMessageRequest request) {
        ChatRoom room = findRoomOrThrow(chatRoomId);
        requireChatRoomMember(chatRoomId, senderId);
        Member sender = memberRepository.findById(senderId).orElseThrow(MemberNotFoundException::new);

        ChatMessageType type = request.type();
        if (type == ChatMessageType.SYSTEM) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "SYSTEM 메시지는 클라이언트 전송이 허용되지 않습니다.");
        }

        String text;
        ChatAccountData accountData = null;
        ChatArrivalData arrivalData = null;

        if (type == ChatMessageType.TEXT) {
            text = requireText(request.text());
        } else if (type == ChatMessageType.IMAGE) {
            text = requireImageUrl(request.imageUrl());
        } else {
            if (room.getType() != ChatRoomType.PARTY) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "파티 채팅방에서만 특수 메시지를 전송할 수 있습니다.");
            }
            PartySpecialMessagePayload payload = partyMessageService.buildSpecialPayload(
                    chatRoomId,
                    senderId,
                    type
            );
            text = payload.text();
            accountData = payload.accountData();
            arrivalData = payload.arrivalData();
        }

        ChatMessage message = ChatMessage.create(
                chatRoomId,
                senderId,
                sender.getNickname(),
                text,
                type,
                accountData,
                arrivalData
        );
        ChatMessage saved = chatMessageRepository.save(message);

        room.applyNewMessage(saved);
        chatRoomRepository.save(room);

        ChatMessageResponse response = toMessageResponse(saved);
        publishAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, response);
            publishChatRoomSummaryEvent(room);
        });
        eventPublisher.publish(new NotificationDomainEvent.ChatMessageCreated(chatRoomId, saved.getId()));

        return response;
    }

    @Transactional
    public void removeMemberFromAllChatRooms(String memberId) {
        List<ChatRoomMember> memberships = chatRoomMemberRepository.findById_MemberId(memberId);
        for (ChatRoomMember membership : memberships) {
            ChatRoom room = membership.getChatRoom();
            chatRoomMemberRepository.delete(membership);
            room.updateMemberCount(Math.max(0, room.getMemberCount() - 1));
            chatRoomRepository.save(room);
            publishAfterCommit(() -> publishChatRoomSummaryEvent(room));
        }
    }

    private void publishChatRoomSummaryEvent(ChatRoom room) {
        List<ChatRoomMember> members = chatRoomMemberRepository.findById_ChatRoomId(room.getId());
        for (ChatRoomMember member : members) {
            long unreadCount = calculateUnreadCount(room, member);
            ChatRoomSummaryEventResponse payload = new ChatRoomSummaryEventResponse(
                    "CHAT_ROOM_UPSERT",
                    room.getId(),
                    room.getName(),
                    room.getMemberCount(),
                    unreadCount,
                    toLastMessage(room),
                    LocalDateTime.now()
            );
            messagingTemplate.convertAndSendToUser(member.getMemberId(), "/queue/chat-rooms", payload);
        }
    }

    private void publishAfterCommit(Runnable publisher) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
            return;
        }
        publisher.run();
    }

    private ChatRoomMember requireChatRoomMember(String chatRoomId, String memberId) {
        return chatRoomMemberRepository
                .findById_ChatRoomIdAndId_MemberId(chatRoomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));
    }

    private ChatRoom findRoomOrThrow(String chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private long calculateUnreadCount(ChatRoom room, ChatRoomMember member) {
        if (member == null) {
            return 0L;
        }
        LocalDateTime lastReadAt = member.getLastReadAt();
        if (lastReadAt == null) {
            return room.getMessageCount();
        }
        return chatMessageRepository.countByChatRoomIdAndCreatedAtAfter(room.getId(), lastReadAt);
    }

    private LocalDateTime clampLastReadAt(ChatRoom room, LocalDateTime requestedLastReadAt) {
        if (requestedLastReadAt == null) {
            return null;
        }

        LocalDateTime upperBound = LocalDateTime.now();
        if (room.getLastMessageTimestamp() != null && room.getLastMessageTimestamp().isBefore(upperBound)) {
            upperBound = room.getLastMessageTimestamp();
        }
        return requestedLastReadAt.isAfter(upperBound) ? upperBound : requestedLastReadAt;
    }

    private Comparator<ChatRoom> chatRoomComparator() {
        Comparator<LocalDateTime> descNullsLast = Comparator.nullsLast(Comparator.reverseOrder());
        return Comparator
                .comparing(ChatRoom::getLastMessageTimestamp, descNullsLast)
                .thenComparing(ChatRoom::getCreatedAt, descNullsLast)
                .thenComparing(ChatRoom::getId, Comparator.reverseOrder());
    }

    private ChatRoomSummaryResponse toSummaryResponse(ChatRoom room, ChatRoomMember member) {
        long unreadCount = calculateUnreadCount(room, member);
        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getName(),
                room.getType(),
                room.getMemberCount(),
                toLastMessage(room),
                unreadCount,
                member != null
        );
    }

    private ChatRoomLastMessageResponse toLastMessage(ChatRoom room) {
        if (room.getLastMessageTimestamp() == null) {
            return null;
        }
        return new ChatRoomLastMessageResponse(
                room.getLastMessageType() != null ? room.getLastMessageType().name() : ChatMessageType.SYSTEM.name(),
                room.getLastMessageText(),
                room.getLastMessageSenderName(),
                room.getLastMessageTimestamp()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        ChatAccountDataResponse accountDataResponse = null;
        if (message.getAccountData() != null) {
            accountDataResponse = new ChatAccountDataResponse(
                    message.getAccountData().getBankName(),
                    message.getAccountData().getAccountNumber(),
                    message.getAccountData().getAccountHolder()
            );
        }

        ChatArrivalDataResponse arrivalDataResponse = null;
        if (message.getArrivalData() != null) {
            arrivalDataResponse = new ChatArrivalDataResponse(
                    message.getArrivalData().getTaxiFare(),
                    message.getArrivalData().getPerPerson(),
                    message.getArrivalData().getMemberCount()
            );
        }

        String imageUrl = message.getType() == ChatMessageType.IMAGE ? message.getText() : null;
        String text = message.getType() == ChatMessageType.IMAGE ? null : message.getText();

        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getSenderName(),
                message.getType(),
                text,
                imageUrl,
                accountDataResponse,
                arrivalDataResponse,
                message.getCreatedAt()
        );
    }

    private String requireText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "TEXT 메시지에는 text가 필요합니다.");
        }
        return text;
    }

    private String requireImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "IMAGE 메시지에는 imageUrl이 필요합니다.");
        }
        return imageUrl;
    }

    private void validateCursor(LocalDateTime cursorCreatedAt, String cursorId) {
        boolean createdAtProvided = cursorCreatedAt != null;
        boolean idProvided = StringUtils.hasText(cursorId);
        if (createdAtProvided != idProvided) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "cursorCreatedAt와 cursorId는 함께 전달해야 합니다.");
        }
    }
}
