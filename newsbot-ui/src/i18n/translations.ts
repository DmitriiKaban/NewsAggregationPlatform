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
    'interests.title': { en: 'Your Current Interests', ro: 'Interesele Tale Curente', ru: 'Ваши Текущие Интересы' },
    'interests.placeholder': { en: 'No interests set yet', ro: 'Niciun interes setat încă', ru: 'Интересы еще не установлены' },
    'interests.button_update': { en: '✏️ Update Interests', ro: '✏️ Actualizează Interesele', ru: '✏️ Обновить Интересы' },
    'interests.input_placeholder': { en: 'AI, Crypto, Tech...', ro: 'AI, Cripto, Tehnologie...', ru: 'ИИ, Крипто, Технологии...' },
    'interests.hint': { en: '💡 Separate topics with commas', ro: '💡 Separă subiectele cu virgule', ru: '💡 Разделяйте темы запятыми' },
    'interests.saved': { en: '✅ Interests saved!', ro: '✅ Interese salvate!', ru: '✅ Интересы сохранены!' },
    'interests.save_button': { en: 'Save Interests', ro: 'Salvează Interesele', ru: 'Сохранить Интересы' },
    'interests.mode_strict': { en: 'Strict Mode: ON', ro: 'Mod Strict: ACTIVAT', ru: 'Строгий Режим: ВКЛ' },
    'interests.mode_ai': { en: 'AI Mode: Active', ro: 'Mod AI: Activ', ru: 'AI Режим: Активен' },
    'interests.mode_strict_desc': { en: 'Only showing news from your subscribed sources', ro: 'Se afișează doar știri din sursele tale abonate', ru: 'Показываются только новости из ваших источников' },
    'interests.mode_ai_desc': { en: 'AI filtering news from all sources based on your interests', ro: 'AI filtrează știri din toate sursele pe baza intereselor tale', ru: 'ИИ фильтрует новости из всех источников на основе ваших интересов' },
    'recommendations.title': { en: '✨ Recommended for You', ro: '✨ Recomandate pentru Tine', ru: '✨ Рекомендовано для Вас' },
    'recommendations.subtitle': { en: 'People with similar interests follow these:', ro: 'Persoane cu interese similare urmăresc acestea:', ru: 'Люди с похожими интересами подписаны на это:' },
    'recommendations.peers': { en: 'Followed by {count} peer', ro: 'Urmărit de {count} persoană', ru: 'Подписан {count} человек' },
    'recommendations.peers_plural': { en: 'Followed by {count} peers', ro: 'Urmărit de {count} persoane', ru: 'Подписано {count} человек' },
    'recommendations.add': { en: '+ Add', ro: '+ Adaugă', ru: '+ Добавить' },
    'sources.strict_mode': { en: 'Strict Mode', ro: 'Mod Strict', ru: 'Строгий Режим' },
    'sources.strict_mode_desc': { en: 'Only show news from my added sources', ro: 'Arată doar știri din sursele mele adăugate', ru: 'Показывать только новости из моих источников' },
    'sources.title': { en: 'Your Sources', ro: 'Sursele Tale', ru: 'Ваши Источники' },
    'sources.add_button': { en: '+ Add', ro: '+ Adaugă', ru: '+ Добавить' },
    'sources.empty': { en: 'No sources yet. Add one!', ro: 'Nicio sursă încă. Adaugă una!', ru: 'Источников пока нет. Добавьте!' },
    'sources.bypass_ai': { en: 'Bypass AI (Get all news)', ro: 'Ocolește AI (Primește toate știrile)', ru: 'Без ИИ (Получать все новости)' },
    'sources.added': { en: '✅ Source added!', ro: '✅ Sursă adăugată!', ru: '✅ Источник добавлен!' },
    'sources.prompt': { en: 'Enter channel (e.g. durov or @channel):', ro: 'Introdu canalul (ex. durov sau @canal):', ru: 'Введите канал (напр. durov или @channel):' },
    'insights.title': { en: '📈 Platform Analytics', ro: '📈 Analiză Platformă', ru: '📈 Аналитика Платформы' },
    'insights.dau_title': { en: 'Daily Active Users (7 Days)', ro: 'Utilizatori Activi Zilnic (7 Zile)', ru: 'Активные Пользователи (7 Дней)' },
    'insights.dau_desc': { en: 'Users reading news and interacting with the bot.', ro: 'Utilizatori care citesc știri și interacționează cu botul.', ru: 'Пользователи, читающие новости и взаимодействующие с ботом.' },
    'insights.no_data': { en: 'No activity data yet.', ro: 'Încă nu există date de activitate.', ru: 'Данных об активности пока нет.' },
    'insights.top_sources': { en: '🏆 Top Subscribed Sources', ro: '🏆 Cele Mai Abonate Surse', ru: '🏆 Топ Источников по Подпискам' },
    'insights.no_sources': { en: 'No sources added yet.', ro: 'Nicio sursă adăugată încă.', ru: 'Источников пока не добавлено.' },
    'insights.users': { en: 'users', ro: 'utilizatori', ru: 'пользователей' },
    'error.update_settings': { en: 'Failed to update settings', ro: 'Nu s-au putut actualiza setările', ru: 'Не удалось обновить настройки' },
    'error.update_preference': { en: 'Failed to update preference', ro: 'Nu s-a putut actualiza preferința', ru: 'Не удалось обновить предпочтение' },
    'error.add_source': { en: 'Failed to add source.', ro: 'Nu s-a putut adăuga sursa.', ru: 'Не удалось добавить источник.' },
    'error.remove_source': { en: 'Failed to remove.', ro: 'Nu s-a putut elimina.', ru: 'Не удалось удалить.' },
    'error.user_not_found': { en: 'Could Not Find User', ro: 'Nu S-a Găsit Utilizatorul', ru: 'Пользователь Не Найден' },
    'error.open_from_telegram': { en: 'Please open this app from Telegram.', ro: 'Te rugăm să deschizi această aplicație din Telegram.', ru: 'Пожалуйста, откройте это приложение из Telegram.' },
    'loading': { en: 'Loading...', ro: 'Se încarcă...', ru: 'Загрузка...' },
    'offline.title': { en: '⚠️ Offline Mode', ro: '⚠️ Mod Offline', ru: '⚠️ Офлайн Режим' },
    'offline.desc': { en: 'Backend not reachable', ro: 'Backend-ul nu este disponibil', ru: 'Бэкенд недоступен' },
    'offline.retry': { en: 'Retry', ro: 'Reîncearcă', ru: 'Повторить' },
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

