declare global {
    interface Window {
        Telegram?: {
            WebApp: {
                ready: () => void;
                close: () => void;
                sendData: (data: string) => void;
                expand: () => void;
                showAlert?: (message: string, callback?: () => void) => void;
                showPopup?: (params: { message: string; buttons?: any[] }, callback?: (buttonId: string) => void) => void;
                initData?: string;
                initDataUnsafe?: {
                    user?: {
                        id?: number;
                        first_name?: string;
                        last_name?: string;
                        username?: string;
                        language_code?: string;
                    };
                };
                themeParams?: {
                    bg_color?: string;
                    text_color?: string;
                    hint_color?: string;
                    link_color?: string;
                    button_color?: string;
                    button_text_color?: string;
                    secondary_bg_color?: string;
                };
                MainButton: {
                    setText: (text: string) => void;
                    show: () => void;
                    hide: () => void;
                    enable: () => void;
                    disable?: () => void;
                    showProgress?: (leaveActive?: boolean) => void;
                    hideProgress?: () => void;
                    onClick: (callback: () => void) => void;
                    offClick: (callback: () => void) => void;
                };
                BackButton: {
                    show: () => void;
                    hide: () => void;
                    onClick: (callback: () => void) => void;
                    offClick: (callback: () => void) => void;
                };
            };
        };
    }
}

export interface Source {
    id: number;
    url: string;
    isReadAll: boolean;
    name?: string;
}

export interface Recommendation {
    name: string;
    url: string;
    peerCount: number;
}

export interface TopSource {
    name: string;
    url: string;
    subscriberCount: number;
}

export interface DauData {
    date: string;
    count: number;
}

export interface TopicStat {
    topic: string;
    readCount: number;
}

export interface SourceFeedback {
    sourceName: string;
    likes: number;
    dislikes: number;
}

export interface SourceAdoption {
    sourceName: string;
    userCount: number;
}

export interface GlobalInsights {
    dauStats: DauData[];
    topSources: TopSource[];
    strictModeAdoptionPercent: number;
    avgArticlesPerSession: number;
    avgTopicDiversityEntropy: number;
    topReadAllSources: SourceAdoption[];
    topGlobalTopics: TopicStat[];
    sourceFeedback: SourceFeedback[];
}

export interface UserInsights {
    totalArticlesRead?: number;
    clickThroughRate?: number;
    avgArticlesPerSession: number;
    avgTopicDiversityEntropy: number;
}

export interface ThemeColors {
    bg: string;
    text: string;
    hint: string;
    link: string;
    button: string;
    buttonText: string;
    secondaryBg: string;
    success: string;
    danger: string;
    chartBar: string;
}