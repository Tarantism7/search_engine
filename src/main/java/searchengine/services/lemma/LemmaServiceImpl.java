package searchengine.services.lemma;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class LemmaServiceImpl {
    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaServiceImpl() throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
    }

    public Map<String, Integer> extractLemmas(String text) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        String[] words = russianWords(text);

        for (String w : words) {
            if (w.isBlank()) {
                continue;
            }
            List<String> baseForms = luceneMorphology.getMorphInfo(w);
            if (isWordBaseParticle(baseForms)) {
                continue;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(w);
            if (normalForms.isEmpty()) {
                continue;
            }
            String normalWord = normalForms.get(0);
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
        return lemmas;
    }

    public Set<String> getLemmasSet(String text) {
        String[] a = russianWords(text);
        Set<String> lemmasSet = new HashSet<>();
        for (String i : a) {
            if (!i.isEmpty() || isCorrectForm(i)) {
                List<String> baseForms = luceneMorphology.getMorphInfo(i);
                if (isWordBaseParticle(baseForms)) continue;
            }
            lemmasSet.addAll(luceneMorphology.getNormalForms(i));
        }
        return lemmasSet;
    }

    private String[] russianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isCorrectForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String i : wordInfo) {
            if (i.matches(WORD_TYPE_REGEX)) return false;
        }
        return true;
    }

    private boolean withParticleProp(String word) {
        for (String i : particlesNames) {
            if (word.toLowerCase().contains(i)) return true;
        }
        return false;
    }

    private boolean isWordBaseParticle(List<String> baseForms) {
        return baseForms.stream().anyMatch(this::withParticleProp);
    }
}
