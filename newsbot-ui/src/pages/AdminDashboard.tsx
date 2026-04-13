import { useState, useEffect } from 'react';
import { tr, type Language } from '../i18n/translations';
import type { ThemeColors } from '../types';
import { api } from '../services/api';

interface Props {
    lang: Language;
    colors: ThemeColors;
    adminId: number;
}

const CustomDropdown = ({ value, label, options, isOpen, setIsOpen, onSelect, colors }: any) => (
    <div style={{ position: 'relative', flex: 1, minWidth: '120px' }}>
        <button
            onClick={() => setIsOpen(!isOpen)}
            style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px',
                padding: '12px', background: colors.bg,
                border: `1px solid ${colors.hint}40`, borderRadius: '12px',
                color: colors.text, fontSize: '14px', fontWeight: '600', cursor: 'pointer',
                boxShadow: '0 2px 6px rgba(0,0,0,0.03)', width: '100%'
            }}
        >
            <span style={{overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}>{label}</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" style={{opacity: 0.6, flexShrink: 0}}><polyline points="6 9 12 15 18 9"></polyline></svg>
        </button>

        {isOpen && (
            <>
                <div 
                    style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, zIndex: 99 }}
                    onClick={() => setIsOpen(false)}
                />
                
                <div style={{
                    position: 'absolute', top: '100%', left: 0, right: 0, marginTop: '8px',
                    background: colors.bg, borderRadius: '14px', border: `1px solid ${colors.hint}30`,
                    boxShadow: '0 8px 24px rgba(0,0,0,0.12)', overflowY: 'auto', maxHeight: '200px', zIndex: 100,
                    display: 'flex', flexDirection: 'column'
                }}>
                    {options.map((opt: any) => (
                        <button
                            key={opt.value}
                            onClick={() => {
                                onSelect(opt.value);
                                setIsOpen(false);
                            }}
                            style={{
                                display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px',
                                padding: '12px 16px', background: value === opt.value ? `${colors.button}15` : 'transparent',
                                border: 'none', color: value === opt.value ? colors.button : colors.text,
                                fontSize: '14px', fontWeight: '600', cursor: 'pointer', textAlign: 'left',
                                transition: 'background 0.2s', width: '100%'
                            }}
                        >
                            <span style={{overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}>{opt.label}</span>
                            {value === opt.value && <svg style={{marginLeft: 'auto', flexShrink: 0}} width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>}
                        </button>
                    ))}
                </div>
            </>
        )}
    </div>
);

