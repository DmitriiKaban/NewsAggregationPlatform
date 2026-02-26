import {useEffect, useState} from 'react';

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

interface Source {
    id: number;
    url: string;
    isReadAll: boolean;
}

// --- NEW INTERFACES FOR ANALYTICS ---
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

// ------------------------------------

const showMessage = (message: string) => {
    const tg = window.Telegram?.WebApp;
    if (tg?.showAlert) {
        tg.showAlert(message);
    } else if (tg?.showPopup) {
        tg.showPopup({message});
    } else {
        console.log('📢 Message:', message);
    }
};

export default function App() {
    const [interests, setInterests] = useState('');
    const [originalInterests, setOriginalInterests] = useState('');
    const [isEditingInterests, setIsEditingInterests] = useState(false);
    const [sources, setSources] = useState<Source[]>([]);
    const [strictMode, setStrictMode] = useState(false);

    // NEW STATE FOR ANALYTICS
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [topSources, setTopSources] = useState<TopSource[]>([]);
    const [dauStats, setDauStats] = useState<DauData[]>([]);

    // ADDED 'insights' tab
    const [activeTab, setActiveTab] = useState<'interests' | 'sources' | 'insights'>('interests');

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [backendReachable, setBackendReachable] = useState(true);
    const [isTelegramEnvironment, setIsTelegramEnvironment] = useState(false);
    const [user, setUser] = useState<{ id?: number; first_name?: string; username?: string } | null>(null);
    const [displayName, setDisplayName] = useState('there');

    // Centralized API Base URL
    const apiBaseUrl = "https://donny-subevergreen-agreeably.ngrok-free.dev/api";

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
        chartBar: '#FF9500' // Beautiful orange for the chart
    };

    useEffect(() => {
        if (tg) {
            setIsTelegramEnvironment(true);
            try {
                tg.ready();
                tg.expand();
                const userData = tg.initDataUnsafe?.user;
                if (userData && userData.id) {
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
            } catch (err) {
                setUser(null);
                setError("Failed to initialize");
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
            headers: {
                "ngrok-skip-browser-warning": "69420",
                "Content-Type": "application/json",
            },
            signal: controller.signal
        };

        Promise.all([
            fetch(`${apiBaseUrl}/users/${user.id}/profile`, fetchOptions).then(res => res.json()),
            fetch(`${apiBaseUrl}/analytics/users/${user.id}/recommendations`, fetchOptions).then(res => res.json()).catch(() => []),
            fetch(`${apiBaseUrl}/analytics/insights`, fetchOptions).then(res => res.json()).catch(err => {
                console.error("❌ Failed to fetch insights:", err);
                return {topSources: [], dauStats: []};
            })
        ])
            .then(([profileData, recommendationsData, insightsData]) => {
                console.log("📊 Insights data received:", insightsData);
                console.log("📊 DAU Stats:", insightsData.dauStats);
                console.log("📊 Top Sources:", insightsData.topSources);
                if (profileData.firstName && profileData.firstName.trim()) setDisplayName(profileData.firstName);

                const interestsStr = Array.isArray(profileData.interests) ? profileData.interests.join(', ') : (profileData.interests || '');
                setInterests(interestsStr);
                setOriginalInterests(interestsStr);
                setStrictMode(profileData.strictSourceFiltering || false);

                if (Array.isArray(profileData.sources)) {
                    setSources(profileData.sources.map((s: any) => ({
                        id: s.id, url: s.url || s.name || '', isReadAll: s.isReadAll || false
                    })).filter((s: Source) => s.url));
                }

                setRecommendations(recommendationsData);
                setDauStats(insightsData.dauStats || []);
                setTopSources(insightsData.topSources || []);

                console.log("✅ State updated - dauStats:", insightsData.dauStats);

                setLoading(false);
                setError(null);
                setBackendReachable(true);
            })
            .catch(err => {
                if (err.name !== 'AbortError') {
                    setError('Cannot connect to backend');
                    setBackendReachable(false);
                }
                setLoading(false);
            });

        return () => controller.abort();
    }, [user?.id, apiBaseUrl]);

    // Back button handling
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

    // Save Interests
    useEffect(() => {
        if (!isTelegramEnvironment) return;
        const handleSaveInterests = async () => {
            if (!user?.id || !backendReachable) return;
            try {
                tg?.MainButton.showProgress?.();
                const response = await fetch(`${apiBaseUrl}/users/${user.id}/interests`, {
                    method: 'POST',
                    headers: {"ngrok-skip-browser-warning": "69420", "Content-Type": "application/json"},
                    body: JSON.stringify({interest: interests})
                });
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                tg?.MainButton.hideProgress?.();
                setOriginalInterests(interests);
                setIsEditingInterests(false);
                showMessage('✅ Interests saved!');
            } catch (err: any) {
                tg?.MainButton.hideProgress?.();
                showMessage(`Failed: ${err.message}`);
            }
        };

        if (activeTab === 'interests' && isEditingInterests && interests.trim()) {
            if (tg?.MainButton) {
                tg.MainButton.setText('Save Interests');
                tg.MainButton.enable();
                tg.MainButton.show();
                tg.MainButton.onClick(handleSaveInterests);
                return () => tg.MainButton.offClick(handleSaveInterests);
            }
        } else {
            tg?.MainButton.hide();
        }
    }, [activeTab, isEditingInterests, interests, user?.id, backendReachable, apiBaseUrl, isTelegramEnvironment]);

    const toggleStrictMode = async () => {
        if (!backendReachable || !user?.id) return;
        const newState = !strictMode;
        setStrictMode(newState);
        try {
            const response = await fetch(`${apiBaseUrl}/users/${user.id}/settings/strict-filtering?enabled=${newState}`, {
                method: 'PUT', headers: {"ngrok-skip-browser-warning": "69420"}
            });
            if (!response.ok) throw new Error();
        } catch (e) {
            setStrictMode(!newState);
            showMessage('Failed to update settings');
        }
    };

    const toggleReadAll = async (sourceId: number, currentIndex: number) => {
        if (!backendReachable || !user?.id) return;
        const source = sources[currentIndex];
        const newState = !source.isReadAll;
        const updatedSources = [...sources];
        updatedSources[currentIndex].isReadAll = newState;
        setSources(updatedSources);

        try {
            const response = await fetch(`${apiBaseUrl}/users/${user.id}/sources/${sourceId}/read-all?readAll=${newState}`, {
                method: 'PUT', headers: {"ngrok-skip-browser-warning": "69420"}
            });
            if (!response.ok) throw new Error();
        } catch (e) {
            updatedSources[currentIndex].isReadAll = !newState;
            setSources([...updatedSources]);
            showMessage('Failed to update preference');
        }
    };

    // Refactored to accept an optional URL so it works with the "Recommend" buttons
    const handleAddSource = async (directUrl?: string) => {
        if (!backendReachable || !user?.id) return;

        const input = directUrl || prompt('Enter channel (e.g. durov or @channel):');
        if (!input) return;

        try {
            const response = await fetch(`${apiBaseUrl}/users/${user.id}/sources`, {
                method: 'POST',
                headers: {"ngrok-skip-browser-warning": "69420", "Content-Type": "application/json"},
                body: JSON.stringify({source: input})
            });

            if (!response.ok) throw new Error();

            // Refetch sources to get the ID generated by the database
            const profileResponse = await fetch(`${apiBaseUrl}/users/${user.id}/profile`, {
                headers: {"ngrok-skip-browser-warning": "69420"}
            });

            if (profileResponse.ok) {
                const data = await profileResponse.json();
                if (Array.isArray(data.sources)) {
                    setSources(data.sources.map((s: any) => ({
                        id: s.id,
                        url: s.url || s.name || '',
                        isReadAll: s.isReadAll || false
                    })).filter((s: Source) => s.url));
                }

                // If it was a recommendation, remove it from the recommended list
                if (directUrl) {
                    setRecommendations(recommendations.filter(r => r.url !== directUrl));
                }

                if (!directUrl) setActiveTab('sources');
                showMessage('✅ Source added!');
            }
        } catch (err: any) {
            showMessage(`Failed to add source.`);
        }
    };

    const handleRemoveSource = async (url: string, index: number) => {
        if (!backendReachable || !user?.id) return;
        try {
            const response = await fetch(`${apiBaseUrl}/users/${user.id}/sources?url=${encodeURIComponent(url)}`, {
                method: 'DELETE', headers: {"ngrok-skip-browser-warning": "69420"}
            });
            if (!response.ok) throw new Error();
            setSources(sources.filter((_, idx) => idx !== index));
        } catch (err: any) {
            showMessage(`Failed to remove.`);
        }
    };

    // Helper to calculate max graph height for DAU
    const maxDau = dauStats.length > 0 ? Math.max(...dauStats.map(d => d.count)) : 1;

    if (loading) return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        }}>
            <div style={{textAlign: 'center', color: colors.text}}>
                <div style={{fontSize: '64px', marginBottom: '24px'}}>📰</div>
                <div style={{fontSize: '18px', fontWeight: '500'}}>Loading...</div>
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
            padding: '20px',
            fontFamily: '-apple-system, sans-serif'
        }}>
            <div style={{textAlign: 'center', maxWidth: '400px', color: colors.text}}>
                <div style={{fontSize: '64px', marginBottom: '24px'}}>❌</div>
                <h2 style={{fontSize: '24px', fontWeight: '600', margin: '0 0 12px 0'}}>Could Not Find User</h2>
                <p style={{fontSize: '15px', color: colors.hint, lineHeight: 1.5, margin: '0 0 20px 0'}}>Please open
                    this app from Telegram.</p>

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

    return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            color: colors.text,
            fontFamily: '-apple-system, sans-serif',
            paddingBottom: '20px'
        }}>
            <div style={{padding: '20px', borderBottom: `1px solid ${colors.hint}40`}}>
                <h1 style={{
                    fontSize: '28px',
                    fontWeight: '700',
                    margin: '0 0 8px 0',
                    color: colors.text
                }}>Hey, {displayName}! 👋</h1>
                <p style={{fontSize: '15px', color: colors.hint, margin: 0}}>Your personalized news hub</p>
            </div>

            <div style={{display: 'flex', borderBottom: `2px solid ${colors.hint}20`, background: colors.secondaryBg}}>
                <TabButton active={activeTab === 'interests'} onClick={() => {
                    setActiveTab('interests');
                    setIsEditingInterests(false);
                }} colors={colors}>🎯 Interests</TabButton>
                <TabButton active={activeTab === 'sources'} onClick={() => {
                    setActiveTab('sources');
                    setIsEditingInterests(false);
                }} colors={colors}>📚 Sources</TabButton>
                <TabButton active={activeTab === 'insights'} onClick={() => {
                    setActiveTab('insights');
                    setIsEditingInterests(false);
                }} colors={colors}>📊 Insights</TabButton>
            </div>

            <div style={{padding: '20px'}}>
                {activeTab === 'interests' && (
                    <div>
                        {/* Interests Editor */}
                        <label style={{
                            display: 'block',
                            fontSize: '15px',
                            fontWeight: '600',
                            marginBottom: '12px',
                            color: colors.text
                        }}>Your Current Interests</label>
                        {!isEditingInterests ? (
                            <>
                                <div style={{
                                    padding: '16px',
                                    background: colors.secondaryBg,
                                    borderRadius: '12px',
                                    border: `1px solid ${colors.hint}20`,
                                    minHeight: '80px',
                                    fontSize: '15px'
                                }}>
                                    {originalInterests || <span style={{color: colors.hint, fontStyle: 'italic'}}>No interests set yet</span>}
                                </div>
                                <button onClick={() => setIsEditingInterests(true)} style={{
                                    marginTop: '16px',
                                    padding: '14px 24px',
                                    background: colors.button,
                                    color: colors.buttonText,
                                    border: 'none',
                                    borderRadius: '10px',
                                    fontSize: '15px',
                                    fontWeight: '600',
                                    width: '100%'
                                }}>
                                    ✏️ Update Interests
                                </button>
                            </>
                        ) : (
                            <>
                                <textarea value={interests} onChange={(e) => setInterests(e.target.value)}
                                          placeholder="AI, Crypto, Tech..." autoFocus style={{
                                    width: '100%',
                                    minHeight: '120px',
                                    padding: '14px',
                                    fontSize: '15px',
                                    border: `2px solid ${colors.button}`,
                                    borderRadius: '12px',
                                    background: colors.bg,
                                    color: colors.text
                                }}/>
                            </>
                        )}

                        {/* 🌟 NEW FEATURE: SMART RECOMMENDATIONS */}
                        {!isEditingInterests && recommendations.length > 0 && (
                            <div style={{marginTop: '32px'}}>
                                <h3 style={{
                                    fontSize: '16px',
                                    fontWeight: '600',
                                    marginBottom: '4px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '6px'
                                }}>
                                    ✨ Recommended for You
                                </h3>
                                <p style={{fontSize: '13px', color: colors.hint, marginBottom: '12px'}}>People with
                                    similar interests follow these:</p>

                                <div style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
                                    {recommendations.map((rec) => (
                                        <div key={rec.url} style={{
                                            padding: '12px 16px',
                                            background: colors.secondaryBg,
                                            borderRadius: '12px',
                                            border: `1px solid ${colors.button}40`,
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center'
                                        }}>
                                            <div>
                                                <div style={{fontSize: '15px', fontWeight: '500'}}>{rec.name}</div>
                                                <div style={{
                                                    fontSize: '12px',
                                                    color: colors.hint,
                                                    marginTop: '2px'
                                                }}>Followed by {rec.peerCount} peer{rec.peerCount > 1 ? 's' : ''}</div>
                                            </div>
                                            <button onClick={() => handleAddSource(rec.url)} style={{
                                                background: colors.button,
                                                color: colors.buttonText,
                                                border: 'none',
                                                padding: '6px 14px',
                                                borderRadius: '16px',
                                                fontSize: '13px',
                                                fontWeight: '600'
                                            }}>
                                                + Add
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'sources' && (
                    <div>
                        {/* Strict Mode Toggle (Same as before) */}
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
                                <h4 style={{margin: '0 0 4px 0', fontSize: '15px'}}>Strict Mode</h4>
                                <span style={{fontSize: '13px', color: colors.hint}}>Only show news from my added sources</span>
                            </div>
                            <div onClick={toggleStrictMode} style={{
                                width: '46px',
                                height: '26px',
                                background: strictMode ? colors.success : colors.hint,
                                borderRadius: '13px',
                                position: 'relative'
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

                        {/* Source List Code (Same as before, skipped repeating to focus on Insights) */}
                        <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '16px'
                        }}>
                            <h3 style={{margin: 0, fontSize: '17px', fontWeight: '600'}}>Your Sources</h3>
                            <button onClick={() => handleAddSource()} style={{
                                padding: '8px 16px',
                                background: colors.button,
                                color: colors.buttonText,
                                border: 'none',
                                borderRadius: '8px',
                                fontWeight: '600'
                            }}>+ Add
                            </button>
                        </div>
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
                                            fontWeight: 'bold'
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
                                        }}>Bypass AI (Get all news)</span>
                                        <div onClick={() => toggleReadAll(source.id, i)} style={{
                                            width: '40px',
                                            height: '22px',
                                            background: source.isReadAll ? colors.button : colors.hint,
                                            borderRadius: '11px',
                                            position: 'relative'
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
                    </div>
                )}

                {/* 🌟 NEW FEATURE: INSIGHTS TAB */}
                {activeTab === 'insights' && (
                    <div>
                        <h3 style={{fontSize: '18px', fontWeight: '700', marginBottom: '16px'}}>📈 Platform
                            Analytics</h3>

                        {/* DAU Graph Section */}
                        <div style={{
                            background: colors.secondaryBg,
                            padding: '20px 16px',
                            borderRadius: '16px',
                            marginBottom: '24px',
                            border: `1px solid ${colors.hint}20`
                        }}>
                            <h4 style={{margin: '0 0 4px 0', fontSize: '15px'}}>Daily Active Users (7 Days)</h4>
                            <p style={{margin: '0 0 20px 0', fontSize: '12px', color: colors.hint}}>Users reading news
                                and interacting with the bot.</p>

                            {/* Custom CSS Bar Chart */}
                            <div style={{
                                display: 'flex',
                                alignItems: 'flex-end',
                                justifyContent: 'space-between',
                                height: '140px',
                                paddingTop: '10px'
                            }}>
                                {dauStats.length > 0 ? dauStats.map((d, index) => {
                                    // Calculate height percentage (min 5% so bar is always visible)
                                    const heightPercent = Math.max((d.count / maxDau) * 100, 5);
                                    // Make date shorter (e.g. "Feb 24")
                                    const dateObj = new Date(d.date);
                                    const shortDate = dateObj.toLocaleDateString('en-US', {
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
                                            }}>
                                                {d.count}
                                            </span>
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
                                            }}>
                                                {shortDate}
                                            </span>
                                        </div>
                                    )
                                }) : (
                                    <div style={{
                                        width: '100%',
                                        textAlign: 'center',
                                        color: colors.hint,
                                        alignSelf: 'center'
                                    }}>No activity data yet.</div>
                                )}
                            </div>
                        </div>

                        {/* Top Sources Section */}
                        <div style={{
                            background: colors.secondaryBg,
                            padding: '20px 16px',
                            borderRadius: '16px',
                            border: `1px solid ${colors.hint}20`
                        }}>
                            <h4 style={{margin: '0 0 16px 0', fontSize: '15px'}}>🏆 Top Subscribed Sources</h4>

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
                                                    width: '24px', height: '24px', borderRadius: '50%',
                                                    background: index < 3 ? colors.chartBar : `${colors.hint}40`,
                                                    color: index < 3 ? '#fff' : colors.text,
                                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                    fontSize: '12px', fontWeight: 'bold'
                                                }}>
                                                    {index + 1}
                                                </div>
                                                <span style={{fontSize: '14px', fontWeight: '500'}}>{source.name}</span>
                                            </div>
                                            <span style={{fontSize: '13px', color: colors.hint, fontWeight: '600'}}>
                                                {source.subscriberCount} <span style={{fontSize: '11px'}}>users</span>
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div style={{textAlign: 'center', color: colors.hint}}>No sources added yet.</div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

function TabButton({active, onClick, children, colors}: {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
    colors: any;
}) {
    return (
        <button onClick={onClick} style={{
            flex: 1,
            padding: '14px 4px',
            background: 'transparent',
            border: 'none',
            borderBottom: active ? `3px solid ${colors.button}` : '3px solid transparent',
            color: active ? colors.button : colors.hint,
            fontSize: '15px',
            fontWeight: '600',
            cursor: 'pointer',
            transition: 'all 0.2s',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center'
        }}>
            {children}
        </button>
    );
}