package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.index.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statisctics.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        if (indexingService.startIndexing()) {
            return ResponseEntity.ok(new IndexingResponse(true));
        } else {
            return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация уже запущена"));
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return ResponseEntity.ok(new IndexingResponse(true));
        } else {
            return ResponseEntity.badRequest().body(new IndexingResponse(false, "Индексация уже запущена"));
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        if (indexingService.indexPage(url)) {
            return ResponseEntity.ok(new IndexingResponse(true));
        } else {
            return ResponseEntity.badRequest().body(new IndexingResponse(false
                    , "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam String site,
                                                 @RequestParam(defaultValue = "0") int offset,
                                                 @RequestParam(defaultValue = "20") int limit) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new SearchResponse(false, "Задан пустой поисковый запрос"));
        } else {
            return ResponseEntity.ok(searchService.search(query, site, offset, limit));
        }
    }
}
