package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseData;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SearchIndexRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemma.LemmaServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaServiceImpl lemmaService;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SearchIndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Override
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchResponse(false, "Задан пустой поисковый запрос");
        }

        List<Site> sitesToSearch = getSitesToSearch(siteUrl);
        if (sitesToSearch == null) {
            return new SearchResponse(false, "Сайт не найден");
        }

        Set<String> queryLemmas = lemmaService.getLemmasSet(query);
        if (queryLemmas.isEmpty()) {
            return new SearchResponse(false, "Не удалось извлечь леммы из запроса");
        }

        Map<Page, Float> relevanceMap = buildRelevanceMap(sitesToSearch, queryLemmas);
        List<SearchResponseData> results = buildSearchResults(relevanceMap, offset, limit, queryLemmas);

        return new SearchResponse(true, relevanceMap.size(), results);
    }

    private List<Site> getSitesToSearch(String siteUrl) {
        List<Site> sitesToSearch = new ArrayList<>();
        if (siteUrl != null && !siteUrl.isEmpty()) {
            Site site = siteRepository.findByUrl(siteUrl).orElse(null);
            if (site == null) {
                return null;
            }
            sitesToSearch.add(site);
        } else {
            sitesToSearch.addAll(siteRepository.findAll());
        }
        return sitesToSearch;
    }

    private Map<Page, Float> buildRelevanceMap(List<Site> sitesToSearch, Set<String> queryLemmas) {
        Map<Page, Float> relevanceMap = new HashMap<>();

        for (Site site : sitesToSearch) {
            List<Lemma> siteLemmas = findQueryLemmasForSite(site, queryLemmas);
            if (siteLemmas.isEmpty()) {
                continue;
            }

            List<SearchIndex> indices = indexRepository.findByLemmaInAndPage_Site(siteLemmas, site);
            aggregateRelevance(relevanceMap, indices);
        }

        return relevanceMap;
    }

    private List<Lemma> findQueryLemmasForSite(Site site, Set<String> queryLemmas) {
        return lemmaRepository.findAll().stream()
                .filter(l -> l.getSite().getId() == site.getId() && queryLemmas.contains(l.getLemma()))
                .collect(Collectors.toList());
    }

    private void aggregateRelevance(Map<Page, Float> relevanceMap, List<SearchIndex> indices) {
        for (SearchIndex index : indices) {
            relevanceMap.merge(index.getPage(), index.getRank(), Float::sum);
        }
    }

    private List<SearchResponseData> buildSearchResults(Map<Page, Float> relevanceMap, 
                                                        int offset, int limit, Set<String> queryLemmas) {
        float maxRelevance = relevanceMap.values().stream()
                .max(Float::compare).orElse(1f);

        return relevanceMap.entrySet().stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .skip((long) offset)
                .limit(limit)
                .map(entry -> createSearchResultData(entry.getKey(), entry.getValue(), maxRelevance, queryLemmas))
                .collect(Collectors.toList());
    }

    private SearchResponseData createSearchResultData(Page page, float relevance, 
                                                       float maxRelevance, Set<String> queryLemmas) {
        SearchResponseData data = new SearchResponseData();
        data.setSite(page.getSite().getUrl());
        data.setSiteName(page.getSite().getName());
        data.setUri(page.getPath());
        data.setTitle(extractTitle(page.getContent()));
        data.setSnippet(extractSnippet(page.getContent(), queryLemmas));
        data.setRelevance((float) (Math.round((relevance / maxRelevance) * 10000) / 10000.0));
        return data;
    }

    private String extractTitle(String html) {
        try {
            Document doc = Jsoup.parse(html);
            org.jsoup.nodes.Element titleEl = doc.selectFirst("title");
            String title = titleEl != null ? titleEl.text() : "";
            if (title.isEmpty()) {
                org.jsoup.nodes.Element h1 = doc.selectFirst("h1");
                title = h1 != null ? h1.text() : "Без названия";
            }
            return title;
        } catch (Exception e) {
            return "Без названия";
        }
    }

    private String extractSnippet(String html, Set<String> queryLemmas) {
        try {
            Document doc = Jsoup.parse(html);
            String text = doc.body().text();
            String[] words = text.split("\\s+");

            int maxLength = 300;
            StringBuilder snippet = new StringBuilder();

            for (int i = 0; i < words.length && snippet.length() < maxLength; i++) {
                String word = words[i].toLowerCase();
                if (queryLemmas.stream().anyMatch(lemma -> word.contains(lemma))) {
                    int start = Math.max(0, i - 5);
                    int end = Math.min(words.length, i + 6);

                    for (int j = start; j < end; j++) {
                        if (j == i) {
                            snippet.append("<b>").append(words[j]).append("</b> ");
                        } else {
                            snippet.append(words[j]).append(" ");
                        }
                    }
                    snippet.append("... ");

                    if (snippet.length() > maxLength) {
                        break;
                    }
                }
            }

            String result = snippet.toString().trim();
            return result.isEmpty() ? text.substring(0, Math.min(300, text.length())) : result;
        } catch (Exception e) {
            return "Фрагмент текста недоступен";
        }
    }
}
