package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchResponseData> data;
    private String error;

    public SearchResponse(boolean result, int count, List<SearchResponseData> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
