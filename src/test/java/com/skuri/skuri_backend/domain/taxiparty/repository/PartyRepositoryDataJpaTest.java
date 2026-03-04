package com.skuri.skuri_backend.domain.taxiparty.repository;

import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PartyRepositoryDataJpaTest {

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findDetailById_멤버정산태그가있어도_예외없이조회된다() {
        Party party = createArrivedParty("leader-1", "member-1");
        entityManager.persist(party);
        entityManager.flush();
        entityManager.clear();

        Party found = assertDoesNotThrow(() ->
                partyRepository.findDetailById(party.getId()).orElseThrow()
        );

        assertEquals(2, found.getMembers().size());
        assertEquals(1, found.getSettlementItems().size());
        assertEquals(List.of("빠른출발"), found.getTagsText());
    }

    @Test
    void findMyParties_멤버정산태그가있어도_예외없이조회된다() {
        Party party = createArrivedParty("leader-2", "member-1");
        entityManager.persist(party);
        entityManager.flush();
        entityManager.clear();

        List<Party> parties = assertDoesNotThrow(() -> partyRepository.findMyParties("member-1"));

        assertEquals(1, parties.size());
        Party found = parties.get(0);
        assertEquals(2, found.getMembers().size());
        assertEquals(1, found.getSettlementItems().size());
        assertEquals(List.of("빠른출발"), found.getTagsText());
    }

    private Party createArrivedParty(String leaderId, String memberId) {
        Party party = Party.create(
                leaderId,
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(1),
                4,
                List.of("빠른출발"),
                "택시비 나눠요"
        );
        party.addMember(memberId);
        party.arrive(14000);
        return party;
    }
}
