import { useEffect, useState } from 'react';

export const useTelegram = () => {
    const [isTelegramEnvironment, setIsTelegramEnvironment] = useState(false);
    const [user, setUser] = useState<{ id?: number; first_name?: string; username?: string } | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [displayName, setDisplayName] = useState('there');
    
    const tg = window.Telegram?.WebApp;
    const theme = tg?.themeParams || {};

    const colors = {
        bg: theme.bg_color || '#ffffff',
        text: theme.text_color || '#000000',
        hint: theme.hint_color || '#999999',
        link: theme.link_color || '#2481cc',
        button: theme.button_color || '#2481cc',
        buttonText: theme.button_text_color || '#ffffff',
        secondaryBg: theme.secondary_bg_color || '#f0f0f0',
        success: '#34C759',
        danger: '#FF3B30',
        chartBar: '#FF9500',
    };

    useEffect(() => {
        if (tg) {
            setIsTelegramEnvironment(true);
            try {
                tg.ready();
                tg.expand();
                const userData = tg.initDataUnsafe?.user;
                if (userData?.id) {
                    setUser({ id: userData.id, first_name: userData.first_name || 'User', username: userData.username });
                    setDisplayName(userData.first_name || 'there');
                    return;
                }
                if (tg.initData && tg.initData.length > 0) {
                    const params = new URLSearchParams(tg.initData);
                    const userJson = params.get('user');
                    if (userJson) {
                        const parsedUser = JSON.parse(userJson);
                        setUser({
                            id: parsedUser.id,
                            first_name: parsedUser.first_name || 'User',
                            username: parsedUser.username
                        });
                        setDisplayName(parsedUser.first_name || 'there');
                        return;
                    }
                }
                setError("Could not identify user from Telegram(");
            } catch {
                setError("Failed to initialize Bot");
            }
        } else {
            setError("Must be opened from Telegram");
        }
    }, []);

    return { tg, isTelegramEnvironment, user, error, displayName, colors, setDisplayName };
};