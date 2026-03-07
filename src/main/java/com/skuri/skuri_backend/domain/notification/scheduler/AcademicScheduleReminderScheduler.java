package com.skuri.skuri_backend.domain.notification.scheduler;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AcademicScheduleReminderScheduler {

    private final AcademicScheduleRepository academicScheduleRepository;
    private final AfterCommitApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void publishReminders() {
        LocalDate today = LocalDate.now();

        academicScheduleRepository.findByStartDateOrderByCreatedAtAsc(today)
                .forEach(schedule -> eventPublisher.publish(
                        new NotificationDomainEvent.AcademicScheduleReminder(
                                schedule.getId(),
                                NotificationDomainEvent.ReminderTiming.DAY_OF
                        )
                ));

        academicScheduleRepository.findByStartDateOrderByCreatedAtAsc(today.plusDays(1))
                .forEach(schedule -> eventPublisher.publish(
                        new NotificationDomainEvent.AcademicScheduleReminder(
                                schedule.getId(),
                                NotificationDomainEvent.ReminderTiming.DAY_BEFORE
                        )
                ));
    }
}
