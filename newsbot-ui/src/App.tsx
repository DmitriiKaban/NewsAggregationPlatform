import { useEffect, useState } from 'react';
import { tr, type Language } from './i18n/translations.ts';
import { ReportModal } from './components/ReportModal';
import { ModeratorDashboard } from './pages/ModeratorDashboard';
import { InterestsTab } from './pages/InterestsTab';
import { SourcesTab } from './pages/SourcesTab';
import { InsightsTab } from './pages/InsightsTab';
import { SettingsTab } from './pages/SettingsTab';
import { TabButton } from './components/TabButton';
import { showMessage, getHandle } from './utils/helpers';
import type { Source, Recommendation, GlobalInsights, UserInsights, DauData, TopSource } from './types';

export default function App() {
    const [lang, setLang] = useState<Language>('en');
    const [originalInterests, setOriginalInterests] = useState('');
    const [interestTags, setInterestTags] = useState<string[]>([]);
    const [isEditingInterests, setIsEditingInterests] = useState(false);
    
    const [sources, setSources] = useState<Source[]>([]);
    const [strictMode, setStrictMode] = useState(false);
    const [dailySummary, setDailySummary] = useState(false);
    const [weeklySummary, setWeeklySummary] = useState(false);
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    
    const [globalInsights, setGlobalInsights] = useState<GlobalInsights | null>(null);
    const [userInsights, setUserInsights] = useState<UserInsights | null>(null);
    const [dauStats, setDauStats] = useState<DauData[]>([]);
    const [topSources, setTopSources] = useState<TopSource[]>([]);
    
    const [activeTab, setActiveTab] = useState<'interests' | 'sources' | 'insights' | 'settings' | 'moderation'>('interests');
    const [userRole, setUserRole] = useState<'USER' | 'MODERATOR' | 'ADMIN'>('USER');
    
    const [reportModalState, setReportModalState] = useState<{isOpen: boolean, sourceId?: number, articleId?: number}>({ isOpen: false });

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [backendReachable, setBackendReachable] = useState(true);
    const [isTelegramEnvironment, setIsTelegramEnvironment] = useState(false);
    const [user, setUser] = useState<{ id?: number; first_name?: string; username?: string } | null>(null);
    const [displayName, setDisplayName] = useState('there');
    const [languageLoaded, setLanguageLoaded] = useState(false);

    const apiBaseUrl = "https://85e9-212-28-65-233.ngrok-free.app/api";
    
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
                setError("Could not identify user from Telegram(");
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
            fetch(`${apiBaseUrl}/analytics/global-dashboard`, fetchOptions).then(r => r.json()).catch(() => null),
            fetch(`${apiBaseUrl}/analytics/users/${user.id}/dashboard`, fetchOptions).then(r => r.json()).catch(() => null),
        ])
            .then(([profileData, recommendationsData, globalData, userData]) => {
                const backendLang = profileData.language?.toLowerCase();

                if (backendLang && ['en', 'ro', 'ru'].includes(backendLang)) {
                    setLang(backendLang as Language);
                }
                setLanguageLoaded(true);

                if (profileData.firstName?.trim()) setDisplayName(profileData.firstName);
                
                if (profileData.role) setUserRole(profileData.role);

                const interestsStr = Array.isArray(profileData.interests)
                    ? profileData.interests.join(', ')
                    : (profileData.interests || '');
                setOriginalInterests(interestsStr);
                
                setInterestTags(interestsStr.split(',').map((s: string) => s.trim()).filter(Boolean));
                
                setStrictMode(profileData.strictSourceFiltering || false);
                setDailySummary(profileData.dailySummaryEnabled || false);
                setWeeklySummary(profileData.weeklySummaryEnabled || false);

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

                if (globalData) {
                    setGlobalInsights(globalData);
                    setDauStats(globalData.dauStats || []);
                    setTopSources(globalData.topSources || []);
                }
                if (userData) {
                    setUserInsights(userData);
                }

                setRecommendations(recommendationsData);
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

    const toggleDailySummary = async () => {
        if (!backendReachable || !user?.id) return;
        const newState = !dailySummary;
        setDailySummary(newState);
        try {
            const res = await fetch(`${apiBaseUrl}/users/${user.id}/settings/daily-summary?enabled=${newState}`, {
                method: 'PUT', headers: {"ngrok-skip-browser-warning": "69420"},
            });
            if (!res.ok) throw new Error();
        } catch {
            setDailySummary(!newState);
            showMessage(tr('error.update_settings', lang));
        }
    };

    const toggleWeeklySummary = async () => {
        if (!backendReachable || !user?.id) return;
        const newState = !weeklySummary;
        setWeeklySummary(newState);
        try {
            const res = await fetch(`${apiBaseUrl}/users/${user.id}/settings/weekly-summary?enabled=${newState}`, {
                method: 'PUT', headers: {"ngrok-skip-browser-warning": "69420"},
            });
            if (!res.ok) throw new Error();
        } catch {
            setWeeklySummary(!newState);
            showMessage(tr('error.update_settings', lang));
        }
    };

    const changeLanguage = async (newLang: Language) => {
        if (!backendReachable || !user?.id || newLang === lang) return;
        const oldLang = lang;
        setLang(newLang);
        try {
            const res = await fetch(`${apiBaseUrl}/users/${user.id}/settings/language?lang=${newLang}`, {
                method: 'PUT', headers: {"ngrok-skip-browser-warning": "69420"},
            });
            if (!res.ok) throw new Error();
        } catch {
            setLang(oldLang);
            showMessage(tr('error.update_settings', oldLang));
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

    if (loading || !languageLoaded) return (
        <div style={{ minHeight: '100vh', background: colors.bg, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
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
        <div style={{ minHeight: '100vh', background: colors.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px', fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif' }}>
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
        <div style={{ minHeight: '100vh', background: colors.bg, color: colors.text, fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif', paddingBottom: '40px' }}>
            <ReportModal isOpen={reportModalState.isOpen} onClose={() => setReportModalState({ isOpen: false })} sourceId={reportModalState.sourceId} articleId={reportModalState.articleId} currentUserId={user.id!} apiBaseUrl={apiBaseUrl} colors={colors} />

            <div style={{padding: '24px 20px 20px 20px'}}>
                <h1 style={{fontSize: '28px', fontWeight: '800', margin: '0 0 6px 0', color: colors.text, letterSpacing: '-0.5px'}}>
                    {tr('header.greeting', lang, {name: displayName})}
                </h1>
                <p style={{fontSize: '15px', color: colors.hint, margin: 0, fontWeight: '500'}}>{tr('header.subtitle', lang)}</p>
            </div>

            <div style={{ padding: '10px 16px', marginBottom: '20px', position: 'sticky', top: 0, background: colors.bg, zIndex: 10, width: '100%', overflow: 'hidden' }}>
                <div className="scrollable-tabs" style={{ display: 'flex', width: '100%', background: colors.secondaryBg, borderRadius: '14px', padding: '6px', gap: '4px', overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
                    <TabButton active={activeTab === 'interests'} onClick={() => { setActiveTab('interests'); setIsEditingInterests(false); }} colors={colors} icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path><line x1="7" y1="7" x2="7.01" y2="7"></line></svg>}>
                        {tr('tab.interests', lang) || "Interests"}
                    </TabButton>
                    <TabButton active={activeTab === 'sources'} onClick={() => { setActiveTab('sources'); setIsEditingInterests(false); }} colors={colors} icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="8" y1="6" x2="21" y2="6"></line><line x1="8" y1="12" x2="21" y2="12"></line><line x1="8" y1="18" x2="21" y2="18"></line><line x1="3" y1="6" x2="3.01" y2="6"></line><line x1="3" y1="12" x2="3.01" y2="12"></line><line x1="3" y1="18" x2="3.01" y2="18"></line></svg>}>
                        {tr('tab.sources', lang) || "Sources"}
                    </TabButton>
                    <TabButton active={activeTab === 'insights'} onClick={() => { setActiveTab('insights'); setIsEditingInterests(false); }} colors={colors} icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>}>
                        {tr('tab.insights', lang) || "Insights"}
                    </TabButton>
                    <TabButton active={activeTab === 'settings'} onClick={() => { setActiveTab('settings'); setIsEditingInterests(false); }} colors={colors} icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>}>
                        {tr('tab.settings', lang) || "Settings"}
                    </TabButton>
                    
                    {(userRole === 'MODERATOR' || userRole === 'ADMIN') && (
                        <TabButton active={activeTab === 'moderation'} onClick={() => { setActiveTab('moderation'); setIsEditingInterests(false); }} colors={colors} icon={<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>}>
                            Mod
                        </TabButton>
                    )}
                </div>
            </div>

            <div style={{padding: '0 20px'}}>
                {activeTab === 'moderation' && <ModeratorDashboard moderatorId={user.id!} apiBaseUrl={apiBaseUrl} colors={colors} />}
                {activeTab === 'interests' && <InterestsTab lang={lang} colors={colors} apiBaseUrl={apiBaseUrl} interestTags={interestTags} setInterestTags={setInterestTags} isEditingInterests={isEditingInterests} setIsEditingInterests={setIsEditingInterests} recommendations={recommendations} handleAddSource={handleAddSource} />}
                {activeTab === 'sources' && <SourcesTab lang={lang} colors={colors} apiBaseUrl={apiBaseUrl} sources={sources} handleAddSource={() => handleAddSource()} handleRemoveSource={handleRemoveSource} toggleReadAll={toggleReadAll} setReportModalState={setReportModalState} />}
                {activeTab === 'insights' && globalInsights && <InsightsTab lang={lang} colors={colors} apiBaseUrl={apiBaseUrl} globalInsights={globalInsights} userInsights={userInsights} dauStats={dauStats} topSources={topSources} />}
                {activeTab === 'settings' && <SettingsTab lang={lang} colors={colors} strictMode={strictMode} dailySummary={dailySummary} weeklySummary={weeklySummary} toggleStrictMode={toggleStrictMode} toggleDailySummary={toggleDailySummary} toggleWeeklySummary={toggleWeeklySummary} changeLanguage={changeLanguage} />}
            </div>
            
            <style>{`
                * { box-sizing: border-box; }
                body { margin: 0; padding: 0; overflow-x: hidden; }
                @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
                .scrollable-tabs::-webkit-scrollbar { display: none; }
                .scrollable-tabs { scrollbar-width: none; -ms-overflow-style: none; }
            `}</style>
        </div>
    );
}