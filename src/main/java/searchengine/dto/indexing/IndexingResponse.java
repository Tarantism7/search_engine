package searchengine.dto.indexing;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse(boolean result) {
        this.result = result;
    }

    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
