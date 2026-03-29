package md.botservice.service;

import md.botservice.dto.TopSourceProjection;
import md.botservice.exceptions.SourceNotFoundException;
import md.botservice.exceptions.TelegramChannelNotFoundException;
import md.botservice.models.Source;
import md.botservice.models.User;
import md.botservice.producers.SourceUpdatePublisher;
import md.botservice.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
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
    private final String VALID_URL = "https://t.me/valid_channel";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setSubscriptions(new HashSet<>());

        testSource = new Source();
        testSource.setId(100L);
        testSource.setUrl(VALID_URL);
        testSource.setName("valid_channel");
    }

    @Test
    void subscribeUser_Success_NewSource() {
        when(restTemplate.headForHeaders(anyString())).thenReturn(new HttpHeaders());
        when(sourceRepository.findByUrl(anyString())).thenReturn(Optional.empty());
        when(sourceRepository.save(any(Source.class))).thenReturn(testSource);
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        sourceService.subscribeUser(testUser, VALID_URL);

        assertTrue(testUser.getSubscriptions().contains(testSource));
        verify(sourceRepository).save(any(Source.class));
        verify(userService).updateUser(testUser);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void subscribeUser_Success_ExistingSource() {
        when(restTemplate.headForHeaders(anyString())).thenReturn(new HttpHeaders());
        when(sourceRepository.findByUrl(anyString())).thenReturn(Optional.of(testSource));
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        sourceService.subscribeUser(testUser, VALID_URL);

        assertTrue(testUser.getSubscriptions().contains(testSource));
        verify(sourceRepository, never()).save(any(Source.class));
        verify(userService).updateUser(testUser);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void subscribeUser_ChannelNotFound_ThrowsException() {
        when(restTemplate.headForHeaders(anyString())).thenThrow(HttpClientErrorException.NotFound.class);

        TelegramChannelNotFoundException exception = assertThrows(TelegramChannelNotFoundException.class,
                () -> sourceService.subscribeUser(testUser, VALID_URL));

        assertTrue(exception.getMessage().contains("Telegram channel not found"));
        verify(userService, never()).updateUser(any());
    }

    @Test
    void subscribeUser_VerificationThrowsOtherException_AssumesExists() {
        when(restTemplate.headForHeaders(anyString())).thenThrow(new RuntimeException("Timeout"));
        when(sourceRepository.findByUrl(anyString())).thenReturn(Optional.of(testSource));
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> sourceService.subscribeUser(testUser, VALID_URL));
        assertTrue(testUser.getSubscriptions().contains(testSource));
    }

    @Test
    void unsubscribeUser_ByUrl_Success() {
        testUser.getSubscriptions().add(testSource);
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        sourceService.unsubscribeUser(testUser, VALID_URL);

        assertTrue(testUser.getSubscriptions().isEmpty());
        verify(userService).updateUser(testUser);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void unsubscribeUser_ById_Success() {
        testUser.getSubscriptions().add(testSource);
        when(sourceRepository.findById(100L)).thenReturn(Optional.of(testSource));
        when(userService.updateUser(any(User.class))).thenReturn(testUser);

        sourceService.unsubscribeUser(testUser, 100L);

        assertTrue(testUser.getSubscriptions().isEmpty());
        verify(userService).updateUser(testUser);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void unsubscribeUser_ById_NotFound_ThrowsException() {
        when(sourceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SourceNotFoundException.class, () -> sourceService.unsubscribeUser(testUser, 999L));
        verify(userService, never()).updateUser(any());
    }

    @Test
    void setShowOnlySubscribedSources_UpdatesAndPublishes() {
        when(userService.updateUserFiltering(testUser.getId(), true)).thenReturn(testUser);

        sourceService.setShowOnlySubscribedSources(testUser, true);

        assertTrue(testUser.isShowOnlySubscribedSources());
        verify(userService).updateUserFiltering(testUser.getId(), true);
        verify(sourceUpdatePublisher).publishSourceUpdate(testUser);
    }

    @Test
    void getTopSources_DelegatesToRepository() {
        List<TopSourceProjection> expected = Collections.emptyList();
        when(sourceRepository.getTopSources()).thenReturn(expected);

        List<TopSourceProjection> result = sourceService.getTopSources();

        assertEquals(expected, result);
        verify(sourceRepository).getTopSources();
    }

}