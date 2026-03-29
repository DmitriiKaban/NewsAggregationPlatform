package md.botservice.service;

import md.botservice.dto.DauProjection;
import md.botservice.models.UserActivity;
import md.botservice.repository.UserActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository activityRepository;

    @InjectMocks
    private UserActivityService userActivityService;

    @Test
    void recordActivity_ExistingActivity_IncrementsCount() {
        UserActivity existingActivity = new UserActivity(1L, LocalDate.now());
        when(activityRepository.findByUserIdAndActivityDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(existingActivity));

        userActivityService.recordActivity(1L);

        verify(activityRepository).incrementActivityCount(eq(1L), any(LocalDate.class));
        verify(activityRepository, never()).save(any(UserActivity.class));
    }

    @Test
    void recordActivity_NewActivity_SavesNewRecord() {
        when(activityRepository.findByUserIdAndActivityDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        userActivityService.recordActivity(1L);

        verify(activityRepository).save(any(UserActivity.class));
        verify(activityRepository, never()).incrementActivityCount(anyLong(), any(LocalDate.class));
    }

    @Test
    void getDailyActiveUsers_DelegatesToRepository() {
        List<DauProjection> mockList = Collections.emptyList();
        when(activityRepository.getDailyActiveUsers()).thenReturn(mockList);

        List<DauProjection> result = userActivityService.getDailyActiveUsers();

        assertEquals(mockList, result);
        verify(activityRepository).getDailyActiveUsers();
    }

}