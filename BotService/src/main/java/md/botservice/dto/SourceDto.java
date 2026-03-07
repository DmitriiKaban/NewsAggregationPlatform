package md.botservice.dto;

import md.botservice.utils.FormatUtils;

public record SourceDto (Long id, String name, String url, boolean isReadAll) {

    public static SourceDto of(Long id, String name, String url, boolean contains) {
        String channelName = FormatUtils.getSimpleTelegramName(url);
        return new SourceDto(id, name, channelName, contains);
    }

}