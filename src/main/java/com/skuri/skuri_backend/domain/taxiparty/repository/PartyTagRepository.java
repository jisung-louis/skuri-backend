package com.skuri.skuri_backend.domain.taxiparty.repository;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PartyTagRepository extends JpaRepository<PartyTag, Long> {

    @Query("""
            select t.party.id as partyId, t.tag as tag
            from PartyTag t
            where t.party.id in :partyIds
            order by t.party.id asc, t.id asc
            """)
    List<PartyTagSummary> findTagSummariesByPartyIds(@Param("partyIds") Collection<String> partyIds);

    interface PartyTagSummary {
        String getPartyId();

        String getTag();
    }
}
