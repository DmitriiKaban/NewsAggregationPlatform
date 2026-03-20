import type { Language } from '../i18n/translations.ts';

export const showMessage = (message: string) => {
    const tg = window.Telegram?.WebApp;
    if (tg?.showAlert) tg.showAlert(message);
    else if (tg?.showPopup) tg.showPopup({message});
    else alert(message);
};

export const getAvatarColor = (str: string) => {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash % 360);
    return `hsl(${hue}, 70%, 60%)`;
};

export const getHandle = (url: string) => {
    if (!url) return '';
    const parts = url.split('/');
    const handle = parts[parts.length - 1];
    return handle.startsWith('@') ? handle : '@' + handle;
};

export const getValidUrl = (url: string) => {
    if (!url) return '#';
    if (url.startsWith('http://') || url.startsWith('https://')) return url;
    const cleanHandle = url.startsWith('@') ? url.substring(1) : url;
    return `https://t.me/${cleanHandle}`;
};

export const getInitials = (name: string) => {
    if (!name) return 'NN';
    const cleanName = name.startsWith('@') ? name.substring(1) : name;
    return cleanName ? cleanName.substring(0, 2).toUpperCase() : 'NN';
};

export const LANGUAGE_MAP: Record<Language, { flag: string; label: string }> = {
    en: { flag: '🇬🇧', label: 'English' },
    ro: { flag: '🇷🇴', label: 'Română' },
    ru: { flag: '🇷🇺', label: 'Русский' }
};