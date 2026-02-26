package md.botservice.models;

public enum Language {
    EN("en", "🇬🇧 English"),
    RO("ro", "🇷🇴 Română"),
    RU("ru", "🇷🇺 Русский");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equals(code)) {
                return lang;
            }
        }
        return null;
    }
}
