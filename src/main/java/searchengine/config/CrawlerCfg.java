package searchengine.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CrawlerCfg {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
            "Gecko/20070725 Firefox/2.0.0.6";
    public static final String REFERRER = "http://www.google.com";
    public static final int TIMEOUT = 3000;

}
