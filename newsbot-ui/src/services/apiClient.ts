import { ENV } from '../config/env';

const defaultHeaders = {
    "ngrok-skip-browser-warning": "69420",
    "Content-Type": "application/json"
};

export const apiClient = async <T = any>(endpoint: string, options: RequestInit = {}): Promise<T> => {
    const response = await fetch(`${ENV.API_BASE_URL}${endpoint}`, {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers,
        },
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        const errorMessage = errorData?.message || errorData?.error || errorData?.detail || `HTTP error ${response.status}`;
        throw new Error(errorMessage);
    }

    const text = await response.text();
    return text ? JSON.parse(text) : ({} as T);
};