package md.botservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import md.botservice.models.Language;
import md.botservice.models.User;
import md.botservice.utils.KeyboardHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebAppDataHandlerTest {

    @Mock private UserService userService;
    @Mock private SourceService sourceService;
    @Mock private MessageService messageService;
    @Mock private KeyboardHelper keyboardHelper;
    @Mock private AbsSender sender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WebAppDataHandler webAppDataHandler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Update update;

    private User testUser;

    @BeforeEach
    void setUp() {
        webAppDataHandler = new WebAppDataHandler(userService, sourceService, objectMapper, messageService, keyboardHelper);

        testUser = new User();
        testUser.setId(1L);
        testUser.setLanguage(Language.EN);

        when(update.getMessage().getChatId()).thenReturn(123L);
        when(update.getMessage().getFrom().getId()).thenReturn(1L);
    }

    @Test
    void handleWebAppData_SaveInterests() throws TelegramApiException {
        String json = "{\"action\":\"save_interests\", \"interests\":\"Tech, AI\"}";
        when(update.getMessage().getWebAppData().getData()).thenReturn(json);
        when(userService.findOrRegister(any())).thenReturn(testUser);
        when(messageService.get(eq("webapp.interests.updated"), any(), anyString())).thenReturn("Updated");

        webAppDataHandler.handleWebAppData(update, sender);

        verify(userService).updateUser(testUser);
        assertEquals("Tech, AI", testUser.getInterestsRaw());
        verify(sender).execute(any(SendMessage.class));
    }

    @Test
    void handleWebAppData_AddSource() throws TelegramApiException {
        String json = "{\"action\":\"add_source\", \"url\":\"https://t.me/tech\"}";
        when(update.getMessage().getWebAppData().getData()).thenReturn(json);
        when(userService.findOrRegister(any())).thenReturn(testUser);
        when(messageService.get(eq("webapp.source.added"), any(), anyString())).thenReturn("Added");

        webAppDataHandler.handleWebAppData(update, sender);

        verify(sourceService).subscribeUser(testUser, "https://t.me/tech");
        verify(sender).execute(any(SendMessage.class));
    }

    @Test
    void handleWebAppData_RemoveSource() throws TelegramApiException {
        String json = "{\"action\":\"remove_source\", \"url\":\"https://t.me/tech\"}";
        when(update.getMessage().getWebAppData().getData()).thenReturn(json);
        when(userService.findOrRegister(any())).thenReturn(testUser);
        when(messageService.get(eq("webapp.source.removed"), any(), anyString())).thenReturn("Removed");

        webAppDataHandler.handleWebAppData(update, sender);

        verify(sourceService).unsubscribeUser(testUser, "https://t.me/tech");
        verify(sender).execute(any(SendMessage.class));
    }

    @Test
    void handleWebAppData_InvalidJson_SendsErrorMessage() throws TelegramApiException {
        String json = "invalid_json";
        when(update.getMessage().getWebAppData().getData()).thenReturn(json);
        when(userService.findOrRegister(any())).thenReturn(testUser);
        when(messageService.get(eq("webapp.error.processing"), any())).thenReturn("Error");

        webAppDataHandler.handleWebAppData(update, sender);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(sender).execute(messageCaptor.capture());

        assertEquals("Error", messageCaptor.getValue().getText());
    }

}