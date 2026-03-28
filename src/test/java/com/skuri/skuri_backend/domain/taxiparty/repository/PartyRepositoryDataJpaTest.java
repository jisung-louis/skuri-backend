package com.skuri.skuri_backend.domain.taxiparty.repository;

import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PartyRepositoryDataJpaTest {

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyTagRepository partyTagRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findDetailById_멤버정산태그가있어도_예외없이조회된다() {
        String partyId = seedArrivedParty("party-1", "leader-1", "member-1");
        entityManager.flush();
        entityManager.clear();

        Party found = assertDoesNotThrow(() ->
                partyRepository.findDetailById(partyId).orElseThrow()
        );

        assertEquals(2, found.getMembers().size());
        assertEquals(1, found.getSettlementItems().size());
        assertEquals(List.of("빠른출발"), found.getTagsText());
    }

    @Test
    void findMyParties_멤버정산태그가있어도_예외없이조회된다() {
        seedArrivedParty("party-2", "leader-2", "member-1");
        entityManager.flush();
        entityManager.clear();

        List<Party> parties = assertDoesNotThrow(() -> partyRepository.findMyParties("member-1"));

        assertEquals(1, parties.size());
        Party found = parties.get(0);
        assertEquals(2, found.getMembers().size());
        assertEquals(1, found.getSettlementItems().size());
        assertEquals(List.of("빠른출발"), found.getTagsText());
    }

    @Test
    void findDetailsByIds_멤버와태그가있어도_예외없이조회된다() {
        String partyId = seedArrivedParty("party-4", "leader-4", "member-4");
        entityManager.flush();
        entityManager.clear();

        List<Party> parties = assertDoesNotThrow(() -> partyRepository.findDetailsByIds(List.of(partyId)));
        List<String> tags = partyTagRepository.findTagSummariesByPartyIds(List.of(partyId)).stream()
                .map(PartyTagRepository.PartyTagSummary::getTag)
                .toList();

        assertEquals(1, parties.size());
        assertEquals(2, parties.getFirst().getMembers().size());
        assertEquals(List.of("빠른출발"), tags);
    }

    @Test
    void findSseSnapshotParties_멤버와태그가있어도_예외없이조회된다() {
        seedArrivedParty("party-5", "leader-5", "member-5");
        entityManager.flush();
        entityManager.clear();

        List<Party> parties = assertDoesNotThrow(() -> partyRepository.findSseSnapshotParties());

        assertEquals(1, parties.size());
        assertEquals(2, parties.getFirst().getMembers().size());
        assertEquals(
                List.of("빠른출발"),
                partyTagRepository.findTagSummariesByPartyIds(List.of("party-5")).stream()
                        .map(PartyTagRepository.PartyTagSummary::getTag)
                        .toList()
        );
    }

    @Test
    void existsActivePartyByMemberId_정산스냅샷만남은ARRIVED탈퇴멤버는_false() {
        String partyId = seedArrivedParty("party-3", "leader-3", "member-1");
        entityManager.createNativeQuery("""
                delete from party_members
                where party_id = :partyId
                  and member_id = :memberId
                """)
                .setParameter("partyId", partyId)
                .setParameter("memberId", "member-1")
                .executeUpdate();
        entityManager.createNativeQuery("""
                update member_settlements
                set left_party = true,
                    left_at = :leftAt
                where party_id = :partyId
                  and member_id = :memberId
                """)
                .setParameter("partyId", partyId)
                .setParameter("memberId", "member-1")
                .setParameter("leftAt", LocalDateTime.now())
                .executeUpdate();

        boolean exists = partyRepository.existsActivePartyByMemberId(
                "member-1",
                EnumSet.of(
                        com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.OPEN,
                        com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.CLOSED,
                        com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus.ARRIVED
                ),
                null
        );

        assertFalse(exists);
    }

    private String seedArrivedParty(String partyId, String leaderId, String memberId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = now.plusHours(1);
        LocalDateTime joinedAt = now.minusMinutes(10);

        entityManager.createNativeQuery("""
                insert into parties (
                    id, leader_id, departure_name, departure_lat, departure_lng,
                    destination_name, destination_lat, destination_lng,
                    departure_time, max_members, current_members, detail,
                    status, settlement_status, per_person_amount,
                    created_at, updated_at, version
                ) values (
                    :id, :leaderId, :departureName, :departureLat, :departureLng,
                    :destinationName, :destinationLat, :destinationLng,
                    :departureTime, :maxMembers, :currentMembers, :detail,
                    :status, :settlementStatus, :perPersonAmount,
                    :createdAt, :updatedAt, :version
                )
                """)
                .setParameter("id", partyId)
                .setParameter("leaderId", leaderId)
                .setParameter("departureName", "성결대학교")
                .setParameter("departureLat", 37.38)
                .setParameter("departureLng", 126.93)
                .setParameter("destinationName", "안양역")
                .setParameter("destinationLat", 37.40)
                .setParameter("destinationLng", 126.92)
                .setParameter("departureTime", departureTime)
                .setParameter("maxMembers", 4)
                .setParameter("currentMembers", 2)
                .setParameter("detail", "택시비 나눠요")
                .setParameter("status", "ARRIVED")
                .setParameter("settlementStatus", "PENDING")
                .setParameter("perPersonAmount", 14000)
                .setParameter("createdAt", now)
                .setParameter("updatedAt", now)
                .setParameter("version", 0L)
                .executeUpdate();

        entityManager.createNativeQuery("""
                insert into party_members (party_id, member_id, joined_at)
                values (:partyId, :memberId, :joinedAt)
                """)
                .setParameter("partyId", partyId)
                .setParameter("memberId", leaderId)
                .setParameter("joinedAt", joinedAt)
                .executeUpdate();

        entityManager.createNativeQuery("""
                insert into party_members (party_id, member_id, joined_at)
                values (:partyId, :memberId, :joinedAt)
                """)
                .setParameter("partyId", partyId)
                .setParameter("memberId", memberId)
                .setParameter("joinedAt", joinedAt.plusMinutes(1))
                .executeUpdate();

        entityManager.createNativeQuery("""
                insert into member_settlements (
                    party_id, member_id, settled, settled_at, display_name, left_party, left_at
                )
                values (:partyId, :memberId, :settled, :settledAt, :displayName, :leftParty, :leftAt)
                """)
                .setParameter("partyId", partyId)
                .setParameter("memberId", memberId)
                .setParameter("settled", false)
                .setParameter("settledAt", null)
                .setParameter("displayName", "김철수")
                .setParameter("leftParty", false)
                .setParameter("leftAt", null)
                .executeUpdate();

        entityManager.createNativeQuery("""
                insert into party_tags (party_id, tag)
                values (:partyId, :tag)
                """)
                .setParameter("partyId", partyId)
                .setParameter("tag", "빠른출발")
                .executeUpdate();

        return partyId;
    }
}
