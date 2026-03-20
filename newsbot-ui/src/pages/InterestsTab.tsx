import { useState } from 'react';
import { tr, type Language } from '../i18n/translations.ts';
import type { ThemeColors, Recommendation } from '../types';
import { ChannelAvatar } from '../components/ChannelAvatar';

interface Props {
    lang: Language;
    colors: ThemeColors;
    apiBaseUrl: string;
    interestTags: string[];
    setInterestTags: (tags: string[] | ((prev: string[]) => string[])) => void;
    isEditingInterests: boolean;
    setIsEditingInterests: (val: boolean) => void;
    recommendations: Recommendation[];
    handleAddSource: (url: string) => void;
}

export const InterestsTab = ({ lang, colors, apiBaseUrl, interestTags, setInterestTags, isEditingInterests, setIsEditingInterests, recommendations, handleAddSource }: Props) => {
    const [tagInput, setTagInput] = useState('');

    return (
        <div style={{animation: 'fadeIn 0.3s ease-in-out'}}>
            <label style={{display: 'flex', alignItems: 'center', gap: '8px', fontSize: '16px', fontWeight: '700', marginBottom: '16px', color: colors.text}}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path><line x1="7" y1="7" x2="7.01" y2="7"></line></svg>
                {tr('interests.title', lang)}
            </label>

            <div onClick={() => !isEditingInterests && setIsEditingInterests(true)} style={{cursor: !isEditingInterests ? 'pointer' : 'default'}}>
                {!isEditingInterests ? (
                    <div style={{
                        padding: '20px', background: colors.secondaryBg, borderRadius: '20px', border: `1px solid ${colors.hint}20`,
                        minHeight: '100px', display: 'flex', flexWrap: 'wrap', gap: '10px', alignItems: 'flex-start',
                        boxShadow: '0 4px 20px rgba(0,0,0,0.03)', position: 'relative', overflow: 'hidden'
                    }}>
                        {interestTags.length > 0 ? (
                            interestTags.map((tag, i) => (
                                <span key={i} style={{ background: colors.bg, border: `1px solid ${colors.button}30`, color: colors.text, padding: '8px 16px', borderRadius: '20px', fontSize: '14px', fontWeight: '600', boxShadow: '0 2px 8px rgba(0,0,0,0.02)' }}>{tag}</span>
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
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', padding: '16px', border: `2px solid ${colors.button}`, borderRadius: '20px', background: colors.bg, minHeight: '140px', alignItems: 'flex-start', boxShadow: `0 0 0 4px ${colors.button}15` }}>
                            {interestTags.map((tag, i) => (
                                <div key={i} style={{ background: `${colors.button}15`, color: colors.button, padding: '6px 12px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '14px', fontWeight: '600' }}>
                                    <span>{tag}</span>
                                    <button onClick={(e) => {
                                        e.stopPropagation();
                                        setInterestTags(interestTags.filter((_, index) => index !== i));
                                    }} style={{ background: 'transparent', border: 'none', color: colors.button, cursor: 'pointer', padding: '0', display: 'flex', alignItems: 'center', opacity: 0.7 }}>
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
                                placeholder={interestTags.length === 0 ? tr('interests.input_placeholder', lang) : tr('interests.type_prompt', lang)}
                                autoFocus
                                style={{ flex: 1, minWidth: '150px', border: 'none', outline: 'none', background: 'transparent', color: colors.text, fontSize: '15px', padding: '8px 0', fontFamily: 'inherit', fontWeight: '500' }}
                            />
                        </div>
                        <p style={{ fontSize: '13px', color: colors.hint, marginTop: '12px', fontWeight: '500', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                            {tr('interests.hint', lang)}
                        </p>
                    </div>
                )}
            </div>

            {!isEditingInterests && recommendations.length > 0 && (
                <div style={{ marginTop: '36px' }}>
                    <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '6px' }}>{tr('recommendations.title', lang)}</h3>
                    <p style={{ fontSize: '14px', color: colors.hint, marginBottom: '16px', fontWeight: '500' }}>{tr('recommendations.subtitle', lang)}</p>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                        {recommendations.map(rec => (
                            <div key={rec.url} style={{ padding: '16px', background: colors.secondaryBg, borderRadius: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: `1px solid ${colors.hint}20` }}>
                                <div style={{display: 'flex', alignItems: 'center', gap: '12px', overflow: 'hidden'}}>
                                    <ChannelAvatar url={rec.url} name={rec.name} size={42} fontSize={16} apiBaseUrl={apiBaseUrl} />
                                    <div style={{overflow: 'hidden'}}>
                                        <div style={{ fontSize: '15px', fontWeight: '600', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{rec.name}</div>
                                        <div style={{ fontSize: '13px', color: colors.hint, marginTop: '2px', fontWeight: '500' }}>
                                            {tr(rec.peerCount > 1 ? 'recommendations.peers_plural' : 'recommendations.peers', lang, { count: rec.peerCount })}
                                        </div>
                                    </div>
                                </div>
                                <button onClick={() => handleAddSource(rec.url)} style={{ background: colors.button, color: colors.buttonText, border: 'none', padding: '8px 16px', borderRadius: '14px', fontSize: '14px', fontWeight: '700', cursor: 'pointer', flexShrink: 0 }}>
                                    {tr('recommendations.add', lang)}
                                </button>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};