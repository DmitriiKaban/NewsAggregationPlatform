import { apiClient } from './apiClient';
import type { Recommendation, GlobalInsights, UserInsights } from '../types';

export const api = {
    getUserProfile: (userId: number, signal?: AbortSignal) => 
        apiClient(`/api/users/${userId}/profile`, { signal }),
        
    getRecommendations: (userId: number, signal?: AbortSignal) => 
        apiClient<Recommendation[]>(`/api/analytics/users/${userId}/recommendations`, { signal }),
        
    getGlobalDashboard: (signal?: AbortSignal) => 
        apiClient<GlobalInsights>(`/api/analytics/global-dashboard`, { signal }),
        
    getUserDashboard: (userId: number, signal?: AbortSignal) => 
        apiClient<UserInsights>(`/api/analytics/users/${userId}/dashboard`, { signal }),
        
    updateInterests: (userId: number, interest: string) => 
        apiClient(`/api/users/${userId}/interests`, { method: 'POST', body: JSON.stringify({ interest }) }),
        
    toggleStrictMode: (userId: number, enabled: boolean) => 
        apiClient(`/api/users/${userId}/settings/strict-filtering?enabled=${enabled}`, { method: 'PUT' }),
        
    toggleDailySummary: (userId: number, enabled: boolean) => 
        apiClient(`/api/users/${userId}/settings/daily-summary?enabled=${enabled}`, { method: 'PUT' }),
        
    toggleWeeklySummary: (userId: number, enabled: boolean) => 
        apiClient(`/api/users/${userId}/settings/weekly-summary?enabled=${enabled}`, { method: 'PUT' }),
        
    changeLanguage: (userId: number, lang: string) => 
        apiClient(`/api/users/${userId}/settings/language?lang=${lang}`, { method: 'PUT' }),
        
    toggleReadAll: (userId: number, sourceId: number, readAll: boolean) => 
        apiClient(`/api/users/${userId}/sources/${sourceId}/read-all?readAll=${readAll}`, { method: 'PUT' }),
        
    addSource: (userId: number, source: string) => 
        apiClient(`/api/users/${userId}/sources`, { method: 'POST', body: JSON.stringify({ source }) }),
        
    removeSource: (userId: number, url: string) => 
        apiClient(`/api/users/${userId}/sources?url=${encodeURIComponent(url)}`, { method: 'DELETE' }),
        
    getReports: (moderatorId: number) => 
        apiClient(`/api/reports?moderatorId=${moderatorId}`),
        
    updateReportStatus: (reportId: number, status: string, moderatorId: number) => 
        apiClient(`/api/reports/${reportId}/status?status=${status}&moderatorId=${moderatorId}`, { method: 'PATCH' }),
        
    submitReport: (data: any) => 
        apiClient(`/api/reports`, { method: 'POST', body: JSON.stringify(data) }),

    updateUserRole: (adminId: number, userId: number, role: 'USER' | 'MODERATOR' | 'ADMIN') => 
        apiClient(`/api/admin/users/${userId}/role?role=${role}&adminId=${adminId}`, { method: 'PUT' }),
        
    getAllGlobalSources: (adminId: number) => 
        apiClient(`/api/admin/sources?adminId=${adminId}`),

    addGlobalSource: (adminId: number, url: string, type: 'TELEGRAM' | 'RSS', name?: string, trustLevel?: string) => {
        let endpoint = `/api/admin/sources?url=${encodeURIComponent(url)}&type=${type}&adminId=${adminId}`;
        if (name) endpoint += `&name=${encodeURIComponent(name)}`;
        if (trustLevel) endpoint += `&trustLevel=${trustLevel}`;
        return apiClient(endpoint, { method: 'POST' });
    },
        
    deleteGlobalSource: (adminId: number, sourceId: number) => 
        apiClient(`/api/admin/sources/${sourceId}?adminId=${adminId}`, { method: 'DELETE' })
};