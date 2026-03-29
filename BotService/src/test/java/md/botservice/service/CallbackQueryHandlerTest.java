package md.botservice.service;

import md.botservice.dto.ReportRequest;
import md.botservice.models.*;
import md.botservice.producers.EventTrackingService;
import md.botservice.utils.KeyboardHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackQueryHandlerTest {

    @Mock private UserStateManager stateManager;
    @Mock private SourceService sourceService;
    @Mock private UserService userService;
    @Mock private EventTrackingService eventTrackingService;
    @Mock private MessageService messageService;
    @Mock private KeyboardHelper keyboardHelper;
    @Mock private ReportService reportService;
    @Mock private AbsSender sender;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CallbackQuery callbackQuery;

    @InjectMocks
    private CallbackQueryHandler callbackQueryHandler;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setLanguage(Language.EN);
        testUser.setShowOnlySubscribedSources(true);

        lenient().when(callbackQuery.getMessage().getChatId()).thenReturn(123L);
        lenient().when(callbackQuery.getMessage().getMessageId()).thenReturn(456);
        lenient().when(callbackQuery.getFrom().getId()).thenReturn(1L);
        lenient().when(callbackQuery.getId()).thenReturn("query_id");
        lenient().when(userService.findOrRegister(any())).thenReturn(testUser);

        lenient().when(messageService.get(anyString(), any(Language.class)))
                .thenReturn("Mocked Button Text");
        lenient().when(messageService.get(anyString(), any(Language.class), any(Object[].class)))
                .thenReturn("Mocked Button Text");
    }

    @Test
    void handleCallbackQuery_LikePost() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("LIKE_POST:100");

        callbackQueryHandler.handleCallbackQuery(callbackQuery, sender);

        verify(eventTrackingService).trackReaction(1L, "100", ReactionType.LIKE);

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(sender).execute(answerCaptor.capture());
        assertEquals("Mocked Button Text", answerCaptor.getValue().getText());
    }

    @Test
    void handleCallbackQuery_ReportPost_OpensMenu() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("REPORT_POST:100:200");

        callbackQueryHandler.handleCallbackQuery(callbackQuery, sender);

        ArgumentCaptor<EditMessageReplyMarkup> editCaptor = ArgumentCaptor.forClass(EditMessageReplyMarkup.class);
        verify(sender).execute(editCaptor.capture());
        assertEquals("123", editCaptor.getValue().getChatId());
        assertEquals(456, editCaptor.getValue().getMessageId());
    }

    @Test
    void handleCallbackQuery_SubmitReport_Success() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("SUBMIT_REPORT:100:200:SPAM");

        callbackQueryHandler.handleCallbackQuery(callbackQuery, sender);

        ArgumentCaptor<ReportRequest> reportCaptor = ArgumentCaptor.forClass(ReportRequest.class);
        verify(reportService).submitReport(reportCaptor.capture());

        assertEquals(1L, reportCaptor.getValue().getReporterId());
        assertEquals(100L, reportCaptor.getValue().getArticleId());
        assertEquals(200L, reportCaptor.getValue().getSourceId());
        assertEquals(ReportReason.SPAM, reportCaptor.getValue().getReason());

        verify(sender).execute(any(EditMessageReplyMarkup.class));
    }

    @Test
    void handleCallbackQuery_ChangeLanguage() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("LANG_RU");

        callbackQueryHandler.handleCallbackQuery(callbackQuery, sender);

        assertEquals("ru", testUser.getPreferredLanguage());
        verify(userService).updateUser(testUser);
        verify(sender).execute(any(EditMessageText.class));
        verify(sender).execute(any(SendMessage.class));
    }

    @Test
    void handleCallbackQuery_ToggleStrictMode() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("TOGGLE_STRICT_MODE");
        testUser.setShowOnlySubscribedSources(false);

        callbackQueryHandler.handleCallbackQuery(callbackQuery, sender);

        assertTrue(testUser.isShowOnlySubscribedSources());
        verify(userService).updateUser(testUser);

        ArgumentCaptor<EditMessageText> editCaptor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(sender).execute(editCaptor.capture());
        assertEquals("Mocked Button Text", editCaptor.getValue().getText());
    }

    @Test
    void handleCallbackQuery_AddSourceCommand() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("CMD_ADD_SOURCE");

        callbackQueryHandler.handleCallbackQuery(callbackQuery, sender);

        verify(stateManager).setState(1L, UserStateManager.State.AWAITING_SOURCE_URL);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(sender).execute(messageCaptor.capture());
        assertEquals("Mocked Button Text", messageCaptor.getValue().getText());
    }

    @Test
    void handleCallbackQuery_RemoveSource() throws TelegramApiException {
        when(callbackQuery.getData()).thenReturn("REMOVE_SOURCE:99");

        callbackQueryHandler.handleCallbackQuery(callbackQuery, sender);

        verify(sourceService).unsubscribeUser(testUser, 99L);
        verify(sender).execute(any(SendMessage.class));
    }

}