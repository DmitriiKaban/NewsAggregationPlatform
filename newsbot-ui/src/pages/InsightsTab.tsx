import { tr, type Language } from '../i18n/translations.ts';
import type { ThemeColors, GlobalInsights, UserInsights, DauData, TopSource } from '../types';
import { ChannelAvatar } from '../components/ChannelAvatar';
import { getHandle, getValidUrl } from '../utils/helpers';

interface Props {
    lang: Language;
    colors: ThemeColors;
    apiBaseUrl: string;
    globalInsights: GlobalInsights;
    userInsights: UserInsights | null;
    dauStats: DauData[];
    topSources: TopSource[];
}

export const InsightsTab = ({ lang, colors, apiBaseUrl, globalInsights, userInsights, dauStats, topSources }: Props) => {
    const maxDau = dauStats.length > 0 ? Math.max(...dauStats.map(d => d.count)) : 1;

    return (
        <div style={{animation: 'fadeIn 0.3s ease-in-out', display: 'flex', flexDirection: 'column', gap: '20px'}}>
            <h3 style={{ fontSize: '20px', fontWeight: '800', margin: 0 }}>{tr('insights.title', lang)}</h3>

            <div style={{ background: colors.secondaryBg, padding: '24px 20px', borderRadius: '24px', border: `1px solid ${colors.hint}20` }}>
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
                                <div style={{ width: '32px', height: `${heightPercent}px`, background: `linear-gradient(180deg, ${colors.chartBar} 0%, ${colors.chartBar}40 100%)`, borderRadius: '8px 8px 0 0', transition: 'height 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275)' }}/>
                                <span style={{ fontSize: '11px', color: colors.hint, marginTop: '10px', transform: 'rotate(-45deg)', whiteSpace: 'nowrap', fontWeight: '600' }}>{shortDate}</span>
                            </div>
                        );
                    }) : (
                        <div style={{ width: '100%', textAlign: 'center', color: colors.hint, alignSelf: 'center', fontWeight: '500' }}>{tr('insights.no_data', lang)}</div>
                    )}
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '24px 20px', borderRadius: '24px', border: `1px solid ${colors.hint}20` }}>
                <h4 style={{margin: '0 0 20px 0', fontSize: '16px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                    {tr('insights.top_sources', lang)}
                </h4>
                {topSources.length > 0 ? (
                    <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
                        {topSources.map((source, index) => (
                            <div key={index} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 0' }}>
                                <div style={{display: 'flex', alignItems: 'center', gap: '12px', overflow: 'hidden', flex: 1}}>
                                    <div style={{ width: '24px', height: '24px', borderRadius: '8px', background: index < 3 ? colors.chartBar : `${colors.hint}30`, color: index < 3 ? '#fff' : colors.text, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: '800', flexShrink: 0 }}>
                                        #{index + 1}
                                    </div>
                                    <ChannelAvatar url={source.url} name={source.name} size={40} fontSize={16} apiBaseUrl={apiBaseUrl} />
                                    <div style={{overflow: 'hidden', display: 'flex', flexDirection: 'column', justifyContent: 'center'}}>
                                        <span style={{fontSize: '15px', fontWeight: '700', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', color: colors.text, lineHeight: 1.2}}>
                                            {source.name}
                                        </span>
                                        <a href={getValidUrl(source.url)} target="_blank" rel="noopener noreferrer" style={{ fontSize: '13px', color: colors.link, textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '4px', marginTop: '4px', fontWeight: '600' }}>
                                            {getHandle(source.url)}
                                        </a>
                                    </div>
                                </div>
                                <div style={{display: 'flex', flexDirection: 'column', alignItems: 'flex-end', flexShrink: 0}}>
                                    <span style={{fontSize: '15px', color: colors.text, fontWeight: '800'}}>
                                        {source.subscriberCount}
                                    </span>
                                    <span style={{fontSize: '11px', color: colors.hint, fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.5px'}}>
                                        {tr('insights.users', lang)}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div style={{ textAlign: 'center', color: colors.hint, fontWeight: '500' }}>{tr('insights.no_sources', lang)}</div>
                )}
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
                <div style={{ flex: 1, background: colors.secondaryBg, padding: '16px', borderRadius: '20px', border: `1px solid ${colors.hint}20` }}>
                    <div style={{ fontSize: '12px', color: colors.hint, fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }}>{tr('insights.articles_per_session', lang)}</div>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                        <span style={{ fontSize: '24px', fontWeight: '800', color: colors.text }}>
                            {userInsights?.avgArticlesPerSession?.toFixed(1) || '0.0'}
                        </span>
                        <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '500' }}>{tr('insights.you', lang)}</span>
                    </div>
                    <div style={{ fontSize: '13px', color: colors.button, marginTop: '4px', fontWeight: '600' }}>
                        {tr('insights.global', lang)} {globalInsights.avgArticlesPerSession?.toFixed(1) || '0.0'}
                    </div>
                </div>

                <div style={{ flex: 1, background: colors.secondaryBg, padding: '16px', borderRadius: '20px', border: `1px solid ${colors.hint}20` }}>
                    <div style={{ fontSize: '12px', color: colors.hint, fontWeight: '700', textTransform: 'uppercase', marginBottom: '8px' }}>{tr('insights.topic_entropy', lang)}</div>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
                        <span style={{ fontSize: '24px', fontWeight: '800', color: colors.text }}>
                            {userInsights?.avgTopicDiversityEntropy?.toFixed(2) || '0.00'}
                        </span>
                        <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '500' }}>{tr('insights.you', lang)}</span>
                    </div>
                    <div style={{ fontSize: '13px', color: colors.chartBar, marginTop: '4px', fontWeight: '600' }}>
                        {tr('insights.global', lang)} {globalInsights.avgTopicDiversityEntropy?.toFixed(2) || '0.00'}
                    </div>
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '20px', borderRadius: '24px', border: `1px solid ${colors.hint}20`, display: 'flex', alignItems: 'center', gap: '20px' }}>
                <div style={{ position: 'relative', width: '60px', height: '60px', flexShrink: 0 }}>
                    <svg viewBox="0 0 36 36" style={{ width: '100%', height: '100%' }}>
                        <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke={`${colors.danger}20`} strokeWidth="4" />
                        <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke={colors.danger} strokeWidth="4" strokeDasharray={`${globalInsights.strictModeAdoptionPercent || 0}, 100`} style={{ transition: 'stroke-dasharray 1s ease-out' }} />
                    </svg>
                    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '14px', fontWeight: '800', color: colors.text }}>
                        {Math.round(globalInsights.strictModeAdoptionPercent || 0)}%
                    </div>
                </div>
                <div>
                    <h4 style={{ margin: '0 0 4px 0', fontSize: '16px', fontWeight: '700' }}>{tr('insights.strict_mode_adoption', lang)}</h4>
                    <p style={{ margin: 0, fontSize: '13px', color: colors.hint, lineHeight: 1.4 }}>{tr('insights.strict_mode_desc', lang)}</p>
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '24px 20px', borderRadius: '24px', border: `1px solid ${colors.hint}20` }}>
                <h4 style={{margin: '0 0 16px 0', fontSize: '16px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.button} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path></svg>
                    {tr('insights.most_read_topics', lang)}
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {globalInsights.topGlobalTopics?.map((topic, i) => {
                        const maxReads = globalInsights.topGlobalTopics[0]?.readCount || 1;
                        const widthPct = Math.max((topic.readCount / maxReads) * 100, 5);
                        return (
                            <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', fontWeight: '600' }}>
                                    <span>{topic.topic}</span>
                                    <span style={{ color: colors.hint }}>{topic.readCount}</span>
                                </div>
                                <div style={{ width: '100%', height: '8px', background: `${colors.hint}20`, borderRadius: '4px', overflow: 'hidden' }}>
                                    <div style={{ width: `${widthPct}%`, height: '100%', background: colors.button, borderRadius: '4px', transition: 'width 1s ease-out' }} />
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '24px 20px', borderRadius: '24px', border: `1px solid ${colors.hint}20` }}>
                <h4 style={{margin: '0 0 16px 0', fontSize: '16px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.success} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path></svg>
                    {tr('insights.source_sentiment', lang)}
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    {globalInsights.sourceFeedback?.slice(0, 5).map((feedback, i) => {
                        const total = feedback.likes + feedback.dislikes;
                        const likePct = total > 0 ? (feedback.likes / total) * 100 : 50;
                        return (
                            <div key={i}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', fontWeight: '600', marginBottom: '6px' }}>
                                    <span>{feedback.sourceName}</span>
                                    <span style={{ fontSize: '11px', color: colors.hint }}>{total} {tr('insights.votes', lang)}</span>
                                </div>
                                <div style={{ display: 'flex', width: '100%', height: '8px', borderRadius: '4px', overflow: 'hidden', gap: '2px' }}>
                                    <div style={{ width: `${likePct}%`, height: '100%', background: colors.success, transition: 'width 1s ease-out' }} />
                                    <div style={{ width: `${100 - likePct}%`, height: '100%', background: colors.danger, transition: 'width 1s ease-out' }} />
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>

            <div style={{ background: colors.secondaryBg, padding: '24px 20px', borderRadius: '24px', border: `1px solid ${colors.hint}20` }}>
                <h4 style={{margin: '0 0 16px 0', fontSize: '16px', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={colors.chartBar} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>
                    {tr('insights.top_read_all', lang)}
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {globalInsights.topReadAllSources?.map((source, i) => (
                        <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <div style={{ width: '20px', fontSize: '12px', fontWeight: '800', color: colors.hint }}>#{i + 1}</div>
                                <span style={{ fontSize: '14px', fontWeight: '600', color: colors.text }}>{source.sourceName}</span>
                            </div>
                            <span style={{ fontSize: '13px', fontWeight: '800', color: colors.chartBar }}>
                                {source.userCount} <span style={{ fontSize: '11px', fontWeight: '600', color: colors.hint, textTransform: 'uppercase' }}>{tr('insights.users', lang)}</span>
                            </span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};