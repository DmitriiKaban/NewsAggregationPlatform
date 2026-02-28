package md.botservice.dto;

public record SourceRecommendationDto(String name, String url, Integer peerCount) {
    public static SourceRecommendationDto from(SourceRecommendationProjection p) {
        return new SourceRecommendationDto(p.getName(), p.getUrl(), p.getPeerCount());
    }
}
