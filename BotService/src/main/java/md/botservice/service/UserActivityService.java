package md.botservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import md.botservice.dto.DauProjection;
import md.botservice.models.UserActivity;
import md.botservice.repository.UserActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository activityRepository;

    @Transactional
    public void recordActivity(Long userId) {
        LocalDate today = LocalDate.now();

        activityRepository.findByUserIdAndActivityDate(userId, today)
                .ifPresentOrElse(
                        existing -> {
                            activityRepository.incrementActivityCount(userId, today);
                            log.debug("Incremented activity for user {}", userId);
                        },
                        () -> {
                            UserActivity activity = new UserActivity(userId, today);
                            activityRepository.save(activity);
                            log.info("First activity for user {} today", userId);
                        }
                );
    }

    public List<DauProjection> getDailyActiveUsers() {
        return activityRepository.getDailyActiveUsers();
    }
}
