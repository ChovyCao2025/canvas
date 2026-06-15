package org.chovy.canvas.canvas.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.HomeOverviewFacade;
import org.springframework.stereotype.Service;

@Service
public class HomeOverviewApplicationService implements HomeOverviewFacade {

    @Override
    public HomeOverviewView overview(int days) {
        LocalDate until = LocalDate.now();
        LocalDate since = until.minusDays(days - 1L);
        return new HomeOverviewView(
                new RangeView(days, since.toString(), until.toString()),
                new SummaryView(0L, 0L, 0L, 0L, "0%"),
                emptyTrend(since, until),
                List.of(),
                List.of(noRecentExecutionsItem()));
    }

    private static List<TrendPointView> emptyTrend(LocalDate since, LocalDate until) {
        List<TrendPointView> trend = new ArrayList<>();
        LocalDate cursor = since;
        while (!cursor.isAfter(until)) {
            trend.add(new TrendPointView(cursor.toString(), 0L, 0L));
            cursor = cursor.plusDays(1);
        }
        return trend;
    }

    private static AttentionItemView noRecentExecutionsItem() {
        return new AttentionItemView(
                0L,
                "全部旅程",
                "NO_RECENT_EXECUTIONS",
                "最近暂无执行记录",
                "info");
    }
}
