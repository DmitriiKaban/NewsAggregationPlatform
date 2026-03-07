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
                showPopup?: (params: { message: string; buttons?: any[] }, callback?: (buttonId: string) => void) => void;
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

interface Source {
    id: number;
    url: string;
    isReadAll: boolean;
    name?: string;
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

const showMessage = (message: string) => {
    const tg = window.Telegram?.WebApp;
    if (tg?.showAlert) tg.showAlert(message);
    else if (tg?.showPopup) tg.showPopup({message});
    else alert(message);
};

const getAvatarColor = (str: string) => {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash % 360);
    return `hsl(${hue}, 70%, 60%)`;
};

const getHandle = (url: string) => {
    if (!url) return '';
    const parts = url.split('/');
    const handle = parts[parts.length - 1];
    return handle.startsWith('@') ? handle : '@' + handle;
};

const getValidUrl = (url: string) => {
    if (!url) return '#';
    if (url.startsWith('http://') || url.startsWith('https://')) return url;
    const cleanHandle = url.startsWith('@') ? url.substring(1) : url;
    return `https://t.me/${cleanHandle}`;
};

const getInitials = (name: string) => {
    if (!name) return 'NN';
    const cleanName = name.startsWith('@') ? name.substring(1) : name;
    return cleanName ? cleanName.substring(0, 2).toUpperCase() : 'NN';
};

// Robust component that safely bypasses ngrok warnings by fetching the URL first
const ChannelAvatar = ({ url, name, size = 48, fontSize = 18, apiBaseUrl }: { url: string, name?: string, size?: number, fontSize?: number, apiBaseUrl: string }) => {
    const [imgUrl, setImgUrl] = useState<string | null>(null);
    const [imgError, setImgError] = useState(false);
    const handle = getHandle(url).replace('@', '');

    useEffect(() => {
        if (!handle) return;
        
        fetch(`${apiBaseUrl}/sources/avatar?handle=${handle}`, {
            headers: {"ngrok-skip-browser-warning": "69420"}
        })
        .then(r => {
            if (!r.ok) throw new Error();
            return r.json();
        })
        .then(data => {
            if (data && data.url) {
                setImgUrl(data.url);
            }
        })
        .catch(() => setImgError(true));
    }, [handle, apiBaseUrl]);

    if (imgUrl && !imgError) {
        return (
            <img
                src={imgUrl}
                alt={name || handle}
                style={{ width: size, height: size, borderRadius: '50%', objectFit: 'cover', flexShrink: 0, backgroundColor: '#f0f0f0' }}
                onError={() => setImgError(true)}
            />
        );
    }

    return (
        <div style={{ width: size, height: size, borderRadius: '50%', background: getAvatarColor(name || url), color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: fontSize, fontWeight: '700', flexShrink: 0 }}>
            {getInitials(name || url)}
        </div>
    );
};

