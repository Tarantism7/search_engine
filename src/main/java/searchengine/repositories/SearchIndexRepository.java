package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SearchIndex;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
    List<SearchIndex> findByLemmaInAndPage_Site(List<Lemma> lemmas, Site site);
    Optional<SearchIndex> findByPageAndLemma(Page page, Lemma lemma);
    List<SearchIndex> findByPage(Page page);
}
