import unittest
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta, timezone
import sys

mock_prometheus = MagicMock()
mock_prometheus.Histogram.return_value.time.return_value = lambda f: f
mock_prometheus.Gauge.return_value.track_inprogress.return_value = lambda f: f

sys.modules['kafka'] = MagicMock()
sys.modules['kafka.errors'] = MagicMock()
sys.modules['redis'] = MagicMock()
sys.modules['psycopg2'] = MagicMock()
sys.modules['telethon'] = MagicMock()
sys.modules['telethon.sync'] = MagicMock()
sys.modules['prometheus_client'] = mock_prometheus

import scraper_main

class TestScraperMain(unittest.TestCase):

    def test_clean_text(self):
        text = "ăâîșț ĂÂÎȘȚ „”’"
        expected = "aaist AAIST \"\"'"
        self.assertEqual(scraper_main.clean_text(text), expected)
        self.assertEqual(scraper_main.clean_text(None), "")

    def test_is_recent(self):
        now = datetime.now(timezone.utc)
        recent_date = now - timedelta(hours=10)
        old_date = now - timedelta(hours=30)
        
        self.assertTrue(scraper_main.is_recent(recent_date, 24))
        self.assertFalse(scraper_main.is_recent(old_date, 24))
        
        naive_date = datetime.now().replace(tzinfo=None) - timedelta(hours=10)
        self.assertTrue(scraper_main.is_recent(naive_date, 24))

    @patch('scraper_main.redis_client')
    def test_redis_functions(self, mock_redis):
        mock_redis.exists.return_value = True
        self.assertTrue(scraper_main.is_processed("http://test.com"))
        
        scraper_main.mark_processed("http://test.com")
        mock_redis.set.assert_called_with("http://test.com", "1", ex=86400)

    @patch('scraper_main.requests.get')
    def test_extract_full_text(self, mock_get):
        mock_response = MagicMock()
        mock_response.content = "<html><body><p>This is a very long paragraph that serves to exceed the minimum character limit of fifty characters required by the scraper logic.</p></body></html>"
        mock_get.return_value = mock_response
        
        result = scraper_main.extract_full_text("http://test.com")
        self.assertIn("very long paragraph", result)

        mock_response.content = "<html><body><p>Too short</p></body></html>"
        result = scraper_main.extract_full_text("http://test.com")
        self.assertEqual(result, "")
        
        mock_get.side_effect = Exception("Error")
        result = scraper_main.extract_full_text("http://test.com")
        self.assertEqual(result, "")

    @patch('scraper_main.psycopg2.connect')
    def test_get_sources_from_db(self, mock_connect):
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_connect.return_value = mock_conn
        mock_conn.cursor.return_value = mock_cur
        
        mock_cur.fetchall.return_value = [
            ("RSS Source", "http://rss.com", "rss"),
            ("TG Source", "https://t.me/channel", "telegram")
        ]
        
        sources = scraper_main.get_sources_from_db()
        self.assertEqual(len(sources), 2)
        self.assertEqual(sources[0]['type'], 'rss')

        mock_connect.side_effect = Exception("DB Error")
        self.assertEqual(scraper_main.get_sources_from_db(), [])

    @patch('scraper_main.producer')
    @patch('scraper_main.mark_processed')
    @patch('scraper_main.ARTICLES_SENT')
    def test_send_to_kafka(self, mock_counter, mock_mark, mock_producer):
        date_obj = datetime.now(timezone.utc)
        scraper_main.send_to_kafka("source", "title", "link", "summary", "full text", date_obj)
        
        mock_producer.send.assert_called()
        mock_mark.assert_called_with("link")

    @patch('scraper_main.is_processed')
    @patch('scraper_main.extract_full_text')
    @patch('scraper_main.send_to_kafka')
    def test_process_single_entry(self, mock_send, mock_extract, mock_processed):
        entry = MagicMock()
        entry.link = "http://test.com"
        entry.published = datetime.now(timezone.utc).isoformat()
        entry.title = "Test"
        entry.get.return_value = "Summary"

        mock_processed.return_value = False
        mock_extract.return_value = "Long enough content to pass the character count check"
        
        res = scraper_main.process_single_entry(entry, "source")
        self.assertEqual(res, 1)
        mock_send.assert_called()

    @patch('scraper_main.requests.get')
    @patch('scraper_main.feedparser.parse')
    @patch('scraper_main.process_single_entry')
    def test_scrape_rss(self, mock_process, mock_parse, mock_get):
        source = {"name": "RSS", "url": "http://rss.com"}
        mock_feed = MagicMock()
        mock_feed.entries = [MagicMock()]
        mock_parse.return_value = mock_feed
        mock_process.return_value = 1
        
        self.assertEqual(scraper_main.scrape_rss(source), 1)

    @patch('scraper_main.is_processed')
    @patch('scraper_main.send_to_kafka')
    def test_scrape_telegram(self, mock_send, mock_processed):
        client = MagicMock()
        source = {"name": "TG", "channel": "test", "filter_hours": 24}
        post = MagicMock()
        post.message = "Message"
        post.id = 1
        post.date = datetime.now(timezone.utc)
        
        client.get_messages.return_value = [post]
        mock_processed.return_value = False
        
        self.assertEqual(scraper_main.scrape_telegram(client, source), 1)

    @patch('scraper_main.get_sources_from_db')
    @patch('scraper_main.scrape_rss')
    @patch('scraper_main.TelegramClient')
    def test_job(self, mock_tg, mock_rss, mock_db):
        mock_db.return_value = [{"name": "RSS", "type": "rss", "url": "url"}]
        mock_rss.return_value = 1
        scraper_main.job()
        mock_rss.assert_called()

if __name__ == '__main__':
    unittest.main()