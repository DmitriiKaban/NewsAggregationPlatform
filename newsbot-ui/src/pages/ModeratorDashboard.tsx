import React, { useEffect, useState } from 'react';

interface Report {
    id: number;
    articleId?: number;
    sourceId?: number;
    source?: { id: number; url: string; name?: string };
    reporter?: { id: number; username?: string };
    reason?: string;
    status?: 'PENDING' | 'REVIEWED' | 'RESOLVED' | 'DISMISSED';
    reportedAt?: string | number[];
}

interface ModeratorDashboardProps {
    moderatorId: number;
    apiBaseUrl: string;
    colors: any;
}

export const ModeratorDashboard: React.FC<ModeratorDashboardProps> = ({ moderatorId, apiBaseUrl, colors }) => {
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        fetchReports();
    }, [moderatorId]);

    const fetchReports = async () => {
        try {
            const response = await fetch(`${apiBaseUrl}/reports?moderatorId=${moderatorId}`, {
                headers: { 'ngrok-skip-browser-warning': '69420' }
            });
            if (response.ok) {
                const data = await response.json();
                
                console.log("Fetched reports payload:", data); 

                setReports(Array.isArray(data) ? data : (data?.content || data?.data || []));
            } else {
                setError('Unauthorized or unable to fetch reports.');
            }
        } catch (err) {
            setError('Network error occurred.');
        } finally {
            setLoading(false);
        }
    };

    const updateStatus = async (reportId: number, status: string) => {
        try {
            const response = await fetch(`${apiBaseUrl}/reports/${reportId}/status?status=${status}&moderatorId=${moderatorId}`, {
                method: 'PATCH',
                headers: { 'ngrok-skip-browser-warning': '69420' }
            });
            if (response.ok) {
                setReports(reports.map(r => r.id === reportId ? { ...r, status: status as any } : r));
            }
        } catch (err) {
            console.error('Failed to update status', err);
            alert('Failed to update status. Please try again.');
        }
    };

    const formatDate = (dateInput: any) => {
        if (!dateInput) return 'Unknown date';
        if (Array.isArray(dateInput)) {
            if (dateInput.length >= 3) {
                const [year, month, day, hour = 0, minute = 0, second = 0] = dateInput;
                return new Date(year, month - 1, day, hour, minute, second).toLocaleString();
            }
            return 'Invalid Date';
        }
        return new Date(dateInput).toLocaleString();
    };

    const getHandle = (url: string) => {
        if (!url) return '';
        const parts = url.split('/');
        const handle = parts[parts.length - 1];
        return handle.startsWith('@') ? handle : '@' + handle;
    };

    const renderTarget = (report: Report) => {
        const sourceLink = report.source ? (
            <a href={report.source.url} target="_blank" rel="noopener noreferrer" style={{ color: colors.link, textDecoration: 'none', fontWeight: '600' }}>
                {report.source.name || getHandle(report.source.url)}
            </a>
        ) : null;

        if (report.articleId) {
            return (
                <>
                    Article #{report.articleId}
                    {sourceLink && (
                        <span style={{ color: colors.hint, marginLeft: '6px', fontSize: '13px' }}>
                            (via {sourceLink})
                        </span>
                    )}
                </>
            );
        }

        if (sourceLink) return sourceLink;
        
        return `Source #${report.sourceId || 'Unknown'}`;
    };

    if (loading) return <div style={{ textAlign: 'center', padding: '40px', color: colors.hint }}>Loading reports...</div>;
    if (error) return <div style={{ textAlign: 'center', padding: '40px', color: colors.danger }}>{error}</div>;

    const validReports = Array.isArray(reports) ? reports : [];

    return (
        <div style={{ animation: 'fadeIn 0.3s ease-in-out', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h3 style={{ fontSize: '20px', fontWeight: '800', margin: '0 0 4px 0' }}>Moderator Dashboard</h3>
            
            {validReports.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: colors.hint, fontWeight: '500' }}>
                    No reports found. All caught up!
                </div>
            ) : (
                validReports.map((report) => (
                    <div key={report.id || Math.random()} style={{ 
                        background: colors.secondaryBg, padding: '16px', borderRadius: '20px', 
                        border: `1px solid ${colors.hint}20`, display: 'flex', flexDirection: 'column', gap: '12px' 
                    }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <div>
                                <span style={{ fontSize: '12px', color: colors.hint, fontWeight: '600', display: 'block', marginBottom: '2px' }}>
                                    {formatDate(report.reportedAt)}
                                </span>
                                <span style={{ fontSize: '15px', color: colors.danger, fontWeight: '800' }}>
                                    {(report.reason || 'UNKNOWN_REASON').replace(/_/g, ' ')}
                                </span>
                            </div>
                            <span style={{ 
                                padding: '4px 10px', borderRadius: '10px', fontSize: '11px', fontWeight: '800',
                                background: report.status === 'PENDING' ? `${colors.chartBar}20` : `${colors.success}20`,
                                color: report.status === 'PENDING' ? colors.chartBar : colors.success
                            }}>
                                {report.status || 'UNKNOWN'}
                            </span>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                            <span style={{ fontSize: '14px', color: colors.text }}>
                                <strong>Target:</strong> {renderTarget(report)}
                            </span>
                            <span style={{ fontSize: '14px', color: colors.text }}>
                                <strong>Reporter:</strong> {report.reporter ? 
                                    (typeof report.reporter === 'object' ? `@${report.reporter.username || report.reporter.id}` : `User ${report.reporter}`) 
                                    : 'Unknown'}
                            </span>
                        </div>

                        {report.status === 'PENDING' && (
                            <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                                <button onClick={() => updateStatus(report.id, 'DISMISSED')} style={{
                                    flex: 1, padding: '8px', background: `${colors.hint}20`, color: colors.text,
                                    border: 'none', borderRadius: '10px', fontSize: '13px', fontWeight: '700', cursor: 'pointer'
                                }}>
                                    Dismiss
                                </button>
                                <button onClick={() => updateStatus(report.id, 'RESOLVED')} style={{
                                    flex: 1, padding: '8px', background: colors.button, color: colors.buttonText,
                                    border: 'none', borderRadius: '10px', fontSize: '13px', fontWeight: '700', cursor: 'pointer'
                                }}>
                                    Resolve
                                </button>
                            </div>
                        )}
                    </div>
                ))
            )}
        </div>
    );
};