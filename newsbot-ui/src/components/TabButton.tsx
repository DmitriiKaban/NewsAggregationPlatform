import React from 'react';
import type { ThemeColors } from '../types';

interface Props {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
    colors: ThemeColors;
    icon?: React.ReactNode;
}

export const TabButton = ({ active, onClick, children, colors, icon }: Props) => {
    const cleanChildren = typeof children === 'string' 
        ? children.replace(/[\p{Emoji_Presentation}\p{Extended_Pictographic}]/gu, '').trim() 
        : children;

    return (
        <button onClick={onClick} style={{
            flex: '1 1 0',
            minWidth: 0,
            padding: '8px 4px',
            background: active ? colors.bg : 'transparent', 
            border: 'none',
            borderRadius: '10px', 
            color: active ? colors.text : colors.hint, 
            fontSize: '12px',
            fontWeight: '700',
            cursor: 'pointer', 
            transition: 'all 0.21s cubic-bezier(0.175, 0.885, 0.32, 1.275)', 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center',
            gap: '4px',
            boxShadow: active ? '0 2px 8px rgba(0,0,0,0.05)' : 'none'
        }}>
            {icon && <span style={{ display: 'flex', alignItems: 'center', flexShrink: 0 }}>{icon}</span>}
            <span style={{ display: 'block', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {cleanChildren}
            </span>
        </button>
    );
};