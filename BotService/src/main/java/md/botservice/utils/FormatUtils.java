package md.botservice.utils;

public class FormatUtils {

    public static String normalizeTelegramUrl(String input) {
        if (input == null) return "";
        String clean = input.trim();

        if (clean.startsWith("http")) {
            return clean;
        }

        if (clean.startsWith("@")) {
            clean = clean.substring(1);
        }

        return "https://t.me/s/" + clean;
    }

    public static String getSimpleTelegramName(String url) {
        if (url == null || url.trim().isEmpty()) return "";

        String clean = url.trim();

        if (clean.startsWith("@")) {
            return clean;
        }

        clean = clean.replace("https://", "")
                .replace("http://", "");

        if (clean.startsWith("t.me/")) {
            clean = clean.substring(5);
        }

        if (clean.startsWith("s/")) {
            clean = clean.substring(2);
        }

        int endIdx = clean.indexOf('?');
        if (endIdx == -1) endIdx = clean.indexOf('#');
        if (endIdx != -1) {
            clean = clean.substring(0, endIdx);
        }

        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }

        return clean.isEmpty() ? "" : "@" + clean;
    }


    public static String escapeMarkdownV2(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }
}
