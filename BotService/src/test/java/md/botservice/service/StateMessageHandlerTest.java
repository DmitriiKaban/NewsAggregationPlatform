package md.botservice.service;

import md.botservice.models.Language;
import md.botservice.models.User;
import md.botservice.utils.KeyboardHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateMessageHandlerTest {

    @Mock private UserStateManager stateManager;
    @Mock private UserService userService;
    @Mock private SourceService sourceService;
    @Mock private MessageService messageService;
    @Mock private KeyboardHelper keyboardHelper;
    @Mock private AbsSender sender;

    @InjectMocks
    private StateMessageHandler stateMessageHandler;

    private User testUser;
    private ReplyKeyboardMarkup mockKeyboard;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setLanguage(Language.EN);

        mockKeyboard = new ReplyKeyboardMarkup();
    }

    @Test
    void handleStateMessage_AwaitingInterests_Success() throws TelegramApiException {
        when(stateManager.getState(1L)).thenReturn(UserStateManager.State.AWAITING_INTERESTS);
        when(messageService.get(eq("interests.updated"), any(Language.class), anyString())).thenReturn("Updated");
        when(keyboardHelper.getMainMenuKeyboard(any(Language.class))).thenReturn(mockKeyboard);

        stateMessageHandler.handleStateMessage(testUser, "Java, Spring", 123L, sender);

        verify(userService).updateInterests(1L, "Java, Spring");
        verify(stateManager).clearState(1L);
        verify(keyboardHelper).getMainMenuKeyboard(Language.EN);
        verify(sender).execute(any(SendMessage.class));
    }

    @Test
    void handleStateMessage_AwaitingInterests_Exception_SendsError() throws TelegramApiException {
        when(stateManager.getState(1L)).thenReturn(UserStateManager.State.AWAITING_INTERESTS);
        doThrow(new RuntimeException("DB Error")).when(userService).updateInterests(anyLong(), anyString());
        when(messageService.get(eq("state.error.interests"), any(Language.class))).thenReturn("Error");
        when(keyboardHelper.getMainMenuKeyboard(any(Language.class))).thenReturn(mockKeyboard);

        stateMessageHandler.handleStateMessage(testUser, "Java", 123L, sender);

        verify(stateManager).clearState(1L);
        verify(keyboardHelper).getMainMenuKeyboard(Language.EN);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(sender).execute(messageCaptor.capture());
        assertEquals("Error", messageCaptor.getValue().getText());
        assertEquals(mockKeyboard, messageCaptor.getValue().getReplyMarkup());
    }

    @Test
    void handleStateMessage_AwaitingSource_Success() throws TelegramApiException {
        when(stateManager.getState(1L)).thenReturn(UserStateManager.State.AWAITING_SOURCE_URL);
        when(messageService.get(eq("sources.added"), any(Language.class), anyString())).thenReturn("Source Added");
        when(keyboardHelper.getMainMenuKeyboard(any(Language.class))).thenReturn(mockKeyboard);

        stateMessageHandler.handleStateMessage(testUser, "t.me/news", 123L, sender);

        verify(sourceService).subscribeUser(eq(testUser), anyString());
        verify(stateManager).clearState(1L);
        verify(keyboardHelper).getMainMenuKeyboard(Language.EN);
        verify(sender).execute(any(SendMessage.class));
    }

    @Test
    void handleStateMessage_UnknownState_ClearsState() {
        when(stateManager.getState(1L)).thenReturn(UserStateManager.State.NORMAL);

        stateMessageHandler.handleStateMessage(testUser, "Test", 123L, sender);

        verify(stateManager).clearState(1L);
        verifyNoInteractions(userService, sourceService, sender, keyboardHelper);
    }

}