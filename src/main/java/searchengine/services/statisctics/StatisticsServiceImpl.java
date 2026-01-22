package searchengine.services.statisctics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesCfgList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesCfgList sitesCfgList;

    @Override
    public StatisticsResponse getStatistics() {
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesCfgList.getSiteCfgs().size());

        long totalPages = 0;
        long totalLemmas = 0;
        boolean anyIndexing = false;

        for (var cfg : sitesCfgList.getSiteCfgs()) {
            String url = cfg.getUrl();
            Site site = siteRepository.findByUrl(url).orElse(null);

            long pageCount = 0;
            long lemmaCount = 0;
            String status = "NOT_INDEXED";
            String error = "";
            long statusTimeMillis = 0L;

            if (site != null) {
                pageCount = pageRepository.countBySite(site);
                lemmaCount = lemmaRepository.findBySite(site).stream().count();
                status = site.getStatus() != null ? site.getStatus().name() : "NOT_INDEXED";
                error = site.getLastError() != null ? site.getLastError() : "";
                if (site.getStatusTime() != null) {
                    statusTimeMillis = site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
                if ("INDEXING".equals(status)) {
                    anyIndexing = true;
                }
            }

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(cfg.getName());
            item.setUrl(url);
            item.setPages((int) pageCount);
            item.setLemmas((int) lemmaCount);
            item.setStatus(status);
            item.setError(error);
            item.setStatusTime(statusTimeMillis);

            totalPages += pageCount;
            totalLemmas += lemmaCount;

            detailed.add(item);
        }

        total.setPages((int) totalPages);
        total.setLemmas((int) totalLemmas);
        total.setIndexing(anyIndexing);

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }
}
