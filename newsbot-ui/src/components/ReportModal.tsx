import React, { useState } from 'react';
import { api } from '../services/api';
import { tr, type Language } from '../i18n/translations.ts';

interface ReportModalProps {
    lang: Language;
    isOpen: boolean;
    onClose: () => void;
    articleId?: number;
    sourceId?: number;
    currentUserId: number;
    colors: any;
}

const REPORT_REASONS = [
    { value: 'SPAM', key: 'report.reason.SPAM' },
    { value: 'MISLEADING', key: 'report.reason.MISLEADING' },
    { value: 'HATE_SPEECH', key: 'report.reason.HATE_SPEECH' },
    { value: 'BROKEN_LINK', key: 'report.reason.BROKEN_LINK' },
    { value: 'UNRELATED_CONTENT', key: 'report.reason.UNRELATED_CONTENT' },
    { value: 'OTHER', key: 'report.reason.OTHER' }
];

export const ReportModal: React.FC<ReportModalProps> = ({ lang, isOpen, onClose, articleId, sourceId, currentUserId, colors }) => {
    const [selectedReason, setSelectedReason] = useState<string>('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async () => {
        if (!selectedReason) {
            alert(tr('report.alert.select_reason', lang));
            return;
        }

        if (!sourceId) {
            alert(tr('report.alert.missing_source', lang));
            return;
        }

        setIsSubmitting(true);
        try {
            await api.submitReport({
                articleId,
                sourceId,
                reporterId: currentUserId,
                reason: selectedReason
            });
            alert(tr('report.alert.success', lang));
            onClose();
        } catch (error: any) {
            alert(error.message || tr('report.alert.failed', lang));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.6)', display: 'flex',
            alignItems: 'center', justifyContent: 'center', zIndex: 9999, padding: '20px'
        }}>
            <div style={{
                backgroundColor: colors.bg, borderRadius: '24px', padding: '24px',
                width: '100%', maxWidth: '400px', boxShadow: '0 10px 30px rgba(0,0,0,0.2)',
                border: `1px solid ${colors.hint}20`
            }}>
                <h2 style={{ fontSize: '20px', fontWeight: '800', margin: '0 0 8px 0', color: colors.text }}>
                    {tr('report.title', lang)}
                </h2>
                <p style={{ fontSize: '14px', color: colors.hint, margin: '0 0 20px 0', fontWeight: '500' }}>
                    {tr('report.subtitle', lang)}
                </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '24px' }}>
                    {REPORT_REASONS.map(reason => (
                        <label key={reason.value} style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
                            <input
                                type="radio"
                                name="reportReason"
                                value={reason.value}
                                checked={selectedReason === reason.value}
                                onChange={(e) => setSelectedReason(e.target.value)}
                                style={{ accentColor: colors.danger, width: '18px', height: '18px' }}
                            />
                            <span style={{ fontSize: '15px', color: colors.text, fontWeight: '600' }}>{tr(reason.key, lang)}</span>
                        </label>
                    ))}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                    <button onClick={onClose} disabled={isSubmitting} style={{
                        padding: '10px 16px', backgroundColor: 'transparent', color: colors.hint,
                        border: 'none', borderRadius: '14px', fontSize: '15px', fontWeight: '700', cursor: 'pointer'
                    }}>
                        {tr('report.cancel', lang)}
                    </button>
                    <button onClick={handleSubmit} disabled={isSubmitting} style={{
                        padding: '10px 16px', backgroundColor: colors.danger, color: '#fff',
                        border: 'none', borderRadius: '14px', fontSize: '15px', fontWeight: '700', 
                        cursor: isSubmitting ? 'not-allowed' : 'pointer', opacity: isSubmitting ? 0.7 : 1
                    }}>
                        {isSubmitting ? tr('report.submitting', lang) : tr('report.submit', lang)}
                    </button>
                </div>
            </div>
        </div>
    );
};