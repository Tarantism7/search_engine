package searchengine.services.index;

public interface IndexingService {
    boolean startIndexing();

    boolean stopIndexing();

    boolean indexPage(String url);
}
