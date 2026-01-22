package searchengine.services.index;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesCfgList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;
import searchengine.services.crawl.CrawlerService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesCfgList sitesCfgList;
    private final SiteRepository siteRepository;
    private final CrawlerService crawlerService;

    private final AtomicBoolean indexing = new AtomicBoolean(false);

    @Override
    public boolean startIndexing() {
        if (!indexing.compareAndSet(false, true)) {
            return false; // Индексация уже запущена
        }

        List<SiteCfg> sites = sitesCfgList.getSiteCfgs();

        for (SiteCfg cfg : sites) {
            Site site = siteRepository.findByUrl(cfg.getUrl())
                    .orElseGet(() -> {
                        Site newSite = new Site();
                        newSite.setUrl(cfg.getUrl());
                        newSite.setName(cfg.getName());
                        newSite.setStatus(Status.INDEXING);
                        newSite.setStatusTime(LocalDateTime.now());
                        return siteRepository.save(newSite);
                    });

            // Запуск обхода сайта в отдельном потоке
            new Thread(() -> crawlerService.crawlSite(site)).start();
        }

        indexing.set(false);
        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (indexing.get()) {
            sitesCfgList.getSiteCfgs().forEach(cfg -> {
                siteRepository.findByUrl(cfg.getUrl()).ifPresent(site -> {
                    // Остановить обход страниц
                    crawlerService.stopCrawling();

                    // Обновить статус сайта
                    site.setStatus(Status.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                });
            });

            indexing.set(false);
            return true;
        }
        return false; // Индексация не запущена
    }

    @Override
    public boolean indexPage(String url) {
        Site site = siteRepository.findByUrl(url).orElse(null);
        if (site != null) {
            new Thread(() -> crawlerService.crawlSite(site)).start();
            return true;
        }
        return false;
    }
}
