import { useEffect, useState } from 'react';
import {
    useLaunchParams,
    miniApp,
    viewport,
    initData,
    mainButton,
    backButton
} from '@telegram-apps/sdk-react';

// Extend window type to include Telegram WebApp with all methods
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

// Helper function to show messages to user
const showMessage = (message: string) => {
    const tg = window.Telegram?.WebApp;

    // Try showAlert first
    if (tg?.showAlert) {
        tg.showAlert(message);
    }
    // Try showPopup as fallback
    else if (tg?.showPopup) {
        tg.showPopup({ message });
    }
    // Console fallback
    else {
        console.log('📢 Message:', message);
        alert(message); // Last resort for testing
    }
};

export default function App() {
    const lp = useLaunchParams();

    const [interests, setInterests] = useState('');
    const [sources, setSources] = useState<string[]>([]);
    const [activeTab, setActiveTab] = useState<'interests' | 'sources'>('interests');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const user = initData.user();
    const backendUrl = "https://ydcm33-ip-89-149-92-74.tunnelmole.net/api/users";

    useEffect(() => {
        if (user?.id) {
            console.log("🚀 Fetching profile for:", user.id);

            fetch(`${backendUrl}/${user.id}/profile`, {
                method: 'GET',
                headers: {
                    "ngrok-skip-browser-warning": "69420",
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                },
                mode: 'cors',
                credentials: 'omit'
            })
                .then(res => {
                    console.log("📡 Response status:", res.status);
                    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    return res.json();
                })
                .then(data => {
                    console.log("✅ Data received:", data);

                    // Handle interests - can be string or array
                    if (typeof data.interests === 'string') {
                        setInterests(data.interests);
                    } else if (Array.isArray(data.interests)) {
                        setInterests(data.interests.join(', '));
                    }

                    // Handle sources - can be array of strings or objects
                    if (Array.isArray(data.sources)) {
                        const sourceList = data.sources.map((s: any) =>
                            typeof s === 'string' ? s : (s.url || s.name || String(s))
                        );
                        setSources(sourceList);
                    }

                    setLoading(false);
                    setError(null);
                })
                .catch(err => {
                    console.error("❌ Failed to load profile:", err);
                    setError(err.message);
                    setLoading(false);
                    showMessage(`Connection failed: ${err.message}`);
                });
        } else {
            console.warn("⚠️ No User ID found. Are you testing outside Telegram?");
            setLoading(false);
            setError("User ID not found");
        }
    }, [user?.id]);

    useEffect(() => {
        // Use native Telegram WebApp API
        const tg = window.Telegram?.WebApp;

        if (tg) {
            tg.ready();
            tg.expand();
        }

        // Also try SDK methods
        if (miniApp.mount.isAvailable()) {
            miniApp.mount();
        }
        miniApp.ready();

        if (viewport.mount.isAvailable()) {
            viewport.mount();
            viewport.expand();
        }

        if (miniApp.setHeaderColor.isAvailable()) {
            miniApp.setHeaderColor('#1c1c1e');
        }
    }, []);

    // Handle back button
    useEffect(() => {
        const tg = window.Telegram?.WebApp;

        const handleBack = () => setActiveTab('interests');

        if (activeTab !== 'interests') {
            // Try both native and SDK
            tg?.BackButton.show();
            tg?.BackButton.onClick(handleBack);

            if (backButton.mount.isAvailable()) {
                backButton.mount();
                backButton.show();
                backButton.onClick(handleBack);
            }

            return () => {
                tg?.BackButton.offClick(handleBack);
                tg?.BackButton.hide();
                if (backButton.offClick) {
                    backButton.offClick(handleBack);
                }
            };
        } else {
            tg?.BackButton.hide();
            if (backButton.hide) {
                backButton.hide();
            }
        }
    }, [activeTab]);

    // Handle main button
    useEffect(() => {
        const tg = window.Telegram?.WebApp;

        const handleSaveInterests = async () => {
            if (!user?.id) return;

            console.log("💾 Saving interests:", interests);

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

                if (!response.ok) {
                    throw new Error(`Failed to save: ${response.status}`);
                }

                console.log("✅ Interests saved successfully");
                showMessage('✅ Interests saved successfully!');

            } catch (err: any) {
                console.error("❌ Failed to save interests:", err);
                showMessage(`Failed to save: ${err.message}`);
            }
        };

        if (activeTab === 'interests' && interests.trim()) {
            // Native API
            if (tg?.MainButton) {
                tg.MainButton.setText('Save Interests');
                tg.MainButton.enable();
                tg.MainButton.show();
                tg.MainButton.onClick(handleSaveInterests);
            }

            // SDK API
            if (mainButton.mount.isAvailable()) {
                mainButton.mount();
                mainButton.setParams({
                    text: 'Save Interests',
                    isEnabled: true,
                    isVisible: true
                });
                mainButton.onClick(handleSaveInterests);
            }

            return () => {
                tg?.MainButton.offClick(handleSaveInterests);
                if (mainButton.offClick) {
                    mainButton.offClick(handleSaveInterests);
                }
            };
        } else {
            tg?.MainButton.hide();
            if (mainButton.setParams) {
                mainButton.setParams({ isVisible: false });
            }
        }
    }, [activeTab, interests, user?.id]);

    const handleAddSource = async () => {
        const input = prompt('Enter channel name (e.g. durov or @point_md):');
        if (input && user?.id) {
            console.log("➕ Adding source:", input);

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

                if (!response.ok) {
                    throw new Error(`Failed to add source: ${response.status}`);
                }

                // Update UI optimistically
                setSources([...sources, input]);
                console.log("✅ Source added successfully");
                showMessage('✅ Source added!');

            } catch (err: any) {
                console.error("❌ Failed to add source:", err);
                showMessage(`Failed to add source: ${err.message}`);
            }
        }
    };

    const handleRemoveSource = async (source: string, index: number) => {
        if (!user?.id) return;

        console.log("🗑️ Removing source:", source);

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

            if (!response.ok) {
                throw new Error(`Failed to remove source: ${response.status}`);
            }

            // Update UI
            setSources(sources.filter((_, idx) => idx !== index));
            console.log("✅ Source removed successfully");

        } catch (err: any) {
            console.error("❌ Failed to remove source:", err);
            showMessage(`Failed to remove: ${err.message}`);
        }
    };

    const firstName = user?.first_name || 'User';

    // Loading state
    if (loading) {
        return (
            <div style={{
                minHeight: '100vh',
                background: 'var(--tg-theme-bg-color, #fff)',
                color: 'var(--tg-theme-text-color, #000)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
            }}>
                <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '48px', marginBottom: '16px' }}>⏳</div>
                    <div>Loading your profile...</div>
                </div>
            </div>
        );
    }

    // Error state with retry
    if (error) {
        return (
            <div style={{
                minHeight: '100vh',
                background: 'var(--tg-theme-bg-color, #fff)',
                color: 'var(--tg-theme-text-color, #000)',
                padding: '16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
            }}>
                <div style={{ textAlign: 'center', maxWidth: '300px' }}>
                    <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
                    <div style={{ marginBottom: '8px', fontWeight: '600' }}>Connection Error</div>
                    <div style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color, #999)', marginBottom: '16px' }}>
                        {error}
                    </div>
                    <button
                        onClick={() => window.location.reload()}
                        style={{
                            padding: '12px 24px',
                            background: 'var(--tg-theme-button-color, #007aff)',
                            color: 'var(--tg-theme-button-text-color, #fff)',
                            border: 'none',
                            borderRadius: '8px',
                            fontSize: '15px',
                            fontWeight: '500',
                            cursor: 'pointer'
                        }}
                    >
                        Retry
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div style={{
            minHeight: '100vh',
            background: 'var(--tg-theme-bg-color, #fff)',
            color: 'var(--tg-theme-text-color, #000)',
            padding: '16px',
            fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        }}>
            {/* Header */}
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{
                    fontSize: '28px',
                    fontWeight: '600',
                    margin: '0 0 8px 0'
                }}>
                    Welcome, {firstName}! 👋
                </h1>
                <p style={{
                    fontSize: '15px',
                    color: 'var(--tg-theme-hint-color, #999)',
                    margin: 0
                }}>
                    Manage your news preferences
                </p>
            </div>

            {/* Tab Navigation */}
            <div style={{
                display: 'flex',
                gap: '8px',
                marginBottom: '24px',
                borderBottom: '1px solid var(--tg-theme-section-separator-color, #e5e5ea)'
            }}>
                <TabButton
                    active={activeTab === 'interests'}
                    onClick={() => setActiveTab('interests')}
                >
                    🎯 Interests
                </TabButton>
                <TabButton
                    active={activeTab === 'sources'}
                    onClick={() => setActiveTab('sources')}
                >
                    📚 Sources
                </TabButton>
            </div>

            {/* Content */}
            {activeTab === 'interests' ? (
                <div>
                    <label style={{
                        display: 'block',
                        fontSize: '15px',
                        fontWeight: '500',
                        marginBottom: '8px'
                    }}>
                        What topics interest you?
                    </label>
                    <textarea
                        value={interests}
                        onChange={(e) => setInterests(e.target.value)}
                        placeholder="e.g., AI, Crypto, Space, Tech..."
                        style={{
                            width: '100%',
                            minHeight: '120px',
                            padding: '12px',
                            fontSize: '16px',
                            border: '1px solid var(--tg-theme-section-separator-color, #e5e5ea)',
                            borderRadius: '12px',
                            background: 'var(--tg-theme-secondary-bg-color, #f2f2f7)',
                            color: 'var(--tg-theme-text-color, #000)',
                            fontFamily: 'inherit',
                            resize: 'vertical',
                            boxSizing: 'border-box'
                        }}
                    />
                    <p style={{
                        fontSize: '13px',
                        color: 'var(--tg-theme-hint-color, #999)',
                        marginTop: '8px'
                    }}>
                        Separate topics with commas. We'll find news matching your interests.
                    </p>
                </div>
            ) : (
                <div>
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        marginBottom: '16px'
                    }}>
                        <h3 style={{ margin: 0, fontSize: '17px', fontWeight: '600' }}>
                            Your Sources
                        </h3>
                        <button
                            onClick={handleAddSource}
                            style={{
                                padding: '8px 16px',
                                background: 'var(--tg-theme-button-color, #007aff)',
                                color: 'var(--tg-theme-button-text-color, #fff)',
                                border: 'none',
                                borderRadius: '8px',
                                fontSize: '15px',
                                fontWeight: '500',
                                cursor: 'pointer'
                            }}
                        >
                            + Add Source
                        </button>
                    </div>

                    {sources.length === 0 ? (
                        <div style={{
                            padding: '40px 20px',
                            textAlign: 'center',
                            color: 'var(--tg-theme-hint-color, #999)'
                        }}>
                            <p style={{ fontSize: '40px', margin: '0 0 12px 0' }}>📭</p>
                            <p style={{ margin: 0 }}>No sources yet. Add one to get started!</p>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            {sources.map((source, i) => (
                                <div key={i} style={{
                                    padding: '12px',
                                    background: 'var(--tg-theme-secondary-bg-color, #f2f2f7)',
                                    borderRadius: '12px',
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'center'
                                }}>
                                    <span style={{ fontSize: '15px', wordBreak: 'break-all' }}>{source}</span>
                                    <button
                                        onClick={() => handleRemoveSource(source, i)}
                                        style={{
                                            background: 'transparent',
                                            border: 'none',
                                            color: 'var(--tg-theme-destructive-text-color, #ff3b30)',
                                            fontSize: '15px',
                                            cursor: 'pointer',
                                            padding: '4px 8px',
                                            marginLeft: '8px',
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

            {/* Debug info */}
            <details style={{
                marginTop: '40px',
                fontSize: '12px',
                color: 'var(--tg-theme-hint-color, #999)'
            }}>
                <summary>Debug Info</summary>
                <pre style={{
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                    fontSize: '11px',
                    marginTop: '8px'
                }}>
                    {JSON.stringify({
                        userId: user?.id,
                        username: user?.username,
                        firstName: user?.first_name,
                        platform: lp.platform,
                        hasNativeAPI: !!window.Telegram?.WebApp,
                        hasShowAlert: !!window.Telegram?.WebApp?.showAlert,
                        backendUrl: backendUrl,
                        error: error
                    }, null, 2)}
                </pre>
            </details>
        </div>
    );
}

function TabButton({
                       active,
                       onClick,
                       children
                   }: {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
}) {
    return (
        <button
            onClick={onClick}
            style={{
                flex: 1,
                padding: '12px',
                background: 'transparent',
                border: 'none',
                borderBottom: active
                    ? '2px solid var(--tg-theme-button-color, #007aff)'
                    : '2px solid transparent',
                color: active
                    ? 'var(--tg-theme-button-color, #007aff)'
                    : 'var(--tg-theme-hint-color, #999)',
                fontSize: '15px',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'all 0.2s'
            }}
        >
            {children}
        </button>
    );
}