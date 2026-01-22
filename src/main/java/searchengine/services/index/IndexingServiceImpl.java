package searchengine.services.index;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteCfg;
import searchengine.config.SitesCfgList;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.crawl.CrawlerService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesCfgList sitesCfgList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final CrawlerService crawlerService;

    private final AtomicBoolean indexing = new AtomicBoolean(false);
    private final List<Thread> crawlerThreads = new CopyOnWriteArrayList<>();

    @Override
    public boolean startIndexing() {
        if (!indexing.compareAndSet(false, true)) {
            return false;
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

            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(null);
            siteRepository.save(site);

            Thread crawlerThread = new Thread(() -> {
                try {
                    crawlerService.crawlSite(site);
                    if (!indexing.get()) {
                        site.setStatus(Status.FAILED);
                        site.setLastError("Индексация остановлена пользователем");
                    } else {
                        // Check if any pages were actually indexed
                        int pageCount = Math.toIntExact(pageRepository.countBySite(site));
                        if (pageCount == 0) {
                            site.setStatus(Status.FAILED);
                            site.setLastError("Сайт не был проиндексирован (нет доступных страниц)");
                        } else {
                            site.setStatus(Status.INDEXED);
                            site.setLastError(null);
                        }
                    }
                } catch (Exception e) {
                    site.setStatus(Status.FAILED);
                    site.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            });
            crawlerThread.start();
            crawlerThreads.add(crawlerThread);
        }

        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (!indexing.getAndSet(false)) {
            return false;
        }

        crawlerService.stopCrawling();

        sitesCfgList.getSiteCfgs().forEach(cfg -> {
            siteRepository.findByUrl(cfg.getUrl()).ifPresent(site -> {
                // Only mark as stopped if it was still being indexed
                if (site.getStatus() == Status.INDEXING) {
                    site.setStatus(Status.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }
            });
        });

        crawlerThreads.forEach(thread -> {
            try {
                thread.interrupt();
                thread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        crawlerThreads.clear();

        return true;
    }

    @Override
    public boolean indexPage(String url) {
        Site site = siteRepository.findByUrl(url).orElse(null);
        if (site == null) {
            return false;
        }

        new Thread(() -> crawlerService.crawlPage(url, site)).start();
        return true;
    }
}
