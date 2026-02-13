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
                    query_id?: string;
                    auth_date?: number;
                    hash?: string;
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
    const [sources, setSources] = useState<string[]>([]);
    const [activeTab, setActiveTab] = useState<'interests' | 'sources'>('interests');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [backendReachable, setBackendReachable] = useState(true);
    const [isTelegramEnvironment, setIsTelegramEnvironment] = useState(false);
    const [user, setUser] = useState<{ id?: number; first_name?: string; username?: string } | null>(null);

    const backendUrl = "https://tw98ca-ip-89-149-68-82.tunnelmole.net/api/users";

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
    };

    useEffect(() => {
        if (tg) {
            console.log("✅ Telegram WebApp detected");
            setIsTelegramEnvironment(true);

            try {
                tg.ready();
                tg.expand();

                // Try to get user data from initDataUnsafe
                console.log("🔍 Checking initDataUnsafe:", tg.initDataUnsafe);
                console.log("🔍 Checking initData:", tg.initData);

                const userData = tg.initDataUnsafe?.user;

                if (userData && userData.id) {
                    console.log("✅ Found user data:", userData);
                    setUser({
                        id: userData.id,
                        first_name: userData.first_name || 'User',
                        username: userData.username
                    });
                } else {
                    console.warn("⚠️ No user data in initDataUnsafe, using fallback");
                    // Try to parse from initData string
                    if (tg.initData) {
                        try {
                            const params = new URLSearchParams(tg.initData);
                            const userJson = params.get('user');
                            if (userJson) {
                                const parsedUser = JSON.parse(userJson);
                                console.log("✅ Parsed user from initData:", parsedUser);
                                setUser({
                                    id: parsedUser.id,
                                    first_name: parsedUser.first_name || 'User',
                                    username: parsedUser.username
                                });
                                return;
                            }
                        } catch (e) {
                            console.error("❌ Failed to parse initData:", e);
                        }
                    }

                    // Last resort: use a default ID for testing
                    console.warn("⚠️ Using test user ID");
                    setUser({ id: 938781412, first_name: 'Telegram User' });
                }
            } catch (err) {
                console.error("❌ Error initializing Telegram WebApp:", err);
                setIsTelegramEnvironment(false);
                setUser({ id: 938781412, first_name: 'Browser User' });
            }
        } else {
            console.warn("⚠️ Not in Telegram environment");
            setIsTelegramEnvironment(false);
            setUser({ id: 938781412, first_name: 'Browser User' });
        }
    }, []);

    useEffect(() => {
        if (user?.id) {
            console.log("🚀 Fetching profile for user:", user);

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 10000);

            fetch(`${backendUrl}/${user.id}/profile`, {
                method: 'GET',
                headers: {
                    "ngrok-skip-browser-warning": "69420",
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                },
                mode: 'cors',
                credentials: 'omit',
                signal: controller.signal
            })
                .then(res => {
                    clearTimeout(timeoutId);
                    console.log("📡 Response status:", res.status);
                    if (!res.ok) throw new Error(`HTTP ${res.status}`);
                    return res.json();
                })
                .then(data => {
                    console.log("✅ Data received:", data);

                    if (typeof data.interests === 'string') {
                        setInterests(data.interests);
                    } else if (Array.isArray(data.interests)) {
                        setInterests(data.interests.join(', '));
                    }

                    if (Array.isArray(data.sources)) {
                        console.log("📚 Raw sources:", data.sources);

                        const sourceList = data.sources.map((s: any) => {
                            if (typeof s === 'string') return s;
                            if (typeof s === 'object' && s !== null) {
                                return s.url || s.name || s.source || JSON.stringify(s);
                            }
                            return String(s);
                        }).filter(Boolean);

                        console.log("📚 Parsed sources:", sourceList);
                        setSources(sourceList);
                    } else {
                        console.warn("⚠️ Sources is not an array:", data.sources);
                        setSources([]);
                    }

                    setLoading(false);
                    setError(null);
                    setBackendReachable(true);
                })
                .catch(err => {
                    clearTimeout(timeoutId);
                    console.error("❌ Failed to load profile:", err);

                    let errorMessage = err.message;
                    if (err.name === 'AbortError') {
                        errorMessage = 'Request timed out';
                    } else if (errorMessage.includes('Failed to fetch')) {
                        errorMessage = 'Cannot connect to backend';
                    }

                    setError(errorMessage);
                    setLoading(false);
                    setBackendReachable(false);
                });
        } else {
            setLoading(false);
            setError("No user ID");
            setBackendReachable(false);
        }
    }, [user?.id, backendUrl]);

    useEffect(() => {
        if (!isTelegramEnvironment) return;

        const handleBack = () => setActiveTab('interests');

        if (activeTab !== 'interests') {
            tg?.BackButton.show();
            tg?.BackButton.onClick(handleBack);

            return () => {
                tg?.BackButton.offClick(handleBack);
                tg?.BackButton.hide();
            };
        } else {
            tg?.BackButton.hide();
        }
    }, [activeTab, isTelegramEnvironment]);

    useEffect(() => {
        if (!isTelegramEnvironment) return;

        const handleSaveInterests = async () => {
            if (!user?.id || !backendReachable) {
                showMessage('⚠️ Cannot save - backend not reachable');
                return;
            }

            try {
                const response = await fetch(`${backendUrl}/${user.id}/interests`, {
                    method: 'POST',
                    headers: {
                        "ngrok-skip-browser-warning": "69420",
                        "Content-Type": "application/json",
                        "Accept": "application/json",
                    },
                    mode: 'cors',
                    credentials: 'omit',
                    body: JSON.stringify({ interest: interests })
                });

                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                showMessage('✅ Interests saved!');
            } catch (err: any) {
                showMessage(`Failed: ${err.message}`);
            }
        };

        if (activeTab === 'interests' && interests.trim()) {
            if (tg?.MainButton) {
                tg.MainButton.setText('Save Interests');
                tg.MainButton.enable();
                tg.MainButton.show();
                tg.MainButton.onClick(handleSaveInterests);

                return () => {
                    tg.MainButton.offClick(handleSaveInterests);
                };
            }
        } else {
            tg?.MainButton.hide();
        }
    }, [activeTab, interests, user?.id, backendReachable, backendUrl, isTelegramEnvironment]);

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
                        "Accept": "application/json",
                    },
                    mode: 'cors',
                    credentials: 'omit',
                    body: JSON.stringify({ source: input })
                });

                if (!response.ok) throw new Error(`HTTP ${response.status}`);

                setSources([...sources, input]);
                showMessage('✅ Source added!');
            } catch (err: any) {
                showMessage(`Failed: ${err.message}`);
            }
        }
    };

    const handleRemoveSource = async (source: string, index: number) => {
        if (!backendReachable || !user?.id) return;

        try {
            const response = await fetch(`${backendUrl}/${user.id}/sources?url=${encodeURIComponent(source)}`, {
                method: 'DELETE',
                headers: {
                    "ngrok-skip-browser-warning": "69420",
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                },
                mode: 'cors',
                credentials: 'omit'
            });

            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            setSources(sources.filter((_, idx) => idx !== index));
        } catch (err: any) {
            showMessage(`Failed: ${err.message}`);
        }
    };

    const handleSaveClick = async () => {
        if (!user?.id || !backendReachable) {
            alert('⚠️ Backend not reachable');
            return;
        }

        try {
            const response = await fetch(`${backendUrl}/${user.id}/interests`, {
                method: 'POST',
                headers: {
                    "ngrok-skip-browser-warning": "69420",
                    "Content-Type": "application/json",
                },
                mode: 'cors',
                credentials: 'omit',
                body: JSON.stringify({ interest: interests })
            });

            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            alert('✅ Saved!');
        } catch (err: any) {
            alert(`Failed: ${err.message}`);
        }
    };

    const firstName = user?.first_name || 'there';

    if (loading) {
        return (
            <div style={{
                minHeight: '100vh',
                background: colors.bg,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
            }}>
                <div style={{ textAlign: 'center', color: colors.text }}>
                    <div style={{ fontSize: '64px', marginBottom: '24px' }}>📰</div>
                    <div style={{ fontSize: '18px', fontWeight: '500' }}>Loading your feed...</div>
                </div>
            </div>
        );
    }

    return (
        <div style={{
            minHeight: '100vh',
            background: colors.bg,
            color: colors.text,
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        }}>
            {/* Header */}
            <div style={{
                padding: '20px',
                borderBottom: `1px solid ${colors.hint}40`
            }}>
                {!isTelegramEnvironment && (
                    <div style={{
                        background: `${colors.link}20`,
                        border: `1px solid ${colors.link}40`,
                        borderRadius: '12px',
                        padding: '12px 16px',
                        marginBottom: '16px',
                        color: colors.link,
                        fontSize: '13px'
                    }}>
                        <strong>ℹ️ Browser Mode</strong> - Open in Telegram for full features
                    </div>
                )}

                {error && (
                    <div style={{
                        background: '#ff980020',
                        border: '1px solid #ff980040',
                        borderRadius: '12px',
                        padding: '12px 16px',
                        marginBottom: '16px',
                        fontSize: '13px'
                    }}>
                        <strong style={{ color: '#ff9800' }}>⚠️ Offline Mode</strong>
                        <div style={{ fontSize: '12px', marginTop: '4px', color: colors.hint }}>{error}</div>
                        <button
                            onClick={() => window.location.reload()}
                            style={{
                                marginTop: '8px',
                                padding: '6px 12px',
                                background: colors.button,
                                border: 'none',
                                borderRadius: '6px',
                                color: colors.buttonText,
                                fontSize: '12px',
                                cursor: 'pointer',
                                fontWeight: '500'
                            }}
                        >
                            Retry
                        </button>
                    </div>
                )}

                <h1 style={{
                    fontSize: '28px',
                    fontWeight: '700',
                    margin: '0 0 8px 0',
                    color: colors.text
                }}>
                    Hey, {firstName}! 👋
                </h1>
                <p style={{
                    fontSize: '15px',
                    color: colors.hint,
                    margin: 0
                }}>
                    Your personalized news hub
                </p>
            </div>

            {/* Tabs */}
            <div style={{
                display: 'flex',
                borderBottom: `2px solid ${colors.hint}20`,
                background: colors.secondaryBg
            }}>
                <TabButton
                    active={activeTab === 'interests'}
                    onClick={() => setActiveTab('interests')}
                    colors={colors}
                >
                    🎯 Interests
                </TabButton>
                <TabButton
                    active={activeTab === 'sources'}
                    onClick={() => setActiveTab('sources')}
                    colors={colors}
                >
                    📚 Sources
                    {sources.length > 0 && (
                        <span style={{
                            marginLeft: '6px',
                            background: colors.button,
                            color: colors.buttonText,
                            padding: '2px 8px',
                            borderRadius: '12px',
                            fontSize: '12px',
                            fontWeight: '600'
                        }}>
                            {sources.length}
                        </span>
                    )}
                </TabButton>
            </div>

            {/* Content */}
            <div style={{ padding: '20px' }}>
                {activeTab === 'interests' ? (
                    <div>
                        <label style={{
                            display: 'block',
                            fontSize: '15px',
                            fontWeight: '600',
                            marginBottom: '12px',
                            color: colors.text
                        }}>
                            What topics interest you?
                        </label>
                        <textarea
                            value={interests}
                            onChange={(e) => setInterests(e.target.value)}
                            placeholder="AI, Crypto, Space, Tech..."
                            style={{
                                width: '100%',
                                minHeight: '120px',
                                padding: '14px',
                                fontSize: '15px',
                                border: `1px solid ${colors.hint}40`,
                                borderRadius: '12px',
                                fontFamily: 'inherit',
                                resize: 'vertical',
                                boxSizing: 'border-box',
                                background: colors.secondaryBg,
                                color: colors.text,
                                outline: 'none'
                            }}
                        />
                        <p style={{
                            fontSize: '13px',
                            color: colors.hint,
                            marginTop: '8px'
                        }}>
                            💡 Separate topics with commas
                        </p>

                        {!isTelegramEnvironment && interests.trim() && (
                            <button
                                onClick={handleSaveClick}
                                disabled={!backendReachable}
                                style={{
                                    marginTop: '16px',
                                    padding: '14px 24px',
                                    background: backendReachable ? colors.button : colors.hint,
                                    color: colors.buttonText,
                                    border: 'none',
                                    borderRadius: '10px',
                                    fontSize: '15px',
                                    fontWeight: '600',
                                    cursor: backendReachable ? 'pointer' : 'not-allowed',
                                    width: '100%'
                                }}
                            >
                                💾 Save Interests
                            </button>
                        )}
                    </div>
                ) : (
                    <div>
                        <div style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '16px'
                        }}>
                            <h3 style={{
                                margin: 0,
                                fontSize: '17px',
                                fontWeight: '600',
                                color: colors.text
                            }}>
                                Your Sources
                            </h3>
                            <button
                                onClick={handleAddSource}
                                disabled={!backendReachable}
                                style={{
                                    padding: '10px 16px',
                                    background: backendReachable ? colors.button : colors.hint,
                                    color: colors.buttonText,
                                    border: 'none',
                                    borderRadius: '8px',
                                    fontSize: '14px',
                                    fontWeight: '600',
                                    cursor: backendReachable ? 'pointer' : 'not-allowed'
                                }}
                            >
                                + Add
                            </button>
                        </div>

                        {sources.length === 0 ? (
                            <div style={{
                                padding: '40px 20px',
                                textAlign: 'center',
                                background: colors.secondaryBg,
                                borderRadius: '12px',
                                border: `2px dashed ${colors.hint}40`
                            }}>
                                <div style={{ fontSize: '48px', marginBottom: '12px', opacity: 0.5 }}>📭</div>
                                <p style={{
                                    margin: 0,
                                    color: colors.hint,
                                    fontSize: '15px'
                                }}>
                                    {backendReachable
                                        ? 'No sources yet. Add one!'
                                        : 'Cannot load - offline'}
                                </p>
                            </div>
                        ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                {sources.map((source, i) => (
                                    <div key={i} style={{
                                        padding: '14px',
                                        background: colors.secondaryBg,
                                        borderRadius: '12px',
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        border: `1px solid ${colors.hint}20`
                                    }}>
                                        <div style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            flex: 1,
                                            minWidth: 0
                                        }}>
                                            <span style={{
                                                fontSize: '20px',
                                                marginRight: '10px'
                                            }}>
                                                📡
                                            </span>
                                            <span style={{
                                                fontSize: '15px',
                                                color: colors.text,
                                                wordBreak: 'break-all',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis'
                                            }}>
                                                {source}
                                            </span>
                                        </div>
                                        <button
                                            onClick={() => handleRemoveSource(source, i)}
                                            disabled={!backendReachable}
                                            style={{
                                                background: 'transparent',
                                                border: 'none',
                                                color: backendReachable ? '#f44336' : colors.hint,
                                                fontSize: '13px',
                                                fontWeight: '600',
                                                cursor: backendReachable ? 'pointer' : 'not-allowed',
                                                padding: '8px 12px',
                                                marginLeft: '10px',
                                                borderRadius: '6px',
                                                flexShrink: 0
                                            }}
                                        >
                                            Remove
                                        </button>
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

function TabButton({ active, onClick, children, colors }: {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
    colors: any;
}) {
    return (
        <button
            onClick={onClick}
            style={{
                flex: 1,
                padding: '14px 12px',
                background: 'transparent',
                border: 'none',
                borderBottom: active ? `3px solid ${colors.button}` : '3px solid transparent',
                color: active ? colors.button : colors.hint,
                fontSize: '15px',
                fontWeight: '600',
                cursor: 'pointer',
                transition: 'all 0.2s'
            }}
        >
            {children}
        </button>
    );
}