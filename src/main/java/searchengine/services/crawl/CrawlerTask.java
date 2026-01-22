package searchengine.services.crawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.CrawlerCfg;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

public class CrawlerTask extends RecursiveAction {

    private final String baseUrl;
    private final String path;
    private final Site site;
    private final Set<String> visited;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    public CrawlerTask(String baseUrl, String path, Site site, Set<String> visited,
                       SiteRepository siteRepository, PageRepository pageRepository) {
        this.baseUrl = baseUrl;
        this.path = path;
        this.site = site;
        this.visited = visited;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Override
    protected void compute() {
        if (visited.contains(path)) return;
        visited.add(path);

        try {
            // Поддержка User-Agent и Referrer
            Document doc = Jsoup.connect(baseUrl + path)
                    .userAgent(CrawlerCfg.USER_AGENT)
                    .referrer(CrawlerCfg.REFERRER)
                    .timeout(CrawlerCfg.TIMEOUT)
                    .get();

            // Сохраняем страницу в базу
            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(200);
            page.setContent(doc.html());
            pageRepository.save(page);

            // Обновляем статус времени
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            // Получаем ссылки на новые страницы
            Elements links = doc.select("a[href]");
            Set<CrawlerTask> subTasks = new HashSet<>();
            for (Element link : links) {
                String href = link.attr("href");
                if (href.startsWith("/")) {
                    CrawlerTask subTask = new CrawlerTask(baseUrl, href, site, visited, siteRepository, pageRepository);
                    subTasks.add(subTask);
                }
            }

            invokeAll(subTasks);

        } catch (IOException e) {
            // Если страница недоступна, сохраняем ошибку
            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(404);
            page.setContent("");
            pageRepository.save(page);

            site.setStatus(Status.FAILED);
            site.setLastError("Ошибка загрузки страницы: " + path);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
    }
}
