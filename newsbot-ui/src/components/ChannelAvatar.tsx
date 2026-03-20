import { useEffect, useState } from 'react';
import { getAvatarColor, getHandle, getInitials } from '../utils/helpers';

interface Props {
    url: string;
    name?: string;
    size?: number;
    fontSize?: number;
    apiBaseUrl: string;
}

export const ChannelAvatar = ({ url, name, size = 48, fontSize = 18, apiBaseUrl }: Props) => {
    const [imgUrl, setImgUrl] = useState<string | null>(null);
    const [imgError, setImgError] = useState(false);
    const handle = getHandle(url).replace('@', '');

    useEffect(() => {
        if (!handle) return;
        
        fetch(`${apiBaseUrl}/sources/avatar?handle=${handle}`, {
            headers: {"ngrok-skip-browser-warning": "69420"}
        })
        .then(r => {
            if (!r.ok) throw new Error();
            return r.json();
        })
        .then(data => {
            if (data && data.url) setImgUrl(data.url);
        })
        .catch(() => setImgError(true));
    }, [handle, apiBaseUrl]);

    if (imgUrl && !imgError) {
        return (
            <img
                src={imgUrl}
                alt={name || handle}
                style={{ width: size, height: size, borderRadius: '50%', objectFit: 'cover', flexShrink: 0, backgroundColor: '#f0f0f0' }}
                onError={() => setImgError(true)}
            />
        );
    }

    return (
        <div style={{ width: size, height: size, borderRadius: '50%', background: getAvatarColor(name || url), color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: fontSize, fontWeight: '700', flexShrink: 0 }}>
            {getInitials(name || url)}
        </div>
    );
};