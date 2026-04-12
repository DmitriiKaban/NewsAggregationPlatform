import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import { tr, type Language } from '../i18n/translations';

interface Report {
    id: number;
    articleId?: number;
    sourceId?: number;
    source?: { id: number; url: string; name?: string };
    article?: { id: number; title: string; url: string };
    reporter?: { id: number; username?: string };
    reason?: string;
    status?: 'PENDING' | 'REVIEWED' | 'RESOLVED' | 'DISMISSED';
    reportedAt?: string | number[];
}

interface ModeratorDashboardProps {
    moderatorId: number;
    colors: any;
    lang: Language;
}

export const ModeratorDashboard: React.FC<ModeratorDashboardProps> = ({ moderatorId, colors, lang }) => {
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    
    const [filterStatus, setFilterStatus] = useState<string>('PENDING');

    useEffect(() => {
        fetchReports();
    }, [moderatorId]);

    const fetchReports = async () => {
        try {
            const data = await api.getReports(moderatorId);
            setReports(Array.isArray(data) ? data : (data?.content || data?.data || []));
        } catch (err) {
            setError(tr('mod.error.network', lang));
        } finally {
            setLoading(false);
        }
    };

    const updateStatus = async (reportId: number, status: string) => {
        try {
            await api.updateReportStatus(reportId, status, moderatorId);
            setReports(reports.map(r => r.id === reportId ? { ...r, status: status as any } : r));
        } catch (err) {
            alert(tr('mod.error.update', lang));
        }
    };

    const formatDate = (dateInput: any) => {
        if (!dateInput) return 'Unknown date';
        if (Array.isArray(dateInput)) {
            if (dateInput.length >= 3) {
                const [year, month, day, hour = 0, minute = 0, second = 0] = dateInput;
                return new Date(year, month - 1, day, hour, minute, second).toLocaleString(undefined, {
                    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                });
            }
            return 'Invalid Date';
        }
        return new Date(dateInput).toLocaleString(undefined, {
            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
    };

    const getHandle = (url: string) => {
        if (!url) return '';
        const parts = url.split('/');
        const handle = parts[parts.length - 1];
        return handle.startsWith('@') ? handle : '@' + handle;
    };

    const getBaseDomain = (xmlUrl: string) => {
        try {
            return new URL(xmlUrl).origin;
        } catch {
            return '#';
        }
    };

    const LinkIcon = () => (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0, opacity: 0.6, marginTop: '2px' }}>
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
            <polyline points="15 3 21 3 21 9"></polyline>
            <line x1="10" y1="14" x2="21" y2="3"></line>
        </svg>
    );

    const renderTarget = (report: Report) => {
        const sourceLink = report.source ? (
            <a href={getBaseDomain(report.source.url)} target="_blank" rel="noopener noreferrer" style={{ color: colors.link, textDecoration: 'none', fontWeight: '600', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                {report.source.name || getHandle(report.source.url)}
                <LinkIcon />
            </a>
        ) : null;

        if (report.article) {
            return (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <a href={report.article.url} target="_blank" rel="noopener noreferrer" style={{ color: colors.link, textDecoration: 'none', fontWeight: '700', fontSize: '15px', lineHeight: '1.4', display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                            {report.article.title || `${tr('mod.article', lang)} #${report.article.id}`}
                        </span>
                        <LinkIcon />
                    </a>
                    {sourceLink && (
                        <span style={{ color: colors.hint, fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
                            {tr('mod.via', lang)} {sourceLink}
                        </span>
                    )}
                </div>
            );
        }

        if (report.articleId) {
            return (
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span style={{ fontWeight: '600', color: colors.text }}>{tr('mod.article', lang)} #{report.articleId}</span>
                    {sourceLink && <span style={{ color: colors.hint, fontSize: '13px' }}>({tr('mod.via', lang)} {sourceLink})</span>}
                </div>
            );
        }

        if (sourceLink) return sourceLink;
        return <span style={{ color: colors.text }}>{tr('mod.source', lang)} #{report.sourceId || 'Unknown'}</span>;
    };

    if (loading) return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '200px', color: colors.hint }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ animation: 'spin 1s linear infinite', marginRight: '8px' }}>
                <line x1="12" y1="2" x2="12" y2="6"></line><line x1="12" y1="18" x2="12" y2="22"></line><line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line><line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line><line x1="2" y1="12" x2="6" y2="12"></line><line x1="18" y1="12" x2="22" y2="12"></line><line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line><line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
            </svg>
            {tr('mod.loading', lang)}
        </div>
    );

    if (error) return <div style={{ textAlign: 'center', padding: '40px', color: colors.danger, fontWeight: '600', backgroundColor: `${colors.danger}15`, borderRadius: '12px' }}>{error}</div>;

    const validReports = Array.isArray(reports) ? reports : [];
    
    const filteredReports = validReports.filter(report => {
        if (filterStatus === 'ALL') return true;
        return report.status === filterStatus;
    });

    return (
        <div style={{ animation: 'fadeIn 0.3s ease-in-out', display: 'flex', flexDirection: 'column', gap: '20px', paddingBottom: '24px' }}>
            <style>{`
                @keyframes spin { 100% { transform: rotate(360deg); } }
                @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
                .mod-card { transition: transform 0.2s ease, box-shadow 0.2s ease; }
                .mod-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.06); }
                .mod-btn { transition: filter 0.2s ease, transform 0.1s ease; }
                .mod-btn:hover { filter: brightness(1.1); }
                .mod-btn:active { transform: scale(0.97); }
                
                .hide-scrollbar::-webkit-scrollbar { display: none; }
                .hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
            `}</style>

            <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', paddingBottom: '8px', borderBottom: `1px solid ${colors.hint}20` }}>
                    <h3 style={{ fontSize: '22px', fontWeight: '800', margin: 0, color: colors.text }}>{tr('mod.title', lang)}</h3>
                    <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '600', backgroundColor: `${colors.hint}15`, padding: '4px 10px', borderRadius: '12px' }}>
                        {tr('mod.reports_count', lang, { count: filteredReports.length })}
                    </span>
                </div>

                <div className="hide-scrollbar" style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingTop: '12px', WebkitOverflowScrolling: 'touch' }}>
                    {['ALL', 'PENDING', 'RESOLVED', 'DISMISSED'].map(status => (
                        <button
                            key={status}
                            onClick={() => setFilterStatus(status)}
                            style={{
                                padding: '6px 14px',
                                borderRadius: '16px',
                                border: `1px solid ${filterStatus === status ? colors.button : colors.hint + '40'}`,
                                background: filterStatus === status ? colors.button : 'transparent',
                                color: filterStatus === status ? colors.buttonText : colors.text,
                                fontSize: '12px',
                                fontWeight: '700',
                                cursor: 'pointer',
                                whiteSpace: 'nowrap',
                                transition: 'all 0.2s ease'
                            }}
                        >
                            {tr(`mod.filter.${status}`, lang)}
                        </button>
                    ))}
                </div>
            </div>
            
            {filteredReports.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '60px 20px', color: colors.hint, fontWeight: '500', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', backgroundColor: colors.secondaryBg, borderRadius: '16px' }}>
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ opacity: 0.5 }}>
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline>
                    </svg>
                    <span>
                        {filterStatus === 'PENDING' 
                            ? tr('mod.empty.pending', lang) 
                            : tr('mod.empty.filtered', lang, { status: tr(`mod.filter.${filterStatus}`, lang).toLowerCase() })}
                    </span>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    {filteredReports.map((report) => (
                        <div key={report.id || Math.random()} className="mod-card" style={{ 
                            background: colors.secondaryBg, borderRadius: '16px', 
                            border: `1px solid ${colors.hint}20`, display: 'flex', flexDirection: 'column', overflow: 'hidden'
                        }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px', borderBottom: `1px solid ${colors.hint}15` }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <span style={{ 
                                        padding: '4px 10px', borderRadius: '8px', fontSize: '11px', fontWeight: '800', letterSpacing: '0.5px',
                                        background: report.status === 'PENDING' ? `${colors.chartBar || '#f59e0b'}20` : 
                                                    report.status === 'RESOLVED' ? `${colors.success}20` : 
                                                    `${colors.hint}20`,
                                        color: report.status === 'PENDING' ? (colors.chartBar || '#f59e0b') : 
                                               report.status === 'RESOLVED' ? colors.success : 
                                               colors.hint
                                    }}>
                                        {report.status ? tr(`mod.filter.${report.status}`, lang).toUpperCase() : 'UNKNOWN'}
                                    </span>
                                    <span style={{ fontSize: '12px', color: colors.hint, fontWeight: '600', display: 'flex', alignItems: 'center', gap: '4px' }}>
                                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                                        {formatDate(report.reportedAt)}
                                    </span>
                                </div>
                                <span style={{ fontSize: '12px', color: colors.hint, fontWeight: '600' }}>#{report.id}</span>
                            </div>

                            <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
                                <div>
                                    <span style={{ fontSize: '13px', color: colors.hint, fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{tr('mod.card.reason', lang)}</span>
                                    <h4 style={{ margin: '4px 0 0 0', fontSize: '18px', fontWeight: '700', color: colors.danger }}>
                                        {report.reason ? tr(`report.reason.${report.reason}`, lang) : 'UNKNOWN REASON'}
                                    </h4>
                                </div>

                                <div style={{ background: colors.bg, padding: '16px', borderRadius: '12px', border: `1px solid ${colors.hint}15`, display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                                        <div style={{ marginTop: '2px', color: colors.hint }}>
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle><circle cx="12" cy="12" r="2"></circle></svg>
                                        </div>
                                        <div style={{ flex: 1 }}>
                                            <span style={{ fontSize: '12px', color: colors.hint, fontWeight: '600', display: 'block', marginBottom: '2px' }}>{tr('mod.card.reported_content', lang)}</span>
                                            {renderTarget(report)}
                                        </div>
                                    </div>
                                    
                                    <div style={{ height: '1px', background: `${colors.hint}15` }}></div>
                                    
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                        <div style={{ color: colors.hint }}>
                                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                                        </div>
                                        <div>
                                            <span style={{ fontSize: '12px', color: colors.hint, fontWeight: '600', display: 'block', marginBottom: '2px' }}>{tr('mod.card.reported_by', lang)}</span>
                                            <span style={{ fontSize: '14px', color: colors.text, fontWeight: '500' }}>
                                                {report.reporter ? 
                                                    (typeof report.reporter === 'object' ? `@${report.reporter.username || report.reporter.id}` : `User ID: ${report.reporter}`) 
                                                    : tr('mod.card.anonymous', lang)}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {report.status === 'PENDING' && (
                                <div style={{ display: 'flex', gap: '12px', padding: '0 20px 20px 20px' }}>
                                    <button className="mod-btn" onClick={() => updateStatus(report.id, 'DISMISSED')} style={{
                                        flex: 1, padding: '12px', background: `${colors.hint}15`, color: colors.text,
                                        border: 'none', borderRadius: '12px', fontSize: '14px', fontWeight: '700', cursor: 'pointer',
                                        display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '6px'
                                    }}>
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>
                                        {tr('mod.btn.dismiss', lang)}
                                    </button>
                                    <button className="mod-btn" onClick={() => {
                                        const isArticleReport = !!report.articleId || !!report.article;
                                        if (isArticleReport) {
                                            if (window.confirm(tr('mod.confirm.ban_article', lang))) {
                                                updateStatus(report.id, 'RESOLVED');
                                            }
                                        } else {
                                            updateStatus(report.id, 'RESOLVED');
                                        }
                                    }} style={{
                                        flex: 1, padding: '12px', 
                                        background: (report.articleId || report.article) ? colors.danger : colors.button, 
                                        color: '#ffffff',
                                        border: 'none', borderRadius: '12px', fontSize: '14px', fontWeight: '700', cursor: 'pointer',
                                        display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '6px', textAlign: 'center'
                                    }}>
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                                        {report.articleId || report.article ? tr('mod.btn.resolve_ban', lang) : tr('mod.btn.resolve', lang)}
                                    </button>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};