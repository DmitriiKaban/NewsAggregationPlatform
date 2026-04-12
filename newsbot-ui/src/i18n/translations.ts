export type Language = 'en' | 'ro' | 'ru';

export interface Translations {
    [key: string]: { en: string; ro: string; ru: string };
}

export const translations: Translations = {
    'header.greeting': { en: 'Hey, {name}! 👋', ro: 'Salut, {name}! 👋', ru: 'Привет, {name}! 👋' },
    'header.subtitle': { en: 'Your personalized news hub', ro: 'Centrul tău personalizat de știri', ru: 'Ваш персонализированный новостной центр' },
    'tab.interests': { en: '🎯 Interests', ro: '🎯 Interese', ru: '🎯 Интересы' },
    'tab.sources': { en: '📚 Sources', ro: '📚 Surse', ru: '📚 Источники' },
    'tab.insights': { en: '📊 Insights', ro: '📊 Statistici', ru: '📊 Аналитика' },
    'tab.settings': { en: '⚙️ Settings', ro: '⚙️ Setări', ru: '⚙️ Настройки' },
    'tab.moderator': { en: '🛡️ Mod', ro: '🛡️ Mod', ru: '🛡️ Мод' },
    
    // Interests
    'interests.title': { en: 'Your Current Interests', ro: 'Interesele Tale Curente', ru: 'Ваши Текущие Интересы' },
    'interests.placeholder': { en: 'No interests set yet', ro: 'Niciun interes setat încă', ru: 'Интересы еще не установлены' },
    'interests.button_update': { en: '✏️ Update Interests', ro: '✏️ Actualizează Interesele', ru: '✏️ Обновить Интересы' },
    'interests.input_placeholder': { en: 'AI, Crypto, Tech...', ro: 'AI, Cripto, Tehnologie...', ru: 'ИИ, Крипто, Технологии...' },
    'interests.type_prompt': { en: 'Type and press Enter...', ro: 'Tastează și apasă Enter...', ru: 'Введите и нажмите Enter...' },
    'interests.hint': { en: '💡 Separate topics with commas', ro: '💡 Separă subiectele cu virgule', ru: '💡 Разделяйте темы запятыми' },
    'interests.saved': { en: '✅ Interests saved!', ro: '✅ Interese salvate!', ru: '✅ Интересы сохранены!' },
    'interests.save_button': { en: 'Save Interests', ro: 'Salvează Interesele', ru: 'Сохранить Интересы' },
    'interests.current': { en: 'Current Interests', ro: 'Interese Curente', ru: 'Текущие Интересы' },
    
    // Recommendations
    'recommendations.title': { en: '✨ Recommended for You', ro: '✨ Recomandate pentru Tine', ru: '✨ Рекомендовано для Вас' },
    'recommendations.subtitle': { en: 'People with similar interests follow these:', ro: 'Persoane cu interese similare urmăresc acestea:', ru: 'Люди с похожими интересами подписаны на это:' },
    'recommendations.peers': { en: 'Followed by {count} peer', ro: 'Urmărit de {count} persoană', ru: 'Подписан {count} человек' },
    'recommendations.peers_plural': { en: 'Followed by {count} peers', ro: 'Urmărit de {count} persoane', ru: 'Подписано {count} человек' },
    'recommendations.add': { en: '+ Add', ro: '+ Adaugă', ru: '+ Добавить' },
    
    // Sources
    'sources.strict_mode': { en: 'Strict Mode', ro: 'Mod Strict', ru: 'Строгий Режим' },
    'sources.strict_mode_desc': { en: 'Only show news from my added sources', ro: 'Arată doar știri din sursele mele adăugate', ru: 'Показывать только новости из моих источников' },
    'sources.title': { en: 'Your Sources', ro: 'Sursele Tale', ru: 'Ваши Источники' },
    'sources.add_button': { en: 'Add', ro: 'Adaugă', ru: 'Добавить' },
    'sources.empty': { en: 'No sources yet. Add one!', ro: 'Nicio sursă încă. Adaugă una!', ru: 'Источников пока нет. Добавьте!' },
    'sources.bypass_ai': { en: 'Bypass AI (Get all news)', ro: 'Ocolește AI (Primește toate știrile)', ru: 'Без ИИ (Получать все новости)' },
    'sources.added': { en: '✅ Source added!', ro: '✅ Sursă adăugată!', ru: '✅ Источник добавлен!' },
    'sources.prompt': { en: 'Enter channel (e.g. durov or @channel):', ro: 'Introdu canalul (ex. durov sau @canal):', ru: 'Введите канал (напр. durov или @channel):' },
    
    // Settings
    'settings.title': { en: 'App Settings', ro: 'Setări Aplicație', ru: 'Настройки Приложения' },
    'settings.language': { en: 'Language', ro: 'Limbă', ru: 'Язык' },
    'settings.language_desc': { en: 'Select your preferred language', ro: 'Selectează limba preferată', ru: 'Выберите предпочитаемый язык' },
    'settings.daily_summary': { en: 'Daily AI Summary', ro: 'Rezumat Zilnic AI', ru: 'Ежедневный ИИ-дайджест' },
    'settings.daily_summary_desc': { en: 'Receive a daily summary of your top news', ro: 'Primește un rezumat zilnic al știrilor', ru: 'Получайте ежедневный обзор ваших новостей' },
    'settings.weekly_summary': { en: 'Weekly AI Summary', ro: 'Rezumat Săptămânal AI', ru: 'Еженедельный ИИ-дайджест' },
    'settings.weekly_summary_desc': { en: 'Receive a weekly wrap-up every Sunday', ro: 'Primește un rezumat săptămânal duminica', ru: 'Получайте еженедельный обзор в воскресенье' },
    
    // Insights
    'insights.title': { en: '📈 Platform Analytics', ro: '📈 Analiză Platformă', ru: '📈 Аналитика Платформы' },
    'insights.dau_title': { en: 'Daily Active Users (7 Days)', ro: 'Utilizatori Activi Zilnic (7 Zile)', ru: 'Активные Пользователи (7 Дней)' },
    'insights.dau_desc': { en: 'Users reading news and interacting with the bot.', ro: 'Utilizatori care citesc știri și interacționează cu botul.', ru: 'Пользователи, читающие новости и взаимодействующие с ботом.' },
    'insights.no_data': { en: 'No activity data yet.', ro: 'Încă nu există date de activitate.', ru: 'Данных об активности пока нет.' },
    'insights.top_sources': { en: '🏆 Top Subscribed Sources', ro: '🏆 Cele Mai Abonate Surse', ru: '🏆 Топ Источников по Подпискам' },
    'insights.no_sources': { en: 'No sources added yet.', ro: 'Nicio sursă adăugată încă.', ru: 'Источников пока не добавлено.' },
    'insights.users': { en: 'users', ro: 'utilizatori', ru: 'пользователей' },
    'insights.articles_per_session': { en: 'Articles / Session', ro: 'Articole / Sesiune', ru: 'Статей / Сессия' },
    'insights.topic_entropy': { en: 'Topic Entropy', ro: 'Entropie Subiecte', ru: 'Энтропия Тем' },
    'insights.you': { en: 'You', ro: 'Tu', ru: 'Вы' },
    'insights.global': { en: 'Global:', ro: 'Global:', ru: 'Глобально:' },
    'insights.strict_mode_adoption': { en: 'Strict Mode Adoption', ro: 'Adoptare Mod Strict', ru: 'Использование Строгого Режима' },
    'insights.strict_mode_desc': { en: 'Percentage of total users relying exclusively on their subscribed sources.', ro: 'Procentul utilizatorilor care se bazează exclusiv pe sursele la care sunt abonați.', ru: 'Процент пользователей, полагающихся исключительно на свои подписки.' },
    'insights.most_read_topics': { en: 'Most Read Topics', ro: 'Cele Mai Citite Subiecte', ru: 'Самые Читаемые Темы' },
    'insights.source_sentiment': { en: 'Source Sentiment', ro: 'Sentiment Sursă', ru: 'Отношение к Источнику' },
    'insights.votes': { en: 'votes', ro: 'voturi', ru: 'голосов' },
    'insights.top_read_all': { en: 'Top "Read-All" Sources', ro: 'Top Surse "Citește-Tot"', ru: 'Топ "Читать-Все" Источников' },
    
    // Reports
    'report.title': { en: 'Report Content', ro: 'Raportează Conținut', ru: 'Пожаловаться на Контент' },
    'report.subtitle': { en: 'Why are you reporting this?', ro: 'De ce raportezi acest lucru?', ru: 'Почему вы жалуетесь на это?' },
    'report.reason.SPAM': { en: 'Spam', ro: 'Spam', ru: 'Спам' },
    'report.reason.MISLEADING': { en: 'Misleading / Fake News', ro: 'Înșelător / Știri False', ru: 'Вводит в заблуждение / Фейк' },
    'report.reason.HATE_SPEECH': { en: 'Hate Speech / Inappropriate', ro: 'Discurs Instigator la Ură / Inadecvat', ru: 'Разжигание ненависти / Неприемлемо' },
    'report.reason.BROKEN_LINK': { en: 'Broken Link', ro: 'Link Necorespunzător', ru: 'Нерабочая Ссылка' },
    'report.reason.UNRELATED_CONTENT': { en: 'Unrelated Content', ro: 'Conținut Fără Legătură', ru: 'Несвязанный Контент' },
    'report.reason.OTHER': { en: 'Other', ro: 'Altele', ru: 'Другое' },
    'report.cancel': { en: 'Cancel', ro: 'Anulează', ru: 'Отмена' },
    'report.submitting': { en: 'Submitting...', ro: 'Se trimite...', ru: 'Отправка...' },
    'report.submit': { en: 'Submit Report', ro: 'Trimite Raport', ru: 'Отправить Жалобу' },
    'report.alert.select_reason': { en: 'Please select a reason.', ro: 'Te rugăm să selectezi un motiv.', ru: 'Пожалуйста, выберите причину.' },
    'report.alert.missing_source': { en: 'Error: Source ID is missing. Cannot submit a report for this content.', ro: 'Eroare: ID-ul sursei lipsește. Nu se poate trimite un raport pentru acest conținut.', ru: 'Ошибка: отсутствует ID источника. Невозможно отправить жалобу на этот контент.' },
    'report.alert.success': { en: 'Thank you, your report has been submitted.', ro: 'Mulțumim, raportul tău a fost trimis.', ru: 'Спасибо, ваша жалоба отправлена.' },
    'report.alert.failed': { en: 'Failed to submit report. Please try again.', ro: 'Nu s-a putut trimite raportul. Te rugăm să reîncerci.', ru: 'Не удалось отправить жалобу. Пожалуйста, попробуйте снова.' },

    // Moderator Dashboard
    'mod.title': { en: 'Moderator Dashboard', ro: 'Panou Moderator', ru: 'Панель Модератора' },
    'mod.reports_count': { en: '{count} Reports', ro: '{count} Rapoarte', ru: 'Жалоб: {count}' },
    'mod.loading': { en: 'Loading reports...', ro: 'Se încarcă rapoartele...', ru: 'Загрузка жалоб...' },
    'mod.error.network': { en: 'Network error occurred.', ro: 'A apărut o eroare de rețea.', ru: 'Произошла ошибка сети.' },
    'mod.error.update': { en: 'Failed to update status. Please try again.', ro: 'Eroare la actualizarea statusului. Reîncearcă.', ru: 'Ошибка обновления статуса. Попробуйте еще раз.' },
    'mod.filter.ALL': { en: 'All', ro: 'Toate', ru: 'Все' },
    'mod.filter.PENDING': { en: 'Pending', ro: 'În așteptare', ru: 'Ожидающие' },
    'mod.filter.RESOLVED': { en: 'Resolved', ro: 'Rezolvate', ru: 'Решенные' },
    'mod.filter.DISMISSED': { en: 'Dismissed', ro: 'Respinse', ru: 'Отклоненные' },
    'mod.empty.pending': { en: 'No pending reports. Great job!', ro: 'Niciun raport în așteptare. Treabă excelentă!', ru: 'Нет ожидающих жалоб. Отличная работа!' },
    'mod.empty.filtered': { en: 'No {status} reports found.', ro: 'Nu s-au găsit rapoarte ({status}).', ru: 'Жалобы ({status}) не найдены.' },
    'mod.card.reason': { en: 'Reason', ro: 'Motiv', ru: 'Причина' },
    'mod.card.reported_content': { en: 'Reported Content', ro: 'Conținut Raportat', ru: 'Жалоба на контент' },
    'mod.card.reported_by': { en: 'Reported By', ro: 'Raportat De', ru: 'Пожаловался' },
    'mod.card.anonymous': { en: 'Anonymous', ro: 'Anonim', ru: 'Аноним' },
    'mod.btn.dismiss': { en: 'Dismiss', ro: 'Respinge', ru: 'Отклонить' },
    'mod.btn.resolve': { en: 'Resolve', ro: 'Rezolvă', ru: 'Решить' },
    'mod.btn.resolve_ban': { en: 'Resolve & Ban Article', ro: 'Rezolvă și Banează', ru: 'Решить и Забанить' },
    'mod.confirm.ban_article': { en: 'Are you sure? Resolving this report will permanently BAN this article.', ro: 'Ești sigur? Rezolvarea acestui raport va BANA permanent articolul.', ru: 'Вы уверены? Решение этой жалобы навсегда ЗАБАНИТ статью.' },
    'mod.article': { en: 'Article', ro: 'Articol', ru: 'Статья' },
    'mod.source': { en: 'Source', ro: 'Sursă', ru: 'Источник' },
    'mod.via': { en: 'via', ro: 'prin', ru: 'через' },

    // Errors & Global
    'loading': { en: 'Loading...', ro: 'Se încarcă...', ru: 'Загрузка...' },
    'offline.title': { en: '⚠️ Offline Mode', ro: '⚠️ Mod Offline', ru: '⚠️ Офлайн Режим' },
    'offline.desc': { en: 'Backend not reachable', ro: 'Backend-ul nu este disponibil', ru: 'Бэкенд недоступен' },
    'offline.retry': { en: 'Retry', ro: 'Reîncearcă', ru: 'Повторить' },
    'error.update_settings': { en: 'Failed to update settings', ro: 'Nu s-au putut actualiza setările', ru: 'Не удалось обновить настройки' },
    'error.update_preference': { en: 'Failed to update preference', ro: 'Nu s-a putut actualiza preferința', ru: 'Не удалось обновить предпочтение' },
    'error.add_source': { en: 'Failed to add source.', ro: 'Nu s-a putut adăuga sursa.', ru: 'Не удалось добавить источник.' },
    'error.remove_source': { en: 'Failed to remove.', ro: 'Nu s-a putut elimina.', ru: 'Не удалось удалить.' },
    'error.user_not_found': { en: 'Could Not Find User', ro: 'Nu S-a Găsit Utilizatorul', ru: 'Пользователь Не Найден' },
    'error.open_from_telegram': { en: 'Please open this app from Telegram.', ro: 'Te rugăm să deschizi această aplicație din Telegram.', ru: 'Пожалуйста, откройте это приложение из Telegram.' },
    'error.backend_unreachable': { en: 'Cannot save: Backend is unreachable.', ro: 'Nu se poate salva: Backend-ul este indisponibil.', ru: 'Не удалось сохранить: Бэкенд недоступен.' },
    'error.failed_prefix': { en: 'Failed: {message}', ro: 'Eșuat: {message}', ru: 'Ошибка: {message}' },
};

export function tr(
    key: string,
    lang: Language,
    params?: Record<string, any>
): string {
    let text = translations[key]?.[lang] ?? key;
    if (params) {
        Object.keys(params).forEach((p) => {
            text = text.replace(`{${p}}`, String(params[p]));
        });
    }
    return text;
}