export const AdminDashboard = ({ lang, colors, adminId }: Props) => {
    const [newSourceUrl, setNewSourceUrl] = useState('');
    const [newSourceName, setNewSourceName] = useState('');
    const [sourceType, setSourceType] = useState<'TELEGRAM' | 'RSS'>('TELEGRAM');
    const [trustLevel, setTrustLevel] = useState('USER_GENERATED_CONTENT');
    const [targetUserId, setTargetUserId] = useState('');
    const [selectedRole, setSelectedRole] = useState<'USER' | 'MODERATOR' | 'ADMIN'>('USER');
    
    const [globalSources, setGlobalSources] = useState<any[]>([]);
    const [deleteSourceId, setDeleteSourceId] = useState<number | ''>('');

    const [isRoleDropdownOpen, setIsRoleDropdownOpen] = useState(false);
    const [isTypeDropdownOpen, setIsTypeDropdownOpen] = useState(false);
    const [isTrustDropdownOpen, setIsTrustDropdownOpen] = useState(false);
    const [isDeleteDropdownOpen, setIsDeleteDropdownOpen] = useState(false);

    const loadSources = async () => {
        try {
            const data = await api.getAllGlobalSources(adminId);
            setGlobalSources(data);
        } catch (error) {
            console.error("Failed to load sources", error);
        }
    };

    useEffect(() => {
        loadSources();
    }, []);

    const handleRoleUpdate = async () => {
        if (!targetUserId) return;
        if (Number(targetUserId) === adminId) {
            alert(tr('admin.alert.cannot_update_self', lang));
            return;
        }

        try {
            await api.updateUserRole(adminId, Number(targetUserId), selectedRole);
            alert(tr('admin.alert.role_success', lang));
            setTargetUserId('');
        } catch (error) {
            alert(tr('admin.alert.role_error', lang));
        }
    };

    const handleAddGlobalSource = async () => {
        if (!newSourceUrl) return;
        try {
            await api.addGlobalSource(adminId, newSourceUrl, sourceType, newSourceName, trustLevel);
            setNewSourceUrl('');
            setNewSourceName('');
            setTrustLevel('USER_GENERATED_CONTENT');
            alert(tr('admin.alert.source_add_success', lang));
            loadSources();
        } catch (error: any) {
            const errorMsg = error?.message || '';
            if (errorMsg.toLowerCase().includes('exist') || errorMsg.includes('409')) {
                alert(tr('admin.alert.source_exists', lang));
            } else if (errorMsg.toLowerCase().includes('invalid') || errorMsg.includes('400') || errorMsg.includes('404')) {
                alert(tr('admin.alert.invalid_link', lang));
            } else {
                alert(tr('admin.alert.source_add_error', lang));
            }
        }
    };

    const handleDeleteGlobalSource = async () => {
        if (!deleteSourceId) return;
        try {
            await api.deleteGlobalSource(adminId, Number(deleteSourceId));
            setDeleteSourceId('');
            alert(tr('admin.alert.source_del_success', lang));
            loadSources();
        } catch (error) {
            alert(tr('admin.alert.source_del_error', lang));
        }
    };

    return (
        <div style={{ animation: 'fadeIn 0.3s ease-in-out', display: 'flex', flexDirection: 'column', gap: '24px' }}>
            <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '800', color: colors.text }}>
                {tr('admin.title', lang)}
            </h3>
            
            <div style={{ padding: '20px', background: colors.secondaryBg, borderRadius: '20px', border: `1px solid ${colors.hint}20` }}>
                <h4 style={{ margin: '0 0 16px 0', fontSize: '16px', color: colors.text }}>
                    {tr('admin.manage_roles', lang)}
                </h4>
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                    <input 
                        type="number" 
                        placeholder={tr('admin.user_id', lang)} 
                        value={targetUserId} 
                        onChange={e => setTargetUserId(e.target.value)}
                        style={{ padding: '12px', borderRadius: '12px', border: `1px solid ${colors.hint}40`, background: colors.bg, color: colors.text, flex: 1, minWidth: '120px' }}
                    />
                    <CustomDropdown
                        value={selectedRole}
                        label={selectedRole}
                        options={[
                            { value: 'USER', label: 'USER' },
                            { value: 'MODERATOR', label: 'MODERATOR' },
                            { value: 'ADMIN', label: 'ADMIN' }
                        ]}
                        isOpen={isRoleDropdownOpen}
                        setIsOpen={setIsRoleDropdownOpen}
                        onSelect={setSelectedRole}
                        colors={colors}
                    />
                    <button 
                        onClick={handleRoleUpdate}
                        style={{ padding: '12px 20px', background: colors.button, color: colors.buttonText, border: 'none', borderRadius: '12px', fontWeight: '700', cursor: 'pointer' }}
                    >
                        {tr('admin.btn.update', lang)}
                    </button>
                </div>
            </div>

            <div style={{ padding: '20px', background: colors.secondaryBg, borderRadius: '20px', border: `1px solid ${colors.hint}20` }}>
                <h4 style={{ margin: '0 0 16px 0', fontSize: '16px', color: colors.text }}>
                    {tr('admin.add_global_source', lang)}
                </h4>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                        <input 
                            type="text" 
                            placeholder={tr('admin.source_url', lang)} 
                            value={newSourceUrl} 
                            onChange={e => setNewSourceUrl(e.target.value)} 
                            style={{ padding: '12px', borderRadius: '12px', border: `1px solid ${colors.hint}40`, background: colors.bg, color: colors.text, flex: 2, minWidth: '200px' }}
                        />
                        <CustomDropdown
                            value={sourceType}
                            label={sourceType}
                            options={[
                                { value: 'TELEGRAM', label: 'TELEGRAM' },
                                { value: 'RSS', label: 'RSS' }
                            ]}
                            isOpen={isTypeDropdownOpen}
                            setIsOpen={setIsTypeDropdownOpen}
                            onSelect={setSourceType}
                            colors={colors}
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                        <input 
                            type="text" 
                            placeholder={tr('admin.source_name', lang)} 
                            value={newSourceName} 
                            onChange={e => setNewSourceName(e.target.value)} 
                            style={{ padding: '12px', borderRadius: '12px', border: `1px solid ${colors.hint}40`, background: colors.bg, color: colors.text, flex: 2, minWidth: '200px' }}
                        />
                        <CustomDropdown
                            value={trustLevel}
                            label={tr(`trust.${trustLevel}`, lang)}
                            options={[
                                { value: 'OFFICIAL', label: tr('trust.OFFICIAL', lang) },
                                { value: 'VERIFIED_MEDIA', label: tr('trust.VERIFIED_MEDIA', lang) },
                                { value: 'INFLUENCER', label: tr('trust.INFLUENCER', lang) },
                                { value: 'USER_GENERATED_CONTENT', label: tr('trust.USER_GENERATED_CONTENT', lang) },
                                { value: 'SCAM', label: tr('trust.SCAM', lang) }
                            ]}
                            isOpen={isTrustDropdownOpen}
                            setIsOpen={setIsTrustDropdownOpen}
                            onSelect={setTrustLevel}
                            colors={colors}
                        />
                    </div>

                    <button 
                        onClick={handleAddGlobalSource}
                        style={{ padding: '12px 20px', background: colors.button, color: colors.buttonText, border: 'none', borderRadius: '12px', fontWeight: '700', cursor: 'pointer', width: '100%' }}
                    >
                        {tr('admin.btn.add_source', lang)}
                    </button>
                </div>
            </div>

            <div style={{ padding: '20px', background: colors.secondaryBg, borderRadius: '20px', border: `1px solid ${colors.hint}20` }}>
                <h4 style={{ margin: '0 0 16px 0', fontSize: '16px', color: colors.text }}>
                    {tr('admin.delete_global_source', lang)}
                </h4>
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                    <div style={{ flex: 2, minWidth: '200px' }}>
                        <CustomDropdown
                            value={deleteSourceId}
                            label={deleteSourceId ? (globalSources.find(s => s.id === deleteSourceId)?.name || deleteSourceId) : tr('admin.select_source', lang)}
                            options={globalSources.map(s => ({ value: s.id, label: s.name || s.url }))}
                            isOpen={isDeleteDropdownOpen}
                            setIsOpen={setIsDeleteDropdownOpen}
                            onSelect={setDeleteSourceId}
                            colors={colors}
                        />
                    </div>
                    <button 
                        onClick={handleDeleteGlobalSource}
                        style={{ padding: '12px 20px', background: colors.danger, color: '#fff', border: 'none', borderRadius: '12px', fontWeight: '700', cursor: 'pointer', flex: 1 }}
                    >
                        {tr('admin.btn.delete_source', lang)}
                    </button>
                </div>
            </div>
        </div>
    );
};