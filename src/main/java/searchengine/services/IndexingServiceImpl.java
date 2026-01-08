package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    @Override
    public boolean startIndexing() {
        return false;
    }

    @Override
    public boolean stopIndexing() {
        return false;
    }

    @Override
    public boolean indexPage(String url) {
        return false;
    }
}
