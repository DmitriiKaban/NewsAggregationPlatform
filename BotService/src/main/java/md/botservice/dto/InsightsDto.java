package md.botservice.dto;

import java.util.List;

public record InsightsDto(List<TopSourceDto> topSources, List<DauDto> dauStats) {

    public static InsightsDto of(List<TopSourceProjection> topSourceProjections, List<DauProjection> dauProjections) {
        List<TopSourceDto> sourceDtos = topSourceProjections.stream()
                .map(p -> new TopSourceDto(p.getName(), p.getSubscriberCount()))
                .toList();
        List<DauDto> dauDtos = dauProjections.stream()
                        .map(p -> new DauDto(p.getDate(), p.getCount()))
                        .toList();
        return new InsightsDto(sourceDtos, dauDtos);
    }

}
