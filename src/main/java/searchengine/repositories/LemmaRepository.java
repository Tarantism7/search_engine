package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    List<Lemma> findByLemma(String lemma);
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
    List<Lemma> findBySiteAndFrequencyLessThanEqualOrderByFrequencyAsc(Site site, int maxFrequency);
    List<Lemma> findBySite(Site site);
}
