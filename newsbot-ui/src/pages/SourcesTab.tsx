import { tr, type Language } from '../i18n/translations.ts';
import type { ThemeColors, Source } from '../types';
import { ChannelAvatar } from '../components/ChannelAvatar';
import { getHandle, getValidUrl } from '../utils/helpers';

interface Props {
    lang: Language;
    colors: ThemeColors;
    apiBaseUrl: string;
    sources: Source[];
    handleAddSource: () => void;
    handleRemoveSource: (url: string, index: number) => void;
    toggleReadAll: (sourceId: number, index: number) => void;
    setReportModalState: (state: {isOpen: boolean, sourceId?: number, articleId?: number}) => void;
}

export const SourcesTab = ({ lang, colors, apiBaseUrl, sources, handleAddSource, handleRemoveSource, toggleReadAll, setReportModalState }: Props) => {
    return (
        <div style={{animation: 'fadeIn 0.3s ease-in-out'}}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '800' }}>{tr('sources.title', lang)}</h3>
                <button onClick={handleAddSource} style={{ padding: '10px 16px', background: colors.button, color: colors.buttonText, border: 'none', borderRadius: '14px', fontSize: '14px', fontWeight: '700', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}>
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
                        <div key={source.id} style={{ padding: '16px', background: colors.secondaryBg, borderRadius: '20px', border: `1px solid ${colors.hint}20`, display: 'flex', flexDirection: 'column', gap: '16px' }}>
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
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <button onClick={() => setReportModalState({ isOpen: true, sourceId: source.id })} style={{ background: `${colors.chartBar}15`, border: 'none', color: colors.chartBar, width: '36px', height: '36px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0 }}>
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"></path><line x1="4" y1="22" x2="4" y2="15"></line></svg>
                                    </button>
                                    <button onClick={() => handleRemoveSource(source.url, i)} style={{ background: `${colors.danger}15`, border: 'none', color: colors.danger, width: '36px', height: '36px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0 }}>
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                                    </button>
                                </div>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: `1px solid ${colors.hint}20`, paddingTop: '16px' }}>
                                <span style={{ fontSize: '14px', color: colors.text, fontWeight: '600', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>
                                    {tr('sources.bypass_ai', lang)}
                                </span>
                                <div onClick={() => toggleReadAll(source.id, i)} style={{ width: '46px', height: '26px', background: source.isReadAll ? colors.button : colors.hint, borderRadius: '13px', position: 'relative', cursor: 'pointer', transition: 'background 0.3s' }}>
                                    <div style={{ width: '22px', height: '22px', background: '#fff', borderRadius: '50%', position: 'absolute', top: '2px', left: source.isReadAll ? '22px' : '2px', transition: 'left 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)', boxShadow: '0 2px 4px rgba(0,0,0,0.2)' }}/>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};