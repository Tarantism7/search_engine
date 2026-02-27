package searchengine.services.crawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.CrawlerCfg;
import searchengine.model.*;
import searchengine.repositories.*;

import searchengine.services.lemma.LemmaServiceImpl;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class CrawlerService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SearchIndexRepository indexRepository;
    private final LemmaServiceImpl lemmaService;

    private volatile boolean stopFlag = false;
    private volatile ForkJoinPool currentPool = null;

    public CrawlerService(SiteRepository siteRepository,
                          PageRepository pageRepository,
                          LemmaRepository lemmaRepository,
                          SearchIndexRepository indexRepository,
                          LemmaServiceImpl lemmaService) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.lemmaService = lemmaService;
    }

    public void stopCrawling() {
        stopFlag = true;
        ForkJoinPool pool = currentPool;
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
        }
    }

    public void crawlSite(Site site) {
        stopFlag = false;
        ForkJoinPool pool = new ForkJoinPool();
        currentPool = pool;
        try {
            pool.invoke(new CrawlTask(site, site.getUrl(), new HashSet<>()));
        } finally {
            pool.shutdown();
            currentPool = null;
        }
    }

    public void crawlPage(String pageUrl, Site site) {
        stopFlag = false;
        ForkJoinPool pool = new ForkJoinPool();
        currentPool = pool;
        try {
            pool.invoke(new CrawlTask(site, pageUrl, new HashSet<>()));
        } finally {
            pool.shutdown();
            currentPool = null;
        }
    }

    private class CrawlTask extends RecursiveAction {
        private final Site site;
        private final String url;
        private final Set<String> visited;

        public CrawlTask(Site site, String url, Set<String> visited) {
            this.site = site;
            this.url = url;
            this.visited = visited;
        }

        @Override
        protected void compute() {
            if (stopFlag || visited.contains(url)) return;

            try {
                Document doc = Jsoup.connect(url)
                        .userAgent(CrawlerCfg.USER_AGENT)
                        .referrer(CrawlerCfg.REFERRER)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.5")
                        .header("Accept-Encoding", "gzip, deflate")
//                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .timeout(CrawlerCfg.TIMEOUT)
                        .followRedirects(true)
                        .get();

                if (stopFlag) return;

                int statusCode = doc.connection().response().statusCode();
                
                // Сохраняем страницу
                String path = new URL(url).getPath();
                Optional<Page> existingOpt = pageRepository.findByPathAndSite(path, site);
                boolean existed = existingOpt.isPresent();
                Page page = existed ? existingOpt.get() : new Page();
                page.setSite(site);
                page.setPath(path);
                page.setCode(statusCode);
                page.setContent(doc.html());
                page = pageRepository.save(page);

                visited.add(url);

                // Only index content if status is successful
                if (statusCode >= 400) return;

                if (stopFlag) return;

                // Если страница уже была в БД — удаляем старые индексы и уменьшаем частоты лемм
                if (existed) {
                    List<SearchIndex> oldIndices = indexRepository.findByPage(page);
                    for (SearchIndex oldIndex : oldIndices) {
                        if (stopFlag) return;
                        // Fetch fresh lemma from repository to avoid LazyInitializationException in ForkJoinPool threads
                        Lemma oldLemma = oldIndex.getLemma();
                        if (oldLemma != null) {
                            Optional<Lemma> freshLemma = lemmaRepository.findById(oldLemma.getId());
                            if (freshLemma.isPresent()) {
                                Lemma lemma = freshLemma.get();
                                int newFreq = Math.max(0, lemma.getFrequency() - 1);
                                lemma.setFrequency(newFreq);
                                lemmaRepository.save(lemma);
                            }
                        }
                        indexRepository.delete(oldIndex);
                    }
                }

                if (stopFlag) return;

                // Индексация страницы: получаем леммы
                Map<String, Integer> lemmas = lemmaService.extractLemmas(doc.html());
                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    if (stopFlag) return;

                    String lemmaText = entry.getKey();
                    int count = entry.getValue();

                    // Получаем или создаём лемму
                    Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaText, site)
                            .orElseGet(() -> {
                                Lemma l = new Lemma();
                                l.setLemma(lemmaText);
                                l.setFrequency(0);
                                l.setSite(site);
                                return l;
                            });

                    // Сохраняем лемму сначала (чтобы она была персистирована)
                    lemma = lemmaRepository.save(lemma);

                    // Проверяем, есть ли уже индекс для этой страницы и леммы
                    Optional<SearchIndex> existingIndex = indexRepository.findByPageAndLemma(page, lemma);

                    // Увеличиваем frequency только если индекса не было
                    if (existingIndex.isEmpty()) {
                        lemma.setFrequency(lemma.getFrequency() + 1);
                        lemma = lemmaRepository.save(lemma);
                    }

                    // Сохраняем/обновляем индекс для страницы
                    SearchIndex index = existingIndex.orElseGet(SearchIndex::new);
                    index.setPage(page);
                    index.setLemma(lemma);
                    index.setRank(count);
                    indexRepository.save(index);
                }

                visited.add(url);

                if (stopFlag) return;

                // Получаем ссылки со страницы
                Elements links = doc.select("a[href]");
                List<CrawlTask> subTasks = new ArrayList<>();
                for (var link : links) {
                    if (stopFlag) return;
                    String absUrl = link.absUrl("href");
                    if (absUrl.startsWith(site.getUrl()) && !visited.contains(absUrl)) {
                        subTasks.add(new CrawlTask(site, absUrl, visited));
                    }
                }
                
                if (stopFlag) return;
                
                // Параллельный вызов
                invokeAll(subTasks);

                if (stopFlag) return;

                // Задержка между запросами 0.5-1 сек
                Thread.sleep(500 + new Random().nextInt(500));

            } catch (Exception e) {
                e.printStackTrace();
                // Throw exception so IndexingServiceImpl can handle it properly
                throw new RuntimeException(e);
            }
        }
    }
}
