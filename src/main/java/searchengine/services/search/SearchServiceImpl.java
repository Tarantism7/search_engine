package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchResponseData;
import searchengine.model.Lemma;
import searchengine.model.Page;
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
        return null;
    }
}
