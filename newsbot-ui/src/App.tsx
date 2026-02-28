import {useEffect, useState} from 'react';
import {tr, type Language} from './i18n/translations.ts';

declare global {
    interface Window {
        Telegram?: {
            WebApp: {
                ready: () => void;
                close: () => void;
                sendData: (data: string) => void;
                expand: () => void;
                showAlert?: (message: string, callback?: () => void) => void;
                showPopup?: (params: {
                    message: string;
                    buttons?: any[]
                }, callback?: (buttonId: string) => void) => void;
                initData?: string;
                initDataUnsafe?: {
                    user?: {
                        id?: number;
                        first_name?: string;
                        last_name?: string;
                        username?: string;
                        language_code?: string;
                    };
                };
                themeParams?: {
                    bg_color?: string;
                    text_color?: string;
                    hint_color?: string;
                    link_color?: string;
                    button_color?: string;
                    button_text_color?: string;
                    secondary_bg_color?: string;
                };
                MainButton: {
                    setText: (text: string) => void;
                    show: () => void;
                    hide: () => void;
                    enable: () => void;
                    disable?: () => void;
                    showProgress?: (leaveActive?: boolean) => void;
                    hideProgress?: () => void;
                    onClick: (callback: () => void) => void;
                    offClick: (callback: () => void) => void;
                };
                BackButton: {
                    show: () => void;
                    hide: () => void;
                    onClick: (callback: () => void) => void;
                    offClick: (callback: () => void) => void;
                };
            };
        };
    }
}

// ─── Types ───────────────────────────────────────────────────────────────────

interface Source {
    id: number;
    url: string;
    isReadAll: boolean;
}

interface Recommendation {
    name: string;
    url: string;
    peerCount: number;
}

interface TopSource {
    name: string;
    subscriberCount: number;
}

interface DauData {
    date: string;
    count: number;
}


// ─── Helpers ──────────────────────────────────────────────────────────────────

const showMessage = (message: string) => {
    const tg = window.Telegram?.WebApp;
    if (tg?.showAlert) tg.showAlert(message);
    else if (tg?.showPopup) tg.showPopup({message});
    else console.log('📢---', message);
};

// ─── App ──────────────────────────────────────────────────────────────────────

