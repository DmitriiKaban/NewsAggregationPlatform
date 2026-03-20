import { useState } from 'react';
import { tr, type Language } from '../i18n/translations.ts';
import type { ThemeColors } from '../types';
import { LANGUAGE_MAP } from '../utils/helpers';

interface Props {
    lang: Language;
    colors: ThemeColors;
    strictMode: boolean;
    dailySummary: boolean;
    weeklySummary: boolean;
    toggleStrictMode: () => void;
    toggleDailySummary: () => void;
    toggleWeeklySummary: () => void;
    changeLanguage: (lang: Language) => void;
}

export const SettingsTab = ({ lang, colors, strictMode, dailySummary, weeklySummary, toggleStrictMode, toggleDailySummary, toggleWeeklySummary, changeLanguage }: Props) => {
    const [isLangDropdownOpen, setIsLangDropdownOpen] = useState(false);

    return (
        <div style={{animation: 'fadeIn 0.3s ease-in-out', display: 'flex', flexDirection: 'column', gap: '16px'}}>
            <h3 style={{ fontSize: '20px', fontWeight: '800', margin: '0 0 4px 0' }}>{tr('settings.title', lang)}</h3>
            
            <div style={{ background: colors.secondaryBg, padding: '16px', borderRadius: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', border: `1px solid ${colors.hint}20` }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                    <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: '700', wordWrap: 'break-word' }}>{tr('settings.language', lang)}</h4>
                    <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '500', display: 'block', lineHeight: 1.4 }}>{tr('settings.language_desc', lang)}</span>
                </div>
                
                <div style={{ position: 'relative', flexShrink: 0 }}>
                    <button
                        onClick={() => setIsLangDropdownOpen(!isLangDropdownOpen)}
                        style={{
                            display: 'flex', alignItems: 'center', gap: '8px',
                            padding: '8px 12px', background: colors.bg,
                            border: `1px solid ${colors.hint}40`, borderRadius: '12px',
                            color: colors.text, fontSize: '14px', fontWeight: '600', cursor: 'pointer',
                            boxShadow: '0 2px 6px rgba(0,0,0,0.03)'
                        }}
                    >
                        <span>{LANGUAGE_MAP[lang].flag}</span>
                        <span>{LANGUAGE_MAP[lang].label}</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{opacity: 0.6, marginLeft: '2px'}}><polyline points="6 9 12 15 18 9"></polyline></svg>
                    </button>

                    {isLangDropdownOpen && (
                        <>
                            <div 
                                style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, zIndex: 99 }}
                                onClick={() => setIsLangDropdownOpen(false)}
                            />
                            
                            <div style={{
                                position: 'absolute', top: '100%', right: 0, marginTop: '8px',
                                background: colors.bg, borderRadius: '14px', border: `1px solid ${colors.hint}30`,
                                boxShadow: '0 8px 24px rgba(0,0,0,0.12)', overflow: 'hidden', zIndex: 100,
                                minWidth: '140px', display: 'flex', flexDirection: 'column'
                            }}>
                                {(Object.keys(LANGUAGE_MAP) as Language[]).map((l) => (
                                    <button
                                        key={l}
                                        onClick={() => {
                                            changeLanguage(l);
                                            setIsLangDropdownOpen(false);
                                        }}
                                        style={{
                                            display: 'flex', alignItems: 'center', gap: '10px',
                                            padding: '12px 16px', background: lang === l ? `${colors.button}15` : 'transparent',
                                            border: 'none', color: lang === l ? colors.button : colors.text,
                                            fontSize: '14px', fontWeight: '600', cursor: 'pointer', textAlign: 'left',
                                            transition: 'background 0.2s', width: '100%'
                                        }}
                                    >
                                        <span style={{fontSize: '16px'}}>{LANGUAGE_MAP[l].flag}</span>
                                        <span>{LANGUAGE_MAP[l].label}</span>
                                        {lang === l && <svg style={{marginLeft: 'auto'}} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>}
                                    </button>
                                ))}
                            </div>
                        </>
                    )}
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '16px', borderRadius: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', border: `1px solid ${colors.hint}20` }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                    <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: '700', wordWrap: 'break-word' }}>{tr('sources.strict_mode', lang)}</h4>
                    <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '500', display: 'block', lineHeight: 1.4 }}>{tr('sources.strict_mode_desc', lang)}</span>
                </div>
                <div onClick={toggleStrictMode} style={{ width: '50px', height: '28px', background: strictMode ? colors.success : colors.hint, borderRadius: '14px', position: 'relative', cursor: 'pointer', flexShrink: 0, transition: 'background 0.3s' }}>
                    <div style={{ width: '24px', height: '24px', background: '#fff', borderRadius: '50%', position: 'absolute', top: '2px', left: strictMode ? '24px' : '2px', transition: 'left 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)', boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}/>
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '16px', borderRadius: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', border: `1px solid ${colors.hint}20` }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                    <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: '700', wordWrap: 'break-word' }}>{tr('settings.daily_summary', lang)}</h4>
                    <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '500', display: 'block', lineHeight: 1.4 }}>{tr('settings.daily_summary_desc', lang)}</span>
                </div>
                <div onClick={toggleDailySummary} style={{ width: '50px', height: '28px', background: dailySummary ? colors.success : colors.hint, borderRadius: '14px', position: 'relative', cursor: 'pointer', flexShrink: 0, transition: 'background 0.3s' }}>
                    <div style={{ width: '24px', height: '24px', background: '#fff', borderRadius: '50%', position: 'absolute', top: '2px', left: dailySummary ? '24px' : '2px', transition: 'left 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)', boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}/>
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '16px', borderRadius: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', border: `1px solid ${colors.hint}20` }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                    <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: '700', wordWrap: 'break-word' }}>{tr('settings.weekly_summary', lang)}</h4>
                    <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '500', display: 'block', lineHeight: 1.4 }}>{tr('settings.weekly_summary_desc', lang)}</span>
                </div>
                <div onClick={toggleWeeklySummary} style={{ width: '50px', height: '28px', background: weeklySummary ? colors.success : colors.hint, borderRadius: '14px', position: 'relative', cursor: 'pointer', flexShrink: 0, transition: 'background 0.3s' }}>
                    <div style={{ width: '24px', height: '24px', background: '#fff', borderRadius: '50%', position: 'absolute', top: '2px', left: weeklySummary ? '24px' : '2px', transition: 'left 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)', boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}/>
                </div>
            </div>
        </div>
    );
};