import React from 'react';
import type { ThemeColors } from '../types';

interface Props {
    active: boolean;
    onClick: () => void;
    children: React.ReactNode;
    colors: ThemeColors;
    icon?: React.ReactNode;
    variant?: 'tab' | 'menu';
}

export const TabButton = ({ active, onClick, children, colors, icon, variant = 'tab' }: Props) => {
    const cleanChildren = typeof children === 'string' 
        ? children.replace(/[\p{Emoji_Presentation}\p{Extended_Pictographic}]/gu, '').trim() 
        : children;

    const isMenu = variant === 'menu';

    return (
        <button onClick={onClick} style={{
            flex: isMenu ? 'none' : '1 1 auto',
            width: isMenu ? '100%' : 'auto',
            padding: isMenu ? '12px 16px' : '8px 12px',
            background: active ? colors.bg : 'transparent', 
            border: 'none',
            borderRadius: '10px', 
            color: active ? colors.text : colors.hint, 
            fontSize: isMenu ? '15px' : '13px',
            fontWeight: '700',
            cursor: 'pointer', 
            transition: 'all 0.2s ease', 
            display: 'flex', 
            justifyContent: isMenu ? 'flex-start' : 'center', 
            alignItems: 'center',
            gap: '12px',
            boxShadow: active && !isMenu ? '0 2px 8px rgba(0,0,0,0.05)' : 'none',
            whiteSpace: 'nowrap',
            WebkitTapHighlightColor: 'transparent'
        }}>
            {icon && (
                <span style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    flexShrink: 0, 
                    color: active ? colors.button : 'inherit',
                    transition: 'color 0.2s'
                }}>
                    {icon}
                </span>
            )}
            <span style={{ display: 'block' }}>
                {cleanChildren}
            </span>
        </button>
    );
};