INSERT INTO sources (name, url, type, trust_level, is_active)
VALUES
    -- MOLDOVA
    ('Moldpres', 'https://www.moldpres.md/config/rss.php?lang=rom', 'RSS', 'OFFICIAL', true),
    ('Ziarul de Gardă', 'https://t.me/zdgmd', 'TELEGRAM', 'VERIFIED_MEDIA', true),
    ('Agora.md', 'https://t.me/agoramd', 'TELEGRAM', 'VERIFIED_MEDIA', true),
    ('Point News', 'https://t.me/PointNews', 'TELEGRAM', 'VERIFIED_MEDIA', true),
    ('Chisinau vteme', 'https://t.me/vtememd', 'TELEGRAM', 'USER_GENERATED_CONTENT', true),

    -- WORLD NEWS
    ('BBC World', 'https://feeds.bbci.co.uk/news/world/rss.xml', 'RSS', 'VERIFIED_MEDIA', true),
    ('The Guardian', 'https://www.theguardian.com/world/rss', 'RSS', 'VERIFIED_MEDIA', true),
    ('Al Jazeera', 'https://www.aljazeera.com/xml/rss/all.xml', 'RSS', 'VERIFIED_MEDIA', true),

    -- FINANCE & BUSINESS
    ('CNBC International', 'https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=10000664', 'RSS',
     'VERIFIED_MEDIA', true),
    ('Fortune', 'https://fortune.com/feed/', 'RSS', 'VERIFIED_MEDIA', true),

    -- SCIENCE & NATURE
    ('Nature Journal', 'http://www.nature.com/nature/current_issue/rss/', 'RSS', 'VERIFIED_MEDIA', true),
    ('Science Daily', 'https://www.sciencedaily.com/rss/top/science.xml', 'RSS', 'VERIFIED_MEDIA', true),
    ('TechCrunch', 'https://techcrunch.com/feed/', 'RSS', 'VERIFIED_MEDIA', true),
    ('The Verge', 'https://www.theverge.com/rss/index.xml', 'RSS', 'VERIFIED_MEDIA', true),

    -- TED TALKS
    ('TED Talks', 'https://www.ted.com/talks/rss', 'RSS', 'VERIFIED_MEDIA', true),

    -- SPORT
    ('Motorsport.com (F1)', 'https://www.motorsport.com/rss/f1/news/', 'RSS', 'VERIFIED_MEDIA', true),
    ('ESPN', 'https://www.espn.com/espn/rss/news', 'RSS', 'VERIFIED_MEDIA', true),

    -- FASHION & LIFESTYLE
    ('Vogue', 'https://www.vogue.com/feed/rss', 'RSS', 'VERIFIED_MEDIA', true),
    ('Elle Romania', 'https://www.elle.ro/feed/', 'RSS', 'VERIFIED_MEDIA', true),
    ('Business of Fashion', 'https://www.businessoffashion.com/feeds/all', 'RSS', 'VERIFIED_MEDIA', true),

    -- HEALTH
    ('Medical News Today', 'https://www.medicalnewstoday.com/feed', 'RSS', 'VERIFIED_MEDIA', true),

    -- FUN
    ('Programmer Jokes', 'https://t.me/programmerjokes', 'TELEGRAM', 'USER_GENERATED_CONTENT',
     true) ON CONFLICT (url) DO NOTHING;