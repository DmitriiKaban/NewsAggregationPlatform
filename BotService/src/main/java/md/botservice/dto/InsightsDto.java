package md.botservice.dto;

import java.util.List;

public record InsightsDto(
        List<DauDto> dauStats,
        List<TopSourceDto> topSources
) {

    public static InsightsDto of(List<DauProjection> dauProjections, List<TopSourceProjection> topSourceProjections) {
        return new InsightsDto(
                dauProjections.stream()
                        .map(p -> new DauDto(p.getDate(), p.getCount()))
                        .toList(),

                topSourceProjections.stream()
                        .map(p -> new TopSourceDto(p.getName(), p.getUrl(), p.getSubscriberCount()))
                        .toList()
        );
    }

}