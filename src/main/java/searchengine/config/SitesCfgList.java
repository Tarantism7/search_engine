package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesCfgList {
    // YAML uses 'sites-cfgs' key; bind to this field
    private List<SiteCfg> sitesCfgs = new ArrayList<>();

    // keep compatibility with existing code expecting getSiteCfgs()
    public List<SiteCfg> getSiteCfgs() {
        return sitesCfgs;
    }

    public void setSiteCfgs(List<SiteCfg> siteCfgs) {
        this.sitesCfgs = siteCfgs != null ? siteCfgs : new ArrayList<>();
    }
}
