package md.botservice.service;

import md.botservice.dto.TopSourceProjection;
import md.botservice.exceptions.SourceNotFoundException;
import md.botservice.models.Source;
import md.botservice.models.SourceType;
import md.botservice.models.User;
import md.botservice.repository.SourceRepository;
import md.botservice.utils.FormatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private UserService userService;

    @Mock
    private SourceUpdatePublisher sourceUpdatePublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SourceService sourceService;

    private User testUser;
    private Source testSource;
    private final String RAW_URL = "durov";
    private final String CLEAN_URL = "https://t.me/durov";

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setSubscriptions(new HashSet<>());

        testSource = new Source();
        testSource.setId(10L);
        testSource.setUrl(CLEAN_URL);
        testSource.setName("durov");
        testSource.setType(SourceType.TELEGRAM);
    }

    @Test
    @DisplayName("Should successfully subscribe user to an existing valid source")
    void subscribeUser_Success_ExistingSource() {
        try (MockedStatic<FormatUtils> formatUtils = mockStatic(FormatUtils.class)) {
            // Arrange
            formatUtils.when(() -> FormatUtils.normalizeTelegramUrl(RAW_URL)).thenReturn(CLEAN_URL);
            when(restTemplate.headForHeaders(CLEAN_URL)).thenReturn(new HttpHeaders());
            when(sourceRepository.findByUrl(CLEAN_URL)).thenReturn(Optional.of(testSource));
            when(userService.updateUser(any(User.class))).thenReturn(testUser);

            // Act
            sourceService.subscribeUser(testUser, RAW_URL);

            // Assert
            assertTrue(testUser.getSubscriptions().contains(testSource));
            verify(sourceRepository, never()).save(any(Source.class));
            verify(userService).updateUser(testUser);
            verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
        }
    }

    @Test
    @DisplayName("Should create new source and subscribe user when source is valid but not in DB")
    void subscribeUser_Success_NewSource() {
        try (MockedStatic<FormatUtils> formatUtils = mockStatic(FormatUtils.class)) {
            // Arrange
            formatUtils.when(() -> FormatUtils.normalizeTelegramUrl(RAW_URL)).thenReturn(CLEAN_URL);
            when(restTemplate.headForHeaders(CLEAN_URL)).thenReturn(new HttpHeaders());
            when(sourceRepository.findByUrl(CLEAN_URL)).thenReturn(Optional.empty());
            when(sourceRepository.save(any(Source.class))).thenReturn(testSource);
            when(userService.updateUser(any(User.class))).thenReturn(testUser);

            // Act
            sourceService.subscribeUser(testUser, RAW_URL);

            // Assert
            assertTrue(testUser.getSubscriptions().contains(testSource));
            verify(sourceRepository).save(any(Source.class));
            verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
        }
    }

    @Test
    @DisplayName("Should throw exception when Telegram channel does not exist")
    void subscribeUser_ThrowsException_WhenChannelNotFound() {
        try (MockedStatic<FormatUtils> formatUtils = mockStatic(FormatUtils.class)) {
            // Arrange
            formatUtils.when(() -> FormatUtils.normalizeTelegramUrl(RAW_URL)).thenReturn(CLEAN_URL);
            when(restTemplate.headForHeaders(CLEAN_URL))
                    .thenThrow(HttpClientErrorException.NotFound.class);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    sourceService.subscribeUser(testUser, RAW_URL)
            );

            assertTrue(exception.getMessage().contains("Telegram channel not found"));
            verify(sourceRepository, never()).findByUrl(anyString());
            verify(userService, never()).updateUser(any());
        }
    }

    @Test
    @DisplayName("Should successfully unsubscribe user by URL")
    void unsubscribeUser_ByUrl_Success() {
        try (MockedStatic<FormatUtils> formatUtils = mockStatic(FormatUtils.class)) {
            // Arrange
            formatUtils.when(() -> FormatUtils.normalizeTelegramUrl(RAW_URL)).thenReturn(CLEAN_URL);
            testUser.getSubscriptions().add(testSource);
            when(userService.updateUser(any(User.class))).thenReturn(testUser);

            // Act
            sourceService.unsubscribeUser(testUser, RAW_URL);

            // Assert
            assertTrue(testUser.getSubscriptions().isEmpty());
            verify(userService).updateUser(testUser);
            verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
        }
    }

    @Test
    @DisplayName("Should successfully unsubscribe user by Source ID")
    void unsubscribeUser_ById_Success() {
        // Arrange
        testUser.getSubscriptions().add(testSource);
        when(sourceRepository.findById(testSource.getId())).thenReturn(Optional.of(testSource));
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        // Act
        sourceService.unsubscribeUser(testUser, testSource.getId());

        // Assert
        assertTrue(testUser.getSubscriptions().isEmpty());
        verify(userService).updateUser(testUser);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    @DisplayName("Should throw exception when unsubscribing by unknown Source ID")
    void unsubscribeUser_ById_ThrowsException_WhenNotFound() {
        // Arrange
        when(sourceRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(SourceNotFoundException.class, () ->
                sourceService.unsubscribeUser(testUser, 999L)
        );

        assertEquals("Source not found", exception.getMessage());
        verify(userService, never()).updateUser(any());
    }

    @Test
    @DisplayName("Should successfully toggle Strict Mode")
    void setShowOnlySubscribedSources_Success() {
        // Arrange
        when(userService.updateUserFiltering(testUser.getId(), true)).thenReturn(testUser);

        // Act
        sourceService.setShowOnlySubscribedSources(testUser, true);

        // Assert
        assertTrue(testUser.isShowOnlySubscribedSources());
        verify(userService).updateUserFiltering(testUser.getId(), true);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    @DisplayName("Should return top sources from repository")
    void getTopSources_ReturnsList() {
        // Arrange
        TopSourceProjection mockProjection = mock(TopSourceProjection.class);
        when(sourceRepository.getTopSources()).thenReturn(List.of(mockProjection));

        // Act
        List<TopSourceProjection> result = sourceService.getTopSources();

        // Assert
        assertEquals(1, result.size());
        verify(sourceRepository).getTopSources();
    }
}