export default function App() {
    const [lang, setLang] = useState<Language>('en');
    const [originalInterests, setOriginalInterests] = useState('');
    const [interestTags, setInterestTags] = useState<string[]>([]);
    const [tagInput, setTagInput] = useState('');
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

    const apiBaseUrl = "https://20ec-212-28-65-233.ngrok-free.app/api";
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
                const backendLang = profileData.language?.toLowerCase();

                if (backendLang && ['en', 'ro', 'ru'].includes(backendLang)) {
                    setLang(backendLang as Language);
                }
                setLanguageLoaded(true);

                if (profileData.firstName?.trim()) setDisplayName(profileData.firstName);

                const interestsStr = Array.isArray(profileData.interests)
                    ? profileData.interests.join(', ')
                    : (profileData.interests || '');
                setOriginalInterests(interestsStr);
                
                setInterestTags(interestsStr.split(',').map((s: string) => s.trim()).filter(Boolean));
                
                setStrictMode(profileData.strictSourceFiltering || false);

                if (Array.isArray(profileData.sources)) {
                    setSources(
                        profileData.sources
                            .map((s: any) => ({
                                id: s.id, 
                                url: s.url || '', 
                                name: s.name || getHandle(s.url || ''),
                                isReadAll: s.isReadAll || false
                            }))
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

    useEffect(() => {
        if (!isTelegramEnvironment) return;
        const handleBack = () => {
            if (isEditingInterests) {
                setIsEditingInterests(false);
                setInterestTags(originalInterests.split(',').map((s: string) => s.trim()).filter(Boolean));
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

    useEffect(() => {
        if (!isTelegramEnvironment) return;

        const handleSaveInterests = async () => {
            if (!user?.id) return;
            
            if (!backendReachable) {
                showMessage(tr('error.update_settings', lang) || "Cannot save: Backend is unreachable.");
                return;
            }

            try {
                tg?.MainButton.showProgress?.();
                const stringToSave = interestTags.join(', ');
                
                const response = await fetch(`${apiBaseUrl}/users/${user.id}/interests`, {
                    method: 'POST',
                    headers: {"ngrok-skip-browser-warning": "69420", "Content-Type": "application/json"},
                    body: JSON.stringify({interest: stringToSave}),
                });
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => null);
                    throw new Error(errorData?.message || `HTTP ${response.status}`);
                }
                
                tg?.MainButton.hideProgress?.();
                setOriginalInterests(stringToSave);
                setIsEditingInterests(false);
                showMessage(tr('interests.saved', lang));
            } catch (err: any) {
                tg?.MainButton.hideProgress?.();
                showMessage(`Failed: ${err.message}`);
            }
        };

        if (activeTab === 'interests' && isEditingInterests) {
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
    }, [activeTab, isEditingInterests, interestTags, user?.id, backendReachable, apiBaseUrl, isTelegramEnvironment, lang]);

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
                        url: s.url || '',
                        name: s.name || getHandle(s.url || ''),
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

    if (loading || !languageLoaded) return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        }}>
            <div style={{textAlign: 'center', color: colors.text}}>
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ animation: 'spin 2s linear infinite', marginBottom: '16px' }}>
                    <line x1="12" y1="2" x2="12" y2="6"></line>
                    <line x1="12" y1="18" x2="12" y2="22"></line>
                    <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
                    <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
                    <line x1="2" y1="12" x2="6" y2="12"></line>
                    <line x1="18" y1="12" x2="22" y2="12"></line>
                    <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
                    <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
                </svg>
                <div style={{fontSize: '16px', fontWeight: '500'}}>{tr('loading', lang)}</div>
                <style>{`@keyframes spin { 100% { transform: rotate(360deg); } }`}</style>
            </div>
        </div>
    );

    if (!user) return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '24px',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        }}>
            <div style={{textAlign: 'center', maxWidth: '400px', color: colors.text}}>
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke={colors.danger} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{marginBottom: '24px'}}>
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="15" y1="9" x2="9" y2="15"></line>
                    <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
                <h2 style={{fontSize: '24px', fontWeight: '700', margin: '0 0 12px 0'}}>{tr('error.user_not_found', lang)}</h2>
                <p style={{fontSize: '15px', color: colors.hint, lineHeight: 1.5, margin: '0 0 24px 0'}}>{tr('error.open_from_telegram', lang)}</p>
                {error && (
                    <div style={{padding: '16px', background: `${colors.danger}15`, borderRadius: '12px', fontSize: '13px', color: colors.danger, textAlign: 'left', border: `1px solid ${colors.danger}30`}}>
                        <strong>Error:</strong> {error}
                    </div>
                )}
            </div>
        </div>
    );

    return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            color: colors.text,
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            paddingBottom: '40px'
        }}>
            <div style={{padding: '24px 20px 20px 20px'}}>
                <h1 style={{fontSize: '28px', fontWeight: '800', margin: '0 0 6px 0', color: colors.text, letterSpacing: '-0.5px'}}>
                    {tr('header.greeting', lang, {name: displayName})}
                </h1>
                <p style={{fontSize: '15px', color: colors.hint, margin: 0, fontWeight: '500'}}>{tr('header.subtitle', lang)}</p>
            </div>

            <div style={{
                display: 'flex', 
                padding: '0 16px',
                marginBottom: '20px',
                position: 'sticky',
                top: 0,
                background: colors.bg,
                zIndex: 10,
                paddingTop: '10px',
                paddingBottom: '10px'
            }}>
                <div style={{
                    display: 'flex',
                    width: '100%',
                    background: colors.secondaryBg,
                    borderRadius: '16px',
                    padding: '4px'
                }}>
                    <TabButton active={activeTab === 'interests'} onClick={() => { setActiveTab('interests'); setIsEditingInterests(false); }} colors={colors}>
                        {tr('tab.interests', lang)}
                    </TabButton>
                    <TabButton active={activeTab === 'sources'} onClick={() => { setActiveTab('sources'); setIsEditingInterests(false); }} colors={colors}>
                        {tr('tab.sources', lang)}
                    </TabButton>
                    <TabButton active={activeTab === 'insights'} onClick={() => { setActiveTab('insights'); setIsEditingInterests(false); }} colors={colors}>
                        {tr('tab.insights', lang)}
                    </TabButton>
                </div>
            </div>

            <div style={{padding: '0 20px'}}>
                {activeTab === 'interests' && (
                    <div style={{animation: 'fadeIn 0.3s ease-in-out'}}>
                        <label style={{display: 'flex', alignItems: 'center', gap: '8px', fontSize: '16px', fontWeight: '700', marginBottom: '16px', color: colors.text}}>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path><line x1="7" y1="7" x2="7.01" y2="7"></line></svg>
                            {tr('interests.title', lang)}
                        </label>

                        <div onClick={() => !isEditingInterests && setIsEditingInterests(true)} style={{cursor: !isEditingInterests ? 'pointer' : 'default'}}>
                            {!isEditingInterests ? (
                                <div style={{
                                    padding: '20px',
                                    background: colors.secondaryBg,
                                    borderRadius: '20px',
                                    border: `1px solid ${colors.hint}20`,
                                    minHeight: '100px',
                                    display: 'flex',
                                    flexWrap: 'wrap',
                                    gap: '10px',
                                    alignItems: 'flex-start',
                                    boxShadow: '0 4px 20px rgba(0,0,0,0.03)',
                                    position: 'relative',
                                    overflow: 'hidden'
                                }}>
                                    {interestTags.length > 0 ? (
                                        interestTags.map((tag, i) => (
                                            <span key={i} style={{
                                                background: colors.bg,
                                                border: `1px solid ${colors.button}30`,
                                                color: colors.text,
                                                padding: '8px 16px',
                                                borderRadius: '20px',
                                                fontSize: '14px',
                                                fontWeight: '600',
                                                boxShadow: '0 2px 8px rgba(0,0,0,0.02)'
                                            }}>{tag}</span>
                                        ))
                                    ) : (
                                        <span style={{ color: colors.hint, fontStyle: 'italic', display: 'flex', alignItems: 'center', fontWeight: '500' }}>
                                            {tr('interests.placeholder', lang)}
                                        </span>
                                    )}
                                    <div style={{ position: 'absolute', right: '16px', bottom: '16px', background: colors.bg, padding: '8px', borderRadius: '50%', display: 'flex', boxShadow: '0 4px 12px rgba(0,0,0,0.05)'}}>
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                                    </div>
                                </div>
                            ) : (
                                <div>
                                    <div style={{
                                        display: 'flex',
                                        flexWrap: 'wrap',
                                        gap: '10px',
                                        padding: '16px',
                                        border: `2px solid ${colors.button}`,
                                        borderRadius: '20px',
                                        background: colors.bg,
                                        minHeight: '140px',
                                        alignItems: 'flex-start',
                                        boxShadow: `0 0 0 4px ${colors.button}15`
                                    }}>
                                        {interestTags.map((tag, i) => (
                                            <div key={i} style={{
                                                background: `${colors.button}15`,
                                                color: colors.button,
                                                padding: '6px 12px',
                                                borderRadius: '20px',
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '8px',
                                                fontSize: '14px',
                                                fontWeight: '600'
                                            }}>
                                                <span>{tag}</span>
                                                <button onClick={(e) => {
                                                    e.stopPropagation();
                                                    setInterestTags(interestTags.filter((_, index) => index !== i));
                                                }} style={{
                                                    background: 'transparent', border: 'none', color: colors.button, cursor: 'pointer', padding: '0', display: 'flex', alignItems: 'center', opacity: 0.7
                                                }}>
                                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                                                </button>
                                            </div>
                                        ))}
                                        <input
                                            type="text"
                                            value={tagInput}
                                            onChange={e => {
                                                const value = e.target.value;
                                                if (value.includes(',')) {
                                                    const newTags = value.split(',').map(t => t.trim()).filter(Boolean);
                                                    if (newTags.length > 0) {
                                                        setInterestTags(prev => {
                                                            const tagsSet = new Set(prev);
                                                            newTags.forEach(t => tagsSet.add(t));
                                                            return Array.from(tagsSet);
                                                        });
                                                    }
                                                    setTagInput('');
                                                } else {
                                                    setTagInput(value);
                                                }
                                            }}
                                            onKeyDown={e => {
                                                if (e.key === 'Enter') {
                                                    e.preventDefault();
                                                    const newTag = tagInput.trim();
                                                    if (newTag && !interestTags.includes(newTag)) {
                                                        setInterestTags([...interestTags, newTag]);
                                                        setTagInput('');
                                                    }
                                                } else if (e.key === 'Backspace' && tagInput === '' && interestTags.length > 0) {
                                                    setInterestTags(interestTags.slice(0, -1));
                                                }
                                            }}
                                            placeholder={interestTags.length === 0 ? tr('interests.input_placeholder', lang) : "Type and press Enter..."}
                                            autoFocus
                                            style={{
                                                flex: 1, minWidth: '150px', border: 'none', outline: 'none', background: 'transparent', color: colors.text, fontSize: '15px', padding: '8px 0', fontFamily: 'inherit', fontWeight: '500'
                                            }}
                                        />
                                    </div>
                                    <p style={{ fontSize: '13px', color: colors.hint, marginTop: '12px', fontWeight: '500', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                                        {tr('interests.hint', lang) || 'Press Enter or comma to add a new tag'}
                                    </p>
                                </div>
                            )}
                        </div>

                        <div style={{
                            marginTop: '24px',
                            padding: '16px',
                            background: strictMode ? `${colors.danger}10` : `${colors.button}10`,
                            borderRadius: '16px',
                            border: `1px solid ${strictMode ? colors.danger : colors.button}20`,
                            display: 'flex',
                            gap: '12px',
                            alignItems: 'flex-start'
                        }}>
                            <div style={{marginTop: '2px'}}>
                                {strictMode ? (
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={colors.danger} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                                ) : (
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M12 16v-4"></path><path d="M12 8h.01"></path></svg>
                                )}
                            </div>
                            <div>
                                <div style={{ fontSize: '14px', fontWeight: '700', color: strictMode ? colors.danger : colors.button }}>
                                    {strictMode ? tr('interests.mode_strict', lang) : tr('interests.mode_ai', lang)}
                                </div>
                                <div style={{ fontSize: '13px', color: colors.hint, marginTop: '4px', lineHeight: 1.4 }}>
                                    {strictMode ? tr('interests.mode_strict_desc', lang) : tr('interests.mode_ai_desc', lang)}
                                </div>
                            </div>
                        </div>

                        {!isEditingInterests && recommendations.length > 0 && (
                            <div style={{ marginTop: '36px' }}>
                                <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '6px' }}>
                                    {tr('recommendations.title', lang)}
                                </h3>
                                <p style={{ fontSize: '14px', color: colors.hint, marginBottom: '16px', fontWeight: '500' }}>
                                    {tr('recommendations.subtitle', lang)}
                                </p>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                    {recommendations.map(rec => (
                                        <div key={rec.url} style={{
                                            padding: '16px', background: colors.secondaryBg, borderRadius: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: `1px solid ${colors.hint}20`
                                        }}>
                                            <div style={{display: 'flex', alignItems: 'center', gap: '12px', overflow: 'hidden'}}>
                                                <ChannelAvatar url={rec.url} name={rec.name} size={42} fontSize={16} apiBaseUrl={apiBaseUrl} />
                                                <div style={{overflow: 'hidden'}}>
                                                    <div style={{ fontSize: '15px', fontWeight: '600', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{rec.name}</div>
                                                    <div style={{ fontSize: '13px', color: colors.hint, marginTop: '2px', fontWeight: '500' }}>
                                                        {tr(rec.peerCount > 1 ? 'recommendations.peers_plural' : 'recommendations.peers', lang, { count: rec.peerCount })}
                                                    </div>
                                                </div>
                                            </div>
                                            <button onClick={() => handleAddSource(rec.url)} style={{
                                                background: colors.button, color: colors.buttonText, border: 'none', padding: '8px 16px', borderRadius: '14px', fontSize: '14px', fontWeight: '700', cursor: 'pointer', flexShrink: 0
                                            }}>
                                                {tr('recommendations.add', lang)}
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'sources' && (
                    <div style={{animation: 'fadeIn 0.3s ease-in-out'}}>
                        <div style={{
                            background: colors.secondaryBg, padding: '20px', borderRadius: '20px', marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: `1px solid ${colors.hint}20`
                        }}>
                            <div>
                                <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: '700' }}>{tr('sources.strict_mode', lang)}</h4>
                                <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '500' }}>{tr('sources.strict_mode_desc', lang)}</span>
                            </div>
                            <div onClick={toggleStrictMode} style={{
                                width: '52px', height: '30px', background: strictMode ? colors.success : colors.hint, borderRadius: '15px', position: 'relative', cursor: 'pointer', flexShrink: 0, transition: 'background 0.3s'
                            }}>
                                <div style={{
                                    width: '26px', height: '26px', background: '#fff', borderRadius: '50%', position: 'absolute', top: '2px', left: strictMode ? '24px' : '2px', transition: 'left 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)', boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                                }}/>
                            </div>
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                            <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '800' }}>{tr('sources.title', lang)}</h3>
                            <button onClick={() => handleAddSource()} style={{
                                padding: '10px 16px', background: colors.button, color: colors.buttonText, border: 'none', borderRadius: '14px', fontSize: '14px', fontWeight: '700', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px'
                            }}>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                                {tr('sources.add_button', lang)}
                            </button>
                        </div>

                        {sources.length === 0 ? (
                            <div style={{ textAlign: 'center', color: colors.hint, padding: '40px 0', fontWeight: '500' }}>
                                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{opacity: 0.5, marginBottom: '12px'}}><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                                <div>{tr('sources.empty', lang)}</div>
                            </div>
                        ) : (
                            <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                                {sources.map((source, i) => (
                                    <div key={source.id} style={{
                                        padding: '16px', background: colors.secondaryBg, borderRadius: '20px', border: `1px solid ${colors.hint}20`, display: 'flex', flexDirection: 'column', gap: '16px'
                                    }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', overflow: 'hidden' }}>
                                                <ChannelAvatar url={source.url} name={source.name} size={48} fontSize={18} apiBaseUrl={apiBaseUrl} />
                                                <div style={{overflow: 'hidden'}}>
                                                    <div style={{ fontSize: '16px', fontWeight: '700', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                                        {source.name || getHandle(source.url)}
                                                    </div>
                                                    <a href={getValidUrl(source.url)} target="_blank" rel="noopener noreferrer" style={{ fontSize: '13px', color: colors.link, textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '4px', marginTop: '4px', fontWeight: '500' }}>
                                                        {getHandle(source.url)}
                                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
                                                    </a>
                                                </div>
                                            </div>
                                            <button onClick={() => handleRemoveSource(source.url, i)} style={{
                                                background: `${colors.danger}15`, border: 'none', color: colors.danger, width: '36px', height: '36px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0
                                            }}>
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                                            </button>
                                        </div>
                                        <div style={{
                                            display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: `1px solid ${colors.hint}20`, paddingTop: '16px'
                                        }}>
                                            <span style={{ fontSize: '14px', color: colors.text, fontWeight: '600', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>
                                                {tr('sources.bypass_ai', lang)}
                                            </span>
                                            <div onClick={() => toggleReadAll(source.id, i)} style={{
                                                width: '46px', height: '26px', background: source.isReadAll ? colors.button : colors.hint, borderRadius: '13px', position: 'relative', cursor: 'pointer', transition: 'background 0.3s'
                                            }}>
                                                <div style={{
                                                    width: '22px', height: '22px', background: '#fff', borderRadius: '50%', position: 'absolute', top: '2px', left: source.isReadAll ? '22px' : '2px', transition: 'left 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)', boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                                                }}/>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'insights' && (
                    <div style={{animation: 'fadeIn 0.3s ease-in-out'}}>
                        <h3 style={{ fontSize: '20px', fontWeight: '800', marginBottom: '20px' }}>{tr('insights.title', lang)}</h3>

                        <div style={{
                            background: colors.secondaryBg, padding: '24px 20px', borderRadius: '24px', marginBottom: '24px', border: `1px solid ${colors.hint}20`
                        }}>
                            <h4 style={{margin: '0 0 6px 0', fontSize: '16px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px'}}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.chartBar} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                                {tr('insights.dau_title', lang)}
                            </h4>
                            <p style={{ margin: '0 0 24px 0', fontSize: '13px', color: colors.hint, fontWeight: '500' }}>{tr('insights.dau_desc', lang)}</p>
                            
                            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', height: '160px', paddingTop: '10px' }}>
                                {dauStats.length > 0 ? dauStats.map((d, index) => {
                                    const heightPercent = Math.max((d.count / maxDau) * 100, 8);
                                    const shortDate = new Date(d.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                                    return (
                                        <div key={index} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flex: 1 }}>
                                            <span style={{ fontSize: '12px', fontWeight: '800', marginBottom: '6px', color: colors.text }}>{d.count}</span>
                                            <div style={{
                                                width: '32px', height: `${heightPercent}px`, background: `linear-gradient(180deg, ${colors.chartBar} 0%, ${colors.chartBar}40 100%)`, borderRadius: '8px 8px 0 0', transition: 'height 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275)'
                                            }}/>
                                            <span style={{ fontSize: '11px', color: colors.hint, marginTop: '10px', transform: 'rotate(-45deg)', whiteSpace: 'nowrap', fontWeight: '600' }}>{shortDate}</span>
                                        </div>
                                    );
                                }) : (
                                    <div style={{ width: '100%', textAlign: 'center', color: colors.hint, alignSelf: 'center', fontWeight: '500' }}>{tr('insights.no_data', lang)}</div>
                                )}
                            </div>
                        </div>

                        <div style={{
                            background: colors.secondaryBg, padding: '24px 20px', borderRadius: '24px', border: `1px solid ${colors.hint}20`
                        }}>
                            <h4 style={{margin: '0 0 20px 0', fontSize: '16px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px'}}>
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                                {tr('insights.top_sources', lang)}
                            </h4>
                            {topSources.length > 0 ? (
                                <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                                    {topSources.map((source, index) => (
                                        <div key={index} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                            <div style={{display: 'flex', alignItems: 'center', gap: '14px', overflow: 'hidden'}}>
                                                <ChannelAvatar url={source.name} name={source.name} size={28} fontSize={13} apiBaseUrl={apiBaseUrl} />
                                                <span style={{fontSize: '15px', fontWeight: '600', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>{source.name}</span>
                                            </div>
                                            <span style={{fontSize: '14px', color: colors.text, fontWeight: '700', flexShrink: 0}}>
                                                {source.subscriberCount} <span style={{fontSize: '12px', color: colors.hint, fontWeight: '500'}}>{tr('insights.users', lang)}</span>
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div style={{ textAlign: 'center', color: colors.hint, fontWeight: '500' }}>{tr('insights.no_sources', lang)}</div>
                            )}
                        </div>
                    </div>
                )}
            </div>
            <style>{`
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
            `}</style>
        </div>
    );
}

function TabButton({active, onClick, children, colors}: { active: boolean; onClick: () => void; children: React.ReactNode; colors: any; }) {
    return (
        <button onClick={onClick} style={{
            flex: 1, padding: '10px 4px', background: active ? colors.bg : 'transparent', border: 'none',
            borderRadius: '12px', color: active ? colors.text : colors.hint, fontSize: '14px', fontWeight: '700',
            cursor: 'pointer', transition: 'all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275)', display: 'flex', justifyContent: 'center', alignItems: 'center',
            boxShadow: active ? '0 2px 8px rgba(0,0,0,0.05)' : 'none'
        }}>
            {children}
        </button>
    );
}