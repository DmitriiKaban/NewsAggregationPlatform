package md.botservice.service;

import md.botservice.dto.SourceAdoptionProjection;
import md.botservice.dto.SourceRecommendationProjection;
import md.botservice.dto.UserProfileResponse;
import md.botservice.events.UserInterestEvent;
import md.botservice.exceptions.UserNotFoundException;
import md.botservice.models.Source;
import md.botservice.models.User;
import md.botservice.models.UserRole;
import md.botservice.producers.SourceUpdatePublisher;
import md.botservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SourceUpdatePublisher sourceUpdatePublisher;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private org.telegram.telegrambots.meta.api.objects.User telegramUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(12345L);
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setSubscriptions(new HashSet<>());
        testUser.setReadAllPostsSources(new HashSet<>());
        testUser.setRole(UserRole.USER);

        telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        lenient().when(telegramUser.getId()).thenReturn(12345L);
        lenient().when(telegramUser.getUserName()).thenReturn("testuser");
        lenient().when(telegramUser.getFirstName()).thenReturn("Test");
    }

    @Test
    void findOrRegister_ExistingUser_UpdatesLastActiveAndReturns() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.findOrRegister(telegramUser);

        assertNotNull(result);
        assertNotNull(result.getLastActiveAt());
        verify(userRepository).save(testUser);
    }

    @Test
    void findOrRegister_NewUser_RegistersAndReturns() {
        when(userRepository.findById(12345L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrRegister(telegramUser);

        assertNotNull(result);
        assertEquals(12345L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("Test", result.getFirstName());
        assertNotNull(result.getRegisteredAt());
        assertNotNull(result.getLastActiveAt());

        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void getUserProfile_ReturnsCorrectProfile() {
        Source source = new Source();
        source.setId(1L);
        source.setName("Tech News");
        source.setUrl("http://tech.news");

        testUser.getSubscriptions().add(source);
        testUser.getReadAllPostsSources().add(source);
        testUser.setPreferredLanguage("en");
        testUser.setInterestsRaw("Java, Spring");

        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));

        UserProfileResponse profile = userService.getUserProfile(12345L);

        assertNotNull(profile);
        assertEquals("Test", profile.firstName());
        assertEquals("User", profile.lastName());
        assertEquals("Java, Spring", profile.interests());
        assertEquals("en", profile.language());
        assertEquals("USER", profile.role());
        assertEquals(1, profile.sources().size());
        assertTrue(profile.sources().getFirst().isReadAll());
    }

    @Test
    void updateInterests_Success_SendsKafkaMessage() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));

        userService.updateInterests(12345L, "AI, Machine Learning");

        assertEquals("AI, Machine Learning", testUser.getInterestsRaw());
        verify(userRepository).save(testUser);

        ArgumentCaptor<UserInterestEvent> eventCaptor = ArgumentCaptor.forClass(UserInterestEvent.class);
        verify(kafkaTemplate).send(eq("user.interests.updated"), eventCaptor.capture());

        UserInterestEvent capturedEvent = eventCaptor.getValue();
        assertEquals(12345L, capturedEvent.getUserId());
        assertEquals("AI, Machine Learning", capturedEvent.getInterests());
    }

    @Test
    void updateInterests_KafkaException_LogsErrorAndDoesNotCrash() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(kafkaTemplate.send(anyString(), any())).thenThrow(new RuntimeException("Kafka down"));

        assertDoesNotThrow(() -> userService.updateInterests(12345L, "AI"));
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_ReturnsSavedUser() {
        when(userRepository.save(testUser)).thenReturn(testUser);
        User result = userService.updateUser(testUser);
        assertEquals(testUser, result);
        verify(userRepository).save(testUser);
    }

    @Test
    void findById_UserExists_ReturnsUser() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        User result = userService.findById(12345L);
        assertEquals(testUser, result);
    }

    @Test
    void findById_UserDoesNotExist_ThrowsException() {
        when(userRepository.findById(12345L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findById(12345L));
    }

    @Test
    void updateUserFiltering_UpdatesAndPublishes() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.updateUserFiltering(12345L, true);

        assertTrue(result.isShowOnlySubscribedSources());
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void updateDailySummary_UpdatesAndSaves() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.updateDailySummary(12345L, true);

        assertTrue(result.isDailySummaryEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateWeeklySummary_UpdatesAndSaves() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.updateWeeklySummary(12345L, true);

        assertTrue(result.isWeeklySummaryEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateLanguage_ValidLanguage_UpdatesAndSaves() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.updateLanguage(12345L, "ru");

        assertEquals("ru", result.getPreferredLanguage());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateLanguage_InvalidLanguage_WarnsAndSavesWithoutChange() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        testUser.setPreferredLanguage("en");

        User result = userService.updateLanguage(12345L, "invalid_code");

        assertEquals("en", result.getPreferredLanguage());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateReadAllNewsSource_AddSource_SavesAndPublishes() {
        Source source = new Source();
        source.setId(1L);
        testUser.getSubscriptions().add(source);

        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        userService.updateReadAllNewsSource(12345L, 1L, true);

        assertTrue(testUser.getReadAllPostsSources().contains(source));
        verify(userRepository).save(testUser);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void updateReadAllNewsSource_RemoveSource_SavesAndPublishes() {
        Source source = new Source();
        source.setId(1L);
        testUser.getSubscriptions().add(source);
        testUser.getReadAllPostsSources().add(source);

        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        userService.updateReadAllNewsSource(12345L, 1L, false);

        assertFalse(testUser.getReadAllPostsSources().contains(source));
        verify(userRepository).save(testUser);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void updateReadAllNewsSource_SourceNotSubscribed_ThrowsException() {
        when(userRepository.findById(12345L)).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateReadAllNewsSource(12345L, 99L, true));

        assertEquals("Source not found in user subscriptions", exception.getMessage());
    }

    @Test
    void getRecommendationsForUser_DelegatesToRepository() {
        List<SourceRecommendationProjection> mockList = Collections.emptyList();
        when(userRepository.getRecommendationsForUser(12345L)).thenReturn(mockList);

        List<SourceRecommendationProjection> result = userService.getRecommendationsForUser(12345L);

        assertEquals(mockList, result);
        verify(userRepository).getRecommendationsForUser(12345L);
    }

    @Test
    void getStrictModeAdoptionPercentage_DelegatesToRepository() {
        when(userRepository.getStrictModeAdoptionPercentage()).thenReturn(45.5);

        Double result = userService.getStrictModeAdoptionPercentage();

        assertEquals(45.5, result);
        verify(userRepository).getStrictModeAdoptionPercentage();
    }

    @Test
    void getTopReadAllSources_DelegatesToRepository() {
        List<SourceAdoptionProjection> mockList = Collections.emptyList();
        when(userRepository.getTopReadAllSources()).thenReturn(mockList);

        List<SourceAdoptionProjection> result = userService.getTopReadAllSources();

        assertEquals(mockList, result);
        verify(userRepository).getTopReadAllSources();
    }

}