package md.botservice.service;

import md.botservice.commands.CommandEffectFactory;
import md.botservice.commands.CommandStrategy;
import md.botservice.events.NewsNotificationEvent;
import md.botservice.models.Command;
import md.botservice.models.User;
import md.botservice.producers.EventTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppData;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramBotServiceTest {

    @Mock private UserService userService;
    @Mock private EventTrackingService eventTrackingService;
    @Mock private CommandEffectFactory commandFactory;
    @Mock private WebAppDataHandler webAppDataHandler;
    @Mock private CallbackQueryHandler callbackQueryHandler;
    @Mock private UserStateManager stateManager;
    @Mock private StateMessageHandler stateMessageHandler;
    @Mock private UserActivityService activityService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Update update;

    private TelegramBotService telegramBotService;
    private User testUser;

    @BeforeEach
    void setUp() {
        telegramBotService = spy(new TelegramBotService(
                userService, eventTrackingService, commandFactory, webAppDataHandler,
                callbackQueryHandler, stateManager, stateMessageHandler,
                "TestBot", "12345:TOKEN", activityService
        ));

        testUser = new User();
        testUser.setId(1L);
    }

    @Test
    void onUpdateReceived_HasCallbackQuery_DelegatesToHandler() {
        CallbackQuery callbackQuery = mock(CallbackQuery.class, Answers.RETURNS_DEEP_STUBS);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getFrom().getId()).thenReturn(1L);

        telegramBotService.onUpdateReceived(update);

        verify(callbackQueryHandler).handleCallbackQuery(callbackQuery, telegramBotService);
        verify(activityService).recordActivity(1L);
    }

    @Test
    void onUpdateReceived_HasWebAppData_DelegatesToHandler() {
        when(update.hasCallbackQuery()).thenReturn(false);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage().getWebAppData()).thenReturn(new WebAppData());

        telegramBotService.onUpdateReceived(update);

        verify(webAppDataHandler).handleWebAppData(update, telegramBotService);
    }

    @Test
    void onUpdateReceived_StateMessage_DelegatesToStateHandler() {
        when(update.hasCallbackQuery()).thenReturn(false);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage().getWebAppData()).thenReturn(null);
        when(update.getMessage().hasText()).thenReturn(true);
        when(update.getMessage().getText()).thenReturn("Some Input");
        when(update.getMessage().getChatId()).thenReturn(123L);
        when(userService.findOrRegister(any())).thenReturn(testUser);

        when(stateManager.isAwaitingInput(1L)).thenReturn(true);

        telegramBotService.onUpdateReceived(update);

        verify(stateMessageHandler).handleStateMessage(testUser, "Some Input", 123L, telegramBotService);
        verifyNoInteractions(commandFactory);
    }

    @Test
    void onUpdateReceived_StandardCommand_ExecutesStrategy() {
        when(update.hasCallbackQuery()).thenReturn(false);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage().getWebAppData()).thenReturn(null);
        when(update.getMessage().hasText()).thenReturn(true);
        when(update.getMessage().getText()).thenReturn("/start");
        when(update.getMessage().getChatId()).thenReturn(123L);
        when(userService.findOrRegister(any())).thenReturn(testUser);

        when(stateManager.isAwaitingInput(1L)).thenReturn(false);

        CommandStrategy mockStrategy = mock(CommandStrategy.class);
        when(commandFactory.getStrategy(any(Command.class))).thenReturn(mockStrategy);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<Command> commandCaptor = ArgumentCaptor.forClass(Command.class);
        verify(mockStrategy).execute(commandCaptor.capture(), eq(telegramBotService));

        assertEquals("/start", commandCaptor.getValue().commandText());
    }

    @Test
    void sendNewsAlert_Success_TracksShownEvent() throws TelegramApiException {
        NewsNotificationEvent event = new NewsNotificationEvent(1L, "10", "Tech News Title", "Tech", "Source", "http://url", 100L);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();

        doReturn(null).when(telegramBotService).execute(any(SendMessage.class));

        telegramBotService.sendNewsAlert(event, keyboard);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals("1", sentMessage.getChatId());
        assertTrue(sentMessage.getText().contains("Tech News Title"));
        assertEquals(keyboard, sentMessage.getReplyMarkup());

        verify(eventTrackingService).trackArticleShown(eq(1L), eq("10"), anyString(), anyString());
    }

}