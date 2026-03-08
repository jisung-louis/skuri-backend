package com.skuri.skuri_backend.common.event;

import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.notification.service.DomainEventNotificationListener;
import com.skuri.skuri_backend.domain.notification.service.NotificationEventHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.mockito.Mockito.verify;

@SpringJUnitConfig(AfterCommitApplicationEventPublisherTest.TestConfig.class)
@ActiveProfiles("test")
class AfterCommitApplicationEventPublisherTest {

    @Autowired
    private TestPublisherService testPublisherService;

    @MockitoBean
    private NotificationEventHandler notificationEventHandler;

    @Test
    void publish_트랜잭션커밋후_리스너가실행된다() {
        NotificationDomainEvent event = new NotificationDomainEvent.PartyCreated("party-1");

        testPublisherService.publish(event);

        verify(notificationEventHandler).handle(event);
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:after-commit-event;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        AfterCommitApplicationEventPublisher afterCommitApplicationEventPublisher(
                ApplicationEventPublisher applicationEventPublisher
        ) {
            return new AfterCommitApplicationEventPublisher(applicationEventPublisher);
        }

        @Bean
        DomainEventNotificationListener domainEventNotificationListener(NotificationEventHandler notificationEventHandler) {
            return new DomainEventNotificationListener(notificationEventHandler);
        }

        @Bean
        TestPublisherService testPublisherService(AfterCommitApplicationEventPublisher eventPublisher) {
            return new TestPublisherService(eventPublisher);
        }
    }

    static class TestPublisherService {

        private final AfterCommitApplicationEventPublisher eventPublisher;

        TestPublisherService(AfterCommitApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        public void publish(NotificationDomainEvent event) {
            eventPublisher.publish(event);
        }
    }
}
