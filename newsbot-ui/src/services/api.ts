import { apiClient } from './apiClient';
import type { Recommendation, GlobalInsights, UserInsights } from '../types';

export const api = {
    getUserProfile: (userId: number, signal?: AbortSignal) => 
        apiClient(`/users/${userId}/profile`, { signal }),
        
    getRecommendations: (userId: number, signal?: AbortSignal) => 
        apiClient<Recommendation[]>(`/analytics/users/${userId}/recommendations`, { signal }),
        
    getGlobalDashboard: (signal?: AbortSignal) => 
        apiClient<GlobalInsights>(`/analytics/global-dashboard`, { signal }),
        
    getUserDashboard: (userId: number, signal?: AbortSignal) => 
        apiClient<UserInsights>(`/analytics/users/${userId}/dashboard`, { signal }),
        
    updateInterests: (userId: number, interest: string) => 
        apiClient(`/users/${userId}/interests`, { method: 'POST', body: JSON.stringify({ interest }) }),
        
    toggleStrictMode: (userId: number, enabled: boolean) => 
        apiClient(`/users/${userId}/settings/strict-filtering?enabled=${enabled}`, { method: 'PUT' }),
        
    toggleDailySummary: (userId: number, enabled: boolean) => 
        apiClient(`/users/${userId}/settings/daily-summary?enabled=${enabled}`, { method: 'PUT' }),
        
    toggleWeeklySummary: (userId: number, enabled: boolean) => 
        apiClient(`/users/${userId}/settings/weekly-summary?enabled=${enabled}`, { method: 'PUT' }),
        
    changeLanguage: (userId: number, lang: string) => 
        apiClient(`/users/${userId}/settings/language?lang=${lang}`, { method: 'PUT' }),
        
    toggleReadAll: (userId: number, sourceId: number, readAll: boolean) => 
        apiClient(`/users/${userId}/sources/${sourceId}/read-all?readAll=${readAll}`, { method: 'PUT' }),
        
    addSource: (userId: number, source: string) => 
        apiClient(`/users/${userId}/sources`, { method: 'POST', body: JSON.stringify({ source }) }),
        
    removeSource: (userId: number, url: string) => 
        apiClient(`/users/${userId}/sources?url=${encodeURIComponent(url)}`, { method: 'DELETE' }),
        
    getReports: (moderatorId: number) => 
        apiClient(`/reports?moderatorId=${moderatorId}`),
        
    updateReportStatus: (reportId: number, status: string, moderatorId: number) => 
        apiClient(`/reports/${reportId}/status?status=${status}&moderatorId=${moderatorId}`, { method: 'PATCH' }),
        
    submitReport: (data: any) => 
        apiClient(`/reports`, { method: 'POST', body: JSON.stringify(data) })
};