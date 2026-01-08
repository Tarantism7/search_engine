package searchengine.services;

public interface IndexingService {
    public boolean startIndexing();

    public boolean stopIndexing();

    public boolean indexPage(String url);
}
