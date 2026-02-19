import { useEffect, useState } from 'react';

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
}

const showMessage = (message: string) => {
    const tg = window.Telegram?.WebApp;
    if (tg?.showAlert) {
        tg.showAlert(message);
    } else if (tg?.showPopup) {
        tg.showPopup({ message });
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
    const [activeTab, setActiveTab] = useState<'interests' | 'sources'>('interests');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [backendReachable, setBackendReachable] = useState(true);
    const [isTelegramEnvironment, setIsTelegramEnvironment] = useState(false);
    const [user, setUser] = useState<{ id?: number; first_name?: string; username?: string } | null>(null);
    const [displayName, setDisplayName] = useState('there');

    const backendUrl = "https://donny-subevergreen-agreeably.ngrok-free.dev/api/users";

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
        danger: '#FF3B30'
    };

    useEffect(() => {
        if (tg) {
            setIsTelegramEnvironment(true);
            try {
                tg.ready();
                tg.expand();

                const userData = tg.initDataUnsafe?.user;
                if (userData && userData.id) {
                    setUser({
                        id: userData.id,
                        first_name: userData.first_name || 'User',
                        username: userData.username
                    });
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
        const timeoutId = setTimeout(() => controller.abort(), 10000);

        fetch(`${backendUrl}/${user.id}/profile`, {
            method: 'GET',
            headers: {
                "ngrok-skip-browser-warning": "69420",
                "Content-Type": "application/json",
            },
            signal: controller.signal
        })
            .then(res => {
                clearTimeout(timeoutId);
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(data => {
                console.log("✅ Profile data:", data);

                if (data.firstName && data.firstName.trim()) {
                    setDisplayName(data.firstName);
                }

                let interestsStr = '';
                if (typeof data.interests === 'string') {
                    interestsStr = data.interests;
                } else if (Array.isArray(data.interests)) {
                    interestsStr = data.interests.join(', ');
                }
                setInterests(interestsStr);
                setOriginalInterests(interestsStr);

                // Load strict mode setting
                setStrictMode(data.strictSourceFiltering || false);

                // Load sources with IDs
                if (Array.isArray(data.sources)) {
                    const sourceList = data.sources.map((s: any) => ({
                        id: s.id,
                        url: s.url || s.name || '',
                        isReadAll: s.isReadAll || false
                    })).filter((s: Source) => s.url);

                    console.log("📚 Loaded sources:", sourceList);
                    setSources(sourceList);
                }

                setLoading(false);
                setError(null);
                setBackendReachable(true);
            })
            .catch(err => {
                clearTimeout(timeoutId);
                console.error("❌ Failed to load profile:", err);
                let errorMessage = err.message;
                if (err.name === 'AbortError') errorMessage = 'Request timed out';
                else if (errorMessage.includes('Failed to fetch')) errorMessage = 'Cannot connect to backend';
                setError(errorMessage);
                setLoading(false);
                setBackendReachable(false);
            });
    }, [user?.id, backendUrl]);

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

    useEffect(() => {
        if (!isTelegramEnvironment) return;

        const handleSaveInterests = async () => {
            if (!user?.id || !backendReachable) return;

            try {
                tg?.MainButton.showProgress?.();
                const response = await fetch(`${backendUrl}/${user.id}/interests`, {
                    method: 'POST',
                    headers: {
                        "ngrok-skip-browser-warning": "69420",
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({ interest: interests })
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
    }, [activeTab, isEditingInterests, interests, user?.id, backendReachable, backendUrl, isTelegramEnvironment]);

    const toggleStrictMode = async () => {
        if (!backendReachable || !user?.id) {
            showMessage('⚠️ Backend not reachable');
            return;
        }

        const newState = !strictMode;
        setStrictMode(newState);

        try {
            const response = await fetch(`${backendUrl}/${user.id}/settings/strict-filtering?enabled=${newState}`, {
                method: 'PUT',
                headers: { "ngrok-skip-browser-warning": "69420" }
            });
            if (!response.ok) throw new Error();
            console.log(`✅ Strict mode updated to: ${newState}`);
        } catch (e) {
            console.error("❌ Failed to update strict mode");
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
            const response = await fetch(`${backendUrl}/${user.id}/sources/${sourceId}/read-all?readAll=${newState}`, {
                method: 'PUT',
                headers: { "ngrok-skip-browser-warning": "69420" }
            });
            if (!response.ok) throw new Error();
            console.log(`✅ Source ${sourceId} read-all updated to: ${newState}`);
        } catch (e) {
            console.error("❌ Failed to update read-all");
            updatedSources[currentIndex].isReadAll = !newState;
            setSources([...updatedSources]);
            showMessage('Failed to update preference');
        }
    };

    const handleAddSource = async () => {
        if (!backendReachable) {
            showMessage('⚠️ Backend not reachable');
            return;
        }

        const input = prompt('Enter channel (e.g. durov or @channel):');
        if (input && user?.id) {
            try {
                const response = await fetch(`${backendUrl}/${user.id}/sources`, {
                    method: 'POST',
                    headers: {
                        "ngrok-skip-browser-warning": "69420",
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({ source: input })
                });

                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(errorText || `HTTP ${response.status}`);
                }

                const profileResponse = await fetch(`${backendUrl}/${user.id}/profile`, {
                    method: 'GET',
                    headers: {
                        "ngrok-skip-browser-warning": "69420",
                        "Content-Type": "application/json",
                    }
                });

                if (profileResponse.ok) {
                    const data = await profileResponse.json();

                    if (Array.isArray(data.sources)) {
                        const sourceList = data.sources.map((s: any) => ({
                            id: s.id,
                            url: s.url || s.name || '',
                            isReadAll: s.isReadAll || false
                        })).filter((s: Source) => s.url);

                        setSources(sourceList);
                    }

                    setActiveTab('sources');
                    showMessage('✅ Source added!');
                } else {
                    window.location.reload();
                }
            } catch (err: any) {
                showMessage(`Failed: ${err.message}`);
            }
        }
    };

    const handleRemoveSource = async (url: string, index: number) => {
        if (!backendReachable || !user?.id) return;

        try {
            const response = await fetch(`${backendUrl}/${user.id}/sources?url=${encodeURIComponent(url)}`, {
                method: 'DELETE',
                headers: {
                    "ngrok-skip-browser-warning": "69420",
                    "Content-Type": "application/json",
                }
            });

            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            setSources(sources.filter((_, idx) => idx !== index));
        } catch (err: any) {
            showMessage(`Failed: ${err.message}`);
        }
    };

    if (loading) return (
        <div style={{ minHeight: '100vh', background: colors.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: '-apple-system, sans-serif' }}>
            <div style={{ textAlign: 'center', color: colors.text }}>
                <div style={{ fontSize: '64px', marginBottom: '24px' }}>📰</div>
                <div style={{ fontSize: '18px', fontWeight: '500' }}>Loading...</div>
            </div>
        </div>
    );

    if (!user) return (
        <div style={{ minHeight: '100vh', background: colors.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', fontFamily: '-apple-system, sans-serif' }}>
            <div style={{ textAlign: 'center', maxWidth: '400px', color: colors.text }}>
                <div style={{ fontSize: '64px', marginBottom: '24px' }}>❌</div>
                <h2 style={{ fontSize: '24px', fontWeight: '600', margin: '0 0 12px 0' }}>Could Not Find User</h2>
                <p style={{ fontSize: '15px', color: colors.hint, lineHeight: 1.5, margin: '0 0 20px 0' }}>Please open this app from Telegram.</p>
                {error && <div style={{ padding: '12px', background: `${colors.hint}20`, borderRadius: '8px', fontSize: '13px', color: colors.hint, textAlign: 'left' }}><strong>Error:</strong> {error}</div>}
            </div>
        </div>
    );

    return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            color: colors.text,
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
            paddingBottom: '20px'
        }}>
            <div style={{ padding: '20px', borderBottom: `1px solid ${colors.hint}40` }}>
                {!backendReachable && (
                    <div style={{ background: '#ff980020', border: '1px solid #ff980040', borderRadius: '12px', padding: '12px 16px', marginBottom: '16px', fontSize: '13px' }}>
                        <strong style={{ color: '#ff9800' }}>⚠️ Offline Mode</strong>
                        <div style={{ fontSize: '12px', marginTop: '4px', color: colors.hint }}>Backend not reachable</div>
                        <button onClick={() => window.location.reload()} style={{ marginTop: '8px', padding: '6px 12px', background: colors.button, border: 'none', borderRadius: '6px', color: colors.buttonText, fontSize: '12px', cursor: 'pointer', fontWeight: '500' }}>Retry</button>
                    </div>
                )}
                <h1 style={{ fontSize: '28px', fontWeight: '700', margin: '0 0 8px 0', color: colors.text }}>Hey, {displayName}! 👋</h1>
                <p style={{ fontSize: '15px', color: colors.hint, margin: 0 }}>Your personalized news hub</p>
            </div>

            <div style={{ display: 'flex', borderBottom: `2px solid ${colors.hint}20`, background: colors.secondaryBg }}>
                <TabButton active={activeTab === 'interests'} onClick={() => { setActiveTab('interests'); setIsEditingInterests(false); setInterests(originalInterests); }} colors={colors}>🎯 Interests</TabButton>
                <TabButton active={activeTab === 'sources'} onClick={() => { setActiveTab('sources'); setIsEditingInterests(false); }} colors={colors}>📚 Sources</TabButton>
            </div>

            <div style={{ padding: '20px' }}>
                {activeTab === 'interests' ? (
                    <div>
                        {/* NEW: Current Mode Display */}
                        <div style={{
                            background: strictMode ? `${colors.button}15` : `${colors.hint}15`,
                            border: `1px solid ${strictMode ? colors.button : colors.hint}40`,
                            borderRadius: '12px',
                            padding: '12px 16px',
                            marginBottom: '16px',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '10px'
                        }}>
                            <span style={{ fontSize: '20px' }}>{strictMode ? '🔒' : '🌐'}</span>
                            <div style={{ flex: 1 }}>
                                <div style={{ fontSize: '14px', fontWeight: '600', color: colors.text }}>
                                    {strictMode ? 'Strict Mode: ON' : 'AI Mode: Active'}
                                </div>
                                <div style={{ fontSize: '12px', color: colors.hint, marginTop: '2px' }}>
                                    {strictMode
                                        ? 'Only showing news from your subscribed sources'
                                        : 'AI filtering news from all sources based on your interests'}
                                </div>
                            </div>
                        </div>

                        <label style={{ display: 'block', fontSize: '15px', fontWeight: '600', marginBottom: '12px', color: colors.text }}>Your Current Interests</label>
                        {!isEditingInterests ? (
                            <>
                                <div style={{ padding: '16px', background: colors.secondaryBg, borderRadius: '12px', border: `1px solid ${colors.hint}20`, minHeight: '80px', fontSize: '15px', color: colors.text }}>
                                    {originalInterests || <span style={{ color: colors.hint, fontStyle: 'italic' }}>No interests set yet</span>}
                                </div>
                                <button onClick={() => setIsEditingInterests(true)} disabled={!backendReachable} style={{ marginTop: '16px', padding: '14px 24px', background: backendReachable ? colors.button : colors.hint, color: colors.buttonText, border: 'none', borderRadius: '10px', fontSize: '15px', fontWeight: '600', cursor: backendReachable ? 'pointer' : 'not-allowed', width: '100%' }}>
                                    ✏️ Update Interests
                                </button>
                            </>
                        ) : (
                            <>
                                <textarea value={interests} onChange={(e) => setInterests(e.target.value)} placeholder="AI, Crypto, Space, Tech..." autoFocus style={{ width: '100%', minHeight: '120px', padding: '14px', fontSize: '15px', border: `2px solid ${colors.button}`, borderRadius: '12px', fontFamily: 'inherit', resize: 'vertical', boxSizing: 'border-box', background: colors.bg, color: colors.text, outline: 'none' }} />
                                <p style={{ fontSize: '13px', color: colors.hint, marginTop: '8px', marginBottom: '0' }}>💡 Separate topics with commas</p>
                            </>
                        )}
                    </div>
                ) : (
                    <div>
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
                                <h4 style={{ margin: '0 0 4px 0', fontSize: '15px', color: colors.text }}>Strict Mode</h4>
                                <span style={{ fontSize: '13px', color: colors.hint }}>Only show news from my added sources</span>
                            </div>

                            <div
                                onClick={toggleStrictMode}
                                style={{
                                    width: '46px',
                                    height: '26px',
                                    background: strictMode ? colors.success : colors.hint,
                                    borderRadius: '13px',
                                    position: 'relative',
                                    cursor: 'pointer',
                                    transition: 'background-color 0.3s'
                                }}
                            >
                                <div style={{
                                    width: '22px',
                                    height: '22px',
                                    background: '#fff',
                                    borderRadius: '50%',
                                    position: 'absolute',
                                    top: '2px',
                                    left: strictMode ? '22px' : '2px',
                                    transition: 'left 0.3s',
                                    boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
                                }}/>
                            </div>
                        </div>

                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                            <h3 style={{ margin: 0, fontSize: '17px', fontWeight: '600', color: colors.text }}>Your Sources</h3>
                            <button onClick={handleAddSource} disabled={!backendReachable} style={{ padding: '8px 16px', background: backendReachable ? colors.button : colors.hint, color: colors.buttonText, border: 'none', borderRadius: '8px', fontSize: '14px', fontWeight: '600', cursor: backendReachable ? 'pointer' : 'not-allowed' }}>
                                + Add
                            </button>
                        </div>

                        {sources.length === 0 ? (
                            <div style={{ padding: '40px 20px', textAlign: 'center', background: colors.secondaryBg, borderRadius: '12px', border: `2px dashed ${colors.hint}40` }}>
                                <div style={{ fontSize: '48px', marginBottom: '12px', opacity: 0.5 }}>📭</div>
                                <p style={{ margin: 0, color: colors.hint, fontSize: '15px' }}>{backendReachable ? 'No sources yet. Add one!' : 'Cannot load - offline'}</p>
                            </div>
                        ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                {sources.map((source, i) => (
                                    <div key={source.id} style={{ padding: '14px', background: colors.secondaryBg, borderRadius: '12px', border: `1px solid ${colors.hint}20` }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                                            <span style={{ fontSize: '15px', color: colors.text, wordBreak: 'break-all', fontWeight: '500' }}>
                                                📡 {source.url}
                                            </span>
                                            <button onClick={() => handleRemoveSource(source.url, i)} disabled={!backendReachable} style={{ background: 'transparent', border: 'none', color: colors.danger, fontSize: '18px', fontWeight: 'bold', cursor: backendReachable ? 'pointer' : 'not-allowed', padding: '0 4px' }}>
                                                ✕
                                            </button>
                                        </div>

                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: `1px solid ${colors.hint}20`, paddingTop: '12px' }}>
                                            <span style={{ fontSize: '14px', color: colors.hint }}>Bypass AI (Get all news)</span>
                                            <div
                                                onClick={() => toggleReadAll(source.id, i)}
                                                style={{
                                                    width: '40px',
                                                    height: '22px',
                                                    background: source.isReadAll ? colors.button : colors.hint,
                                                    borderRadius: '11px',
                                                    position: 'relative',
                                                    cursor: 'pointer',
                                                    transition: 'background-color 0.3s'
                                                }}
                                            >
                                                <div style={{
                                                    width: '18px',
                                                    height: '18px',
                                                    background: '#fff',
                                                    borderRadius: '50%',
                                                    position: 'absolute',
                                                    top: '2px',
                                                    left: source.isReadAll ? '20px' : '2px',
                                                    transition: 'left 0.3s',
                                                    boxShadow: '0 1px 3px rgba(0,0,0,0.2)'
                                                }}/>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

function TabButton({ active, onClick, children, colors }: { active: boolean; onClick: () => void; children: React.ReactNode; colors: any; }) {
    return (
        <button onClick={onClick} style={{ flex: 1, padding: '14px 12px', background: 'transparent', border: 'none', borderBottom: active ? `3px solid ${colors.button}` : '3px solid transparent', color: active ? colors.button : colors.hint, fontSize: '15px', fontWeight: '600', cursor: 'pointer', transition: 'all 0.2s' }}>
            {children}
        </button>
    );
}