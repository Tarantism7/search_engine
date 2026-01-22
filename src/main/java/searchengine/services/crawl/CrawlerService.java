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
    }

    public void crawlSite(Site site) {
        stopFlag = false;
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new CrawlTask(site, site.getUrl(), new HashSet<>()));
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
                        .timeout(CrawlerCfg.TIMEOUT)
                        .get();

                int statusCode = doc.connection().response().statusCode();
                if (statusCode >= 400) return;

                // Сохраняем страницу
                String path = new URL(url).getPath();
                Page page = pageRepository.findByPathAndSite(path, site)
                        .orElse(new Page());
                page.setSite(site);
                page.setPath(path);
                page.setCode(statusCode);
                page.setContent(doc.html());
                page = pageRepository.save(page);

                // Индексация страницы: получаем леммы
                Map<String, Integer> lemmas = lemmaService.extractLemmas(doc.html());
                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    String lemmaText = entry.getKey();
                    int count = entry.getValue();

                    // Сохраняем лемму
                    Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaText, site)
                            .orElseGet(() -> {
                                Lemma l = new Lemma();
                                l.setLemma(lemmaText);
                                l.setFrequency(0);
                                l.setSite(site);
                                return l;
                            });
                    lemma.setFrequency(lemma.getFrequency() + 1);
                    lemma = lemmaRepository.save(lemma);

                    // Сохраняем индекс
                    SearchIndex index = new SearchIndex();
                    index.setPage(page);
                    index.setLemma(lemma);
                    index.setRank(count);
                    indexRepository.save(index);
                }

                visited.add(url);

                // Получаем ссылки со страницы
                Elements links = doc.select("a[href]");
                List<CrawlTask> subTasks = new ArrayList<>();
                for (var link : links) {
                    String absUrl = link.absUrl("href");
                    if (absUrl.startsWith(site.getUrl()) && !visited.contains(absUrl)) {
                        subTasks.add(new CrawlTask(site, absUrl, visited));
                    }
                }
                // Параллельный вызов
                invokeAll(subTasks);

                // Задержка между запросами 0.5-1 сек
                Thread.sleep(500 + new Random().nextInt(500));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
