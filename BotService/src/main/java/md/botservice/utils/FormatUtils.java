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

    public static String escapeMarkdownV2(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }
}