export default function App() {
    const [lang, setLang] = useState<Language>('en');
    const [interests, setInterests] = useState('');
    const [originalInterests, setOriginalInterests] = useState('');
    const [isEditingInterests, setIsEditingInterests] = useState(false);
    const [sources, setSources] = useState<Source[]>([]);
    const [strictMode, setStrictMode] = useState(false);
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [topSources, setTopSources] = useState<TopSource[]>([]);
    const [dauStats, setDauStats] = useState<DauData[]>([]);
    const [activeTab, setActiveTab] = useState<'interests' | 'sources' | 'insights'>('interests');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [backendReachable, setBackendReachable] = useState(true);
    const [isTelegramEnvironment, setIsTelegramEnvironment] = useState(false);
    const [user, setUser] = useState<{ id?: number; first_name?: string; username?: string } | null>(null);
    const [displayName, setDisplayName] = useState('there');
    const [languageLoaded, setLanguageLoaded] = useState(false);

    const apiBaseUrl = "https://f5a7-212-28-65-233.ngrok-free.app/api";
    const tg = window.Telegram?.WebApp;
    const theme = tg?.themeParams || {};

    const colors = {
        bg: theme.bg_color || 'var(--tg-theme-bg-color, #ffffff)',
        text: theme.text_color || 'var(--tg-theme-text-color, #000000)',
        hint: theme.hint_color || 'var(--tg-theme-hint-color, #999999)',
        link: theme.link_color || 'var(--tg-theme-link-color, #2481cc)',
        button: theme.button_color || 'var(--tg-theme-button-color, #2481cc)',
        buttonText: theme.button_text_color || 'var(--tg-theme-button-text-color, #ffffff)',
        secondaryBg: theme.secondary_bg_color || 'var(--tg-theme-secondary-bg-color, #f0f0f0)',
        success: '#34C759',
        danger: '#FF3B30',
        chartBar: '#FF9500',
    };

    // ── Init Telegram user ───────────────────────────────────────────────────
    useEffect(() => {
        if (tg) {
            setIsTelegramEnvironment(true);
            try {
                tg.ready();
                tg.expand();
                const userData = tg.initDataUnsafe?.user;
                if (userData?.id) {
                    setUser({id: userData.id, first_name: userData.first_name || 'User', username: userData.username});
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
                setUser(null);
                setError("Could not identify user from Telegram");
                setLoading(false);
            } catch {
                setUser(null);
                setError("Failed to initialize Bot");
                setLoading(false);
            }
        } else {
            setUser(null);
            setError("Must be opened from Telegram");
            setLoading(false);
        }
    }, []);

    // ── Fetch profile + analytics ────────────────────────────────────────────
    useEffect(() => {
        if (!user?.id) {
            setLoading(false);
            return;
        }

        const controller = new AbortController();
        const fetchOptions = {
            headers: {"ngrok-skip-browser-warning": "69420", "Content-Type": "application/json"},
            signal: controller.signal,
        };

        Promise.all([
            fetch(`${apiBaseUrl}/users/${user.id}/profile`, fetchOptions).then(r => r.json()),
            fetch(`${apiBaseUrl}/analytics/users/${user.id}/recommendations`, fetchOptions).then(r => r.json()).catch(() => []),
            fetch(`${apiBaseUrl}/analytics/insights`, fetchOptions).then(r => r.json()).catch(() => ({
                topSources: [],
                dauStats: []
            })),
        ])
            .then(([profileData, recommendationsData, insightsData]) => {
                console.log("Full profile data:", profileData);
                console.log("Language field:", profileData.language);
                console.log("Language valid:", ['en', 'ro', 'ru'].includes(profileData.language));

                const backendLang = profileData.language?.toLowerCase();

                if (backendLang && ['en', 'ro', 'ru'].includes(backendLang)) {
                    setLang(backendLang as Language);
                    console.log("Language set to:", backendLang);
                }
                setLanguageLoaded(true);

                if (profileData.firstName?.trim()) setDisplayName(profileData.firstName);

                const interestsStr = Array.isArray(profileData.interests)
                    ? profileData.interests.join(', ')
                    : (profileData.interests || '');
                setInterests(interestsStr);
                setOriginalInterests(interestsStr);
                setStrictMode(profileData.strictSourceFiltering || false);

                if (Array.isArray(profileData.sources)) {
                    setSources(
                        profileData.sources
                            .map((s: any) => ({id: s.id, url: s.url || s.name || '', isReadAll: s.isReadAll || false}))
                            .filter((s: Source) => s.url)
                    );
                }

                setRecommendations(recommendationsData);
                setDauStats(insightsData.dauStats || []);
                setTopSources(insightsData.topSources || []);
                setLoading(false);
                setError(null);
                setBackendReachable(true);
            })
            .catch(err => {
                if (err.name !== 'AbortError') {
                    setError('Cannot connect to backend');
                    setBackendReachable(false);
                }
                setLanguageLoaded(true);
                setLoading(false);
            });

        return () => controller.abort();
    }, [user?.id, apiBaseUrl]);

    // ── Back button ──────────────────────────────────────────────────────────
    useEffect(() => {
        if (!isTelegramEnvironment) return;
        const handleBack = () => {
            if (isEditingInterests) {
                setIsEditingInterests(false);
                setInterests(originalInterests);
            } else {
                setActiveTab('interests');
            }
        };

        if (activeTab !== 'interests' || isEditingInterests) {
            tg?.BackButton.show();
            tg?.BackButton.onClick(handleBack);
            return () => {
                tg?.BackButton.offClick(handleBack);
                tg?.BackButton.hide();
            };
        } else {
            tg?.BackButton.hide();
        }
    }, [activeTab, isEditingInterests, originalInterests, isTelegramEnvironment]);

    // ── Main button (save interests) ─────────────────────────────────────────
    useEffect(() => {
        if (!isTelegramEnvironment) return;

        const handleSaveInterests = async () => {
            if (!user?.id || !backendReachable) return;
            try {
                tg?.MainButton.showProgress?.();
                const response = await fetch(`${apiBaseUrl}/users/${user.id}/interests`, {
                    method: 'POST',
                    headers: {"ngrok-skip-browser-warning": "69420", "Content-Type": "application/json"},
                    body: JSON.stringify({interest: interests}),
                });
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                tg?.MainButton.hideProgress?.();
                setOriginalInterests(interests);
                setIsEditingInterests(false);
                showMessage(tr('interests.saved', lang));
            } catch (err: any) {
                tg?.MainButton.hideProgress?.();
                showMessage(`Failed: ${err.message}`);
            }
        };

        if (activeTab === 'interests' && isEditingInterests && interests.trim()) {
            if (tg?.MainButton) {
                tg.MainButton.setText(tr('interests.save_button', lang));
                tg.MainButton.enable();
                tg.MainButton.show();
                tg.MainButton.onClick(handleSaveInterests);
                return () => tg.MainButton.offClick(handleSaveInterests);
            }
        } else {
            tg?.MainButton.hide();
        }
    }, [activeTab, isEditingInterests, interests, user?.id, backendReachable, apiBaseUrl, isTelegramEnvironment, lang]);

    // ── Actions ──────────────────────────────────────────────────────────────
    const toggleStrictMode = async () => {
        if (!backendReachable || !user?.id) return;
        const newState = !strictMode;
        setStrictMode(newState);
        try {
            const res = await fetch(`${apiBaseUrl}/users/${user.id}/settings/strict-filtering?enabled=${newState}`, {
                method: 'PUT', headers: {"ngrok-skip-browser-warning": "69420"},
            });
            if (!res.ok) throw new Error();
        } catch {
            setStrictMode(!newState);
            showMessage(tr('error.update_settings', lang));
        }
    };

    const toggleReadAll = async (sourceId: number, index: number) => {
        if (!backendReachable || !user?.id) return;
        const newState = !sources[index].isReadAll;
        const updated = [...sources];
        updated[index].isReadAll = newState;
        setSources(updated);
        try {
            const res = await fetch(`${apiBaseUrl}/users/${user.id}/sources/${sourceId}/read-all?readAll=${newState}`, {
                method: 'PUT', headers: {"ngrok-skip-browser-warning": "69420"},
            });
            if (!res.ok) throw new Error();
        } catch {
            updated[index].isReadAll = !newState;
            setSources([...updated]);
            showMessage(tr('error.update_preference', lang));
        }
    };

    const handleAddSource = async (directUrl?: string) => {
        if (!backendReachable || !user?.id) return;
        const input = directUrl || prompt(tr('sources.prompt', lang));
        if (!input) return;

        try {
            const res = await fetch(`${apiBaseUrl}/users/${user.id}/sources`, {
                method: 'POST',
                headers: {"ngrok-skip-browser-warning": "69420", "Content-Type": "application/json"},
                body: JSON.stringify({source: input}),
            });
            if (!res.ok) throw new Error();

            const profileRes = await fetch(`${apiBaseUrl}/users/${user.id}/profile`, {
                headers: {"ngrok-skip-browser-warning": "69420"},
            });
            if (profileRes.ok) {
                const data = await profileRes.json();
                if (Array.isArray(data.sources)) {
                    setSources(data.sources.map((s: any) => ({
                        id: s.id,
                        url: s.url || s.name || '',
                        isReadAll: s.isReadAll || false
                    })).filter((s: Source) => s.url));
                }
                if (directUrl) setRecommendations(recommendations.filter(r => r.url !== directUrl));
                if (!directUrl) setActiveTab('sources');
                showMessage(tr('sources.added', lang));
            }
        } catch {
            showMessage(tr('error.add_source', lang));
        }
    };

    const handleRemoveSource = async (url: string, index: number) => {
        if (!backendReachable || !user?.id) return;
        try {
            const res = await fetch(`${apiBaseUrl}/users/${user.id}/sources?url=${encodeURIComponent(url)}`, {
                method: 'DELETE', headers: {"ngrok-skip-browser-warning": "69420"},
            });
            if (!res.ok) throw new Error();
            setSources(sources.filter((_, i) => i !== index));
        } catch {
            showMessage(tr('error.remove_source', lang));
        }
    };

    const maxDau = dauStats.length > 0 ? Math.max(...dauStats.map(d => d.count)) : 1;

    // ── Loading ───────────────────────────────────────────────────────────────
    if (loading || !languageLoaded) return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        }}>
            <div style={{textAlign: 'center', color: colors.text}}>
                <div style={{fontSize: '64px', marginBottom: '24px'}}>📰</div>
                <div style={{fontSize: '18px', fontWeight: '500'}}>{tr('loading', lang)}</div>
            </div>
        </div>
    );

    // ── No user ───────────────────────────────────────────────────────────────
    if (!user) return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            fontFamily: '-apple-system, sans-serif'
        }}>
            <div style={{textAlign: 'center', maxWidth: '400px', color: colors.text}}>
                <div style={{fontSize: '64px', marginBottom: '24px'}}>❌</div>
                <h2 style={{
                    fontSize: '24px',
                    fontWeight: '600',
                    margin: '0 0 12px 0'
                }}>{tr('error.user_not_found', lang)}</h2>
                <p style={{
                    fontSize: '15px',
                    color: colors.hint,
                    lineHeight: 1.5,
                    margin: '0 0 20px 0'
                }}>{tr('error.open_from_telegram', lang)}</p>
                {error && (
                    <div style={{
                        padding: '12px',
                        background: `${colors.hint}20`,
                        borderRadius: '8px',
                        fontSize: '13px',
                        color: colors.hint,
                        textAlign: 'left'
                    }}>
                        <strong>Error:</strong> {error}
                    </div>
                )}
            </div>
        </div>
    );

    // ── Main UI ───────────────────────────────────────────────────────────────
    return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            color: colors.text,
            fontFamily: '-apple-system, sans-serif',
            paddingBottom: '20px'
        }}>

            {/* Header */}
            <div style={{padding: '20px', borderBottom: `1px solid ${colors.hint}40`}}>
                <h1 style={{fontSize: '28px', fontWeight: '700', margin: '0 0 8px 0', color: colors.text}}>
                    {tr('header.greeting', lang, {name: displayName})}
                </h1>
                <p style={{fontSize: '15px', color: colors.hint, margin: 0}}>{tr('header.subtitle', lang)}</p>
            </div>

            {/* Tabs */}
            <div style={{display: 'flex', borderBottom: `2px solid ${colors.hint}20`, background: colors.secondaryBg}}>
                <TabButton active={activeTab === 'interests'} onClick={() => {
                    setActiveTab('interests');
                    setIsEditingInterests(false);
                }} colors={colors}>
                    {tr('tab.interests', lang)}
                </TabButton>
                <TabButton active={activeTab === 'sources'} onClick={() => {
                    setActiveTab('sources');
                    setIsEditingInterests(false);
                }} colors={colors}>
                    {tr('tab.sources', lang)}
                </TabButton>
                <TabButton active={activeTab === 'insights'} onClick={() => {
                    setActiveTab('insights');
                    setIsEditingInterests(false);
                }} colors={colors}>
                    {tr('tab.insights', lang)}
                </TabButton>
            </div>

            <div style={{padding: '20px'}}>

                {/* ── Interests Tab ── */}

                {activeTab === 'interests' && (
                    <div>

                        <label style={{
                            display: 'block',
                            fontSize: '15px',
                            fontWeight: '600',
                            marginBottom: '12px',
                            color: colors.text
                        }}>
                            {tr('interests.title', lang)}
                        </label>

                        {/* Editable Interests Area */}
                        <div
                            onClick={() => !isEditingInterests && setIsEditingInterests(true)}
                            style={{
                                cursor: !isEditingInterests ? 'pointer' : 'default',
                                position: 'relative'
                            }}
                        >

                            {!isEditingInterests ? (
                                <div style={{
                                    padding: '16px',
                                    background: colors.secondaryBg,
                                    borderRadius: '12px',
                                    border: `1px solid ${colors.hint}40`,
                                    minHeight: '80px',
                                    fontSize: '15px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    transition: 'all 0.2s'
                                }}>
                                    {originalInterests ? (
                                        <span style={{ color: colors.text }}>{originalInterests}</span>
                                    ) : (
                                        <span style={{ color: colors.hint, fontStyle: 'italic' }}>
                            {tr('interests.placeholder', lang)}
                        </span>
                                    )}
                                    <span style={{
                                        fontSize: '18px',
                                        color: colors.hint,
                                        marginLeft: '12px',
                                        opacity: 0.6
                                    }}>✏️</span>
                                </div>
                            ) : (
                                <div>
                    <textarea
                        value={interests}
                        onChange={e => setInterests(e.target.value)}
                        placeholder={tr('interests.input_placeholder', lang)}
                        autoFocus
                        style={{
                            width: '100%',
                            minHeight: '120px',
                            padding: '14px',
                            fontSize: '15px',
                            border: `2px solid ${colors.button}`,
                            borderRadius: '12px',
                            background: colors.bg,
                            color: colors.text,
                            boxSizing: 'border-box',
                            resize: 'vertical',
                            outline: 'none',
                            fontFamily: 'inherit'
                        }}
                    />
                                    <p style={{
                                        fontSize: '13px',
                                        color: colors.hint,
                                        marginTop: '8px',
                                        marginBottom: 0
                                    }}>
                                        {tr('interests.hint', lang)}
                                    </p>
                                </div>
                            )}
                        </div>

                        {/* Mode indicator */}
                        <div style={{
                            marginTop: '16px',
                            padding: '10px 14px',
                            background: strictMode ? `${colors.danger}15` : `${colors.button}15`,
                            borderRadius: '10px',
                            border: `1px solid ${strictMode ? colors.danger : colors.button}30`
                        }}>
                            <div style={{
                                fontSize: '13px',
                                fontWeight: '600',
                                color: strictMode ? colors.danger : colors.button
                            }}>
                                {strictMode ? tr('interests.mode_strict', lang) : tr('interests.mode_ai', lang)}
                            </div>
                            <div style={{
                                fontSize: '12px',
                                color: colors.hint,
                                marginTop: '2px'
                            }}>
                                {strictMode ? tr('interests.mode_strict_desc', lang) : tr('interests.mode_ai_desc', lang)}
                            </div>
                        </div>

                        {/* Recommendations */}
                        {!isEditingInterests && recommendations.length > 0 && (
                            <div style={{ marginTop: '32px' }}>
                                <h3 style={{
                                    fontSize: '16px',
                                    fontWeight: '600',
                                    marginBottom: '4px'
                                }}>
                                    {tr('recommendations.title', lang)}
                                </h3>
                                <p style={{
                                    fontSize: '13px',
                                    color: colors.hint,
                                    marginBottom: '12px'
                                }}>
                                    {tr('recommendations.subtitle', lang)}
                                </p>
                                <div style={{
                                    display: 'flex',
                                    flexDirection: 'column',
                                    gap: '10px'
                                }}>
                                    {recommendations.map(rec => (
                                        <div
                                            key={rec.url}
                                            style={{
                                                padding: '12px 16px',
                                                background: colors.secondaryBg,
                                                borderRadius: '12px',
                                                border: `1px solid ${colors.button}40`,
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center'
                                            }}
                                        >
                                            <div>
                                                <div style={{
                                                    fontSize: '15px',
                                                    fontWeight: '500'
                                                }}>
                                                    {rec.name}
                                                </div>
                                                <div style={{
                                                    fontSize: '12px',
                                                    color: colors.hint,
                                                    marginTop: '2px'
                                                }}>
                                                    {tr(
                                                        rec.peerCount > 1
                                                            ? 'recommendations.peers_plural'
                                                            : 'recommendations.peers',
                                                        lang,
                                                        { count: rec.peerCount }
                                                    )}
                                                </div>
                                            </div>
                                            <button
                                                onClick={() => handleAddSource(rec.url)}
                                                style={{
                                                    background: colors.button,
                                                    color: colors.buttonText,
                                                    border: 'none',
                                                    padding: '6px 14px',
                                                    borderRadius: '16px',
                                                    fontSize: '13px',
                                                    fontWeight: '600',
                                                    cursor: 'pointer'
                                                }}
                                            >
                                                {tr('recommendations.add', lang)}
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* ── Sources Tab ── */}
                {activeTab === 'sources' && (
                    <div>
                        {/* Strict mode toggle */}
                        <div style={{
                            background: colors.secondaryBg,
                            padding: '16px',
                            borderRadius: '12px',
                            marginBottom: '20px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            border: `1px solid ${colors.hint}40`
                        }}>
                            <div>
                                <h4 style={{
                                    margin: '0 0 4px 0',
                                    fontSize: '15px'
                                }}>{tr('sources.strict_mode', lang)}</h4>
                                <span style={{
                                    fontSize: '13px',
                                    color: colors.hint
                                }}>{tr('sources.strict_mode_desc', lang)}</span>
                            </div>
                            <div onClick={toggleStrictMode} style={{
                                width: '46px',
                                height: '26px',
                                background: strictMode ? colors.success : colors.hint,
                                borderRadius: '13px',
                                position: 'relative',
                                cursor: 'pointer'
                            }}>
                                <div style={{
                                    width: '22px',
                                    height: '22px',
                                    background: '#fff',
                                    borderRadius: '50%',
                                    position: 'absolute',
                                    top: '2px',
                                    left: strictMode ? '22px' : '2px',
                                    transition: 'left 0.3s'
                                }}/>
                            </div>
                        </div>

                        {/* Source list */}
                        <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '16px'
                        }}>
                            <h3 style={{
                                margin: 0,
                                fontSize: '17px',
                                fontWeight: '600'
                            }}>{tr('sources.title', lang)}</h3>
                            <button onClick={() => handleAddSource()} style={{
                                padding: '8px 16px',
                                background: colors.button,
                                color: colors.buttonText,
                                border: 'none',
                                borderRadius: '8px',
                                fontWeight: '600',
                                cursor: 'pointer'
                            }}>
                                {tr('sources.add_button', lang)}
                            </button>
                        </div>

                        {sources.length === 0 ? (
                            <div style={{
                                textAlign: 'center',
                                color: colors.hint,
                                padding: '32px 0'
                            }}>{tr('sources.empty', lang)}</div>
                        ) : (
                            <div style={{display: 'flex', flexDirection: 'column', gap: '12px'}}>
                                {sources.map((source, i) => (
                                    <div key={source.id} style={{
                                        padding: '14px',
                                        background: colors.secondaryBg,
                                        borderRadius: '12px',
                                        border: `1px solid ${colors.hint}20`
                                    }}>
                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center',
                                            marginBottom: '12px'
                                        }}>
                                            <span style={{
                                                fontSize: '15px',
                                                wordBreak: 'break-all',
                                                fontWeight: '500'
                                            }}>📡 {source.url}</span>
                                            <button onClick={() => handleRemoveSource(source.url, i)} style={{
                                                background: 'transparent',
                                                border: 'none',
                                                color: colors.danger,
                                                fontSize: '18px',
                                                fontWeight: 'bold',
                                                cursor: 'pointer'
                                            }}>✕
                                            </button>
                                        </div>
                                        <div style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center',
                                            borderTop: `1px solid ${colors.hint}20`,
                                            paddingTop: '12px'
                                        }}>
                                            <span style={{
                                                fontSize: '14px',
                                                color: colors.hint
                                            }}>{tr('sources.bypass_ai', lang)}</span>
                                            <div onClick={() => toggleReadAll(source.id, i)} style={{
                                                width: '40px',
                                                height: '22px',
                                                background: source.isReadAll ? colors.button : colors.hint,
                                                borderRadius: '11px',
                                                position: 'relative',
                                                cursor: 'pointer'
                                            }}>
                                                <div style={{
                                                    width: '18px',
                                                    height: '18px',
                                                    background: '#fff',
                                                    borderRadius: '50%',
                                                    position: 'absolute',
                                                    top: '2px',
                                                    left: source.isReadAll ? '20px' : '2px',
                                                    transition: 'left 0.3s'
                                                }}/>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {/* ── Insights Tab ── */}
                {activeTab === 'insights' && (
                    <div>
                        <h3 style={{
                            fontSize: '18px',
                            fontWeight: '700',
                            marginBottom: '16px'
                        }}>{tr('insights.title', lang)}</h3>

                        {/* DAU chart */}
                        <div style={{
                            background: colors.secondaryBg,
                            padding: '20px 16px',
                            borderRadius: '16px',
                            marginBottom: '24px',
                            border: `1px solid ${colors.hint}20`
                        }}>
                            <h4 style={{margin: '0 0 4px 0', fontSize: '15px'}}>{tr('insights.dau_title', lang)}</h4>
                            <p style={{
                                margin: '0 0 20px 0',
                                fontSize: '12px',
                                color: colors.hint
                            }}>{tr('insights.dau_desc', lang)}</p>
                            <div style={{
                                display: 'flex',
                                alignItems: 'flex-end',
                                justifyContent: 'space-between',
                                height: '140px',
                                paddingTop: '10px'
                            }}>
                                {dauStats.length > 0 ? dauStats.map((d, index) => {
                                    const heightPercent = Math.max((d.count / maxDau) * 100, 5);
                                    const shortDate = new Date(d.date).toLocaleDateString('en-US', {
                                        month: 'short',
                                        day: 'numeric'
                                    });
                                    return (
                                        <div key={index} style={{
                                            display: 'flex',
                                            flexDirection: 'column',
                                            alignItems: 'center',
                                            flex: 1
                                        }}>
                                            <span style={{
                                                fontSize: '11px',
                                                fontWeight: 'bold',
                                                marginBottom: '4px',
                                                color: colors.text
                                            }}>{d.count}</span>
                                            <div style={{
                                                width: '28px',
                                                height: `${heightPercent}px`,
                                                background: `linear-gradient(180deg, ${colors.chartBar} 0%, ${colors.chartBar}80 100%)`,
                                                borderRadius: '6px 6px 0 0',
                                                transition: 'height 0.5s ease-out'
                                            }}/>
                                            <span style={{
                                                fontSize: '10px',
                                                color: colors.hint,
                                                marginTop: '8px',
                                                transform: 'rotate(-45deg)',
                                                whiteSpace: 'nowrap'
                                            }}>{shortDate}</span>
                                        </div>
                                    );
                                }) : (
                                    <div style={{
                                        width: '100%',
                                        textAlign: 'center',
                                        color: colors.hint,
                                        alignSelf: 'center'
                                    }}>{tr('insights.no_data', lang)}</div>
                                )}
                            </div>
                        </div>

                        {/* Top sources */}
                        <div style={{
                            background: colors.secondaryBg,
                            padding: '20px 16px',
                            borderRadius: '16px',
                            border: `1px solid ${colors.hint}20`
                        }}>
                            <h4 style={{margin: '0 0 16px 0', fontSize: '15px'}}>{tr('insights.top_sources', lang)}</h4>
                            {topSources.length > 0 ? (
                                <div style={{display: 'flex', flexDirection: 'column', gap: '12px'}}>
                                    {topSources.map((source, index) => (
                                        <div key={index} style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'space-between'
                                        }}>
                                            <div style={{display: 'flex', alignItems: 'center', gap: '12px'}}>
                                                <div style={{
                                                    width: '24px',
                                                    height: '24px',
                                                    borderRadius: '50%',
                                                    background: index < 3 ? colors.chartBar : `${colors.hint}40`,
                                                    color: index < 3 ? '#fff' : colors.text,
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    fontSize: '12px',
                                                    fontWeight: 'bold'
                                                }}>
                                                    {index + 1}
                                                </div>
                                                <span style={{fontSize: '14px', fontWeight: '500'}}>{source.name}</span>
                                            </div>
                                            <span style={{fontSize: '13px', color: colors.hint, fontWeight: '600'}}>
                                                {source.subscriberCount} <span
                                                style={{fontSize: '11px'}}>{tr('insights.users', lang)}</span>
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div style={{
                                    textAlign: 'center',
                                    color: colors.hint
                                }}>{tr('insights.no_sources', lang)}</div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

// ─── TabButton ────────────────────────────────────────────────────────────────

function TabButton({active, onClick, children, colors}: {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
    colors: any;
}) {
    return (
        <button onClick={onClick} style={{
            flex: 1, padding: '14px 4px', background: 'transparent', border: 'none',
            borderBottom: active ? `3px solid ${colors.button}` : '3px solid transparent',
            color: active ? colors.button : colors.hint, fontSize: '15px', fontWeight: '600',
            cursor: 'pointer', transition: 'all 0.2s', display: 'flex', justifyContent: 'center', alignItems: 'center',
        }}>
            {children}
        </button>
    );
}