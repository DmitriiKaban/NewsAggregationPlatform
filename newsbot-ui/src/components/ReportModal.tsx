import React, { useState } from 'react';

interface ReportModalProps {
    isOpen: boolean;
    onClose: () => void;
    articleId?: number;
    sourceId?: number;
    currentUserId: number;
    apiBaseUrl: string;
    colors: any;
}

const REPORT_REASONS = [
    { value: 'SPAM', label: 'Spam' },
    { value: 'MISLEADING', label: 'Misleading / Fake News' },
    { value: 'HATE_SPEECH', label: 'Hate Speech / Inappropriate' },
    { value: 'BROKEN_LINK', label: 'Broken Link' },
    { value: 'UNRELATED_CONTENT', label: 'Unrelated Content' },
    { value: 'OTHER', label: 'Other' }
];

export const ReportModal: React.FC<ReportModalProps> = ({ isOpen, onClose, articleId, sourceId, currentUserId, apiBaseUrl, colors }) => {
    const [selectedReason, setSelectedReason] = useState<string>('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async () => {
        if (!selectedReason) {
            alert('Please select a reason.');
            return;
        }

        setIsSubmitting(true);
        try {
            const response = await fetch(`${apiBaseUrl}/reports`, {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'ngrok-skip-browser-warning': '69420'
                },
                body: JSON.stringify({
                    articleId,
                    sourceId,
                    reporterId: currentUserId,
                    reason: selectedReason
                })
            });

            if (response.ok) {
                alert('Thank you, your report has been submitted.');
                onClose();
            } else {
                alert('Failed to submit report. Please try again.');
            }
        } catch (error) {
            console.error('Error submitting report:', error);
            alert('Network error. Please try again.');
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
                    Report Content
                </h2>
                <p style={{ fontSize: '14px', color: colors.hint, margin: '0 0 20px 0', fontWeight: '500' }}>
                    Why are you reporting this?
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
                            <span style={{ fontSize: '15px', color: colors.text, fontWeight: '600' }}>{reason.label}</span>
                        </label>
                    ))}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                    <button onClick={onClose} disabled={isSubmitting} style={{
                        padding: '10px 16px', backgroundColor: 'transparent', color: colors.hint,
                        border: 'none', borderRadius: '14px', fontSize: '15px', fontWeight: '700', cursor: 'pointer'
                    }}>
                        Cancel
                    </button>
                    <button onClick={handleSubmit} disabled={isSubmitting} style={{
                        padding: '10px 16px', backgroundColor: colors.danger, color: '#fff',
                        border: 'none', borderRadius: '14px', fontSize: '15px', fontWeight: '700', 
                        cursor: isSubmitting ? 'not-allowed' : 'pointer', opacity: isSubmitting ? 0.7 : 1
                    }}>
                        {isSubmitting ? 'Submitting...' : 'Submit Report'}
                    </button>
                </div>
            </div>
        </div>
    );
};