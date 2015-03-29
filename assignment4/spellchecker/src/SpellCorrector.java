import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SpellCorrector {
    final private CorpusReader cr;
    final private ConfusionMatrixReader cmr;
    
    final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz'".toCharArray();
    
    
    public SpellCorrector(CorpusReader cr, ConfusionMatrixReader cmr) 
    {
        this.cr = cr;
        this.cmr = cmr;
    }
    
    public String correctPhrase(String phrase)
    {
        if(phrase == null || phrase.length() == 0)
        {
            throw new IllegalArgumentException("phrase must be non-empty.");
        }
            
        String[] words = phrase.split(" ");

        // For each word, generate a list of candidates.
        List<Map<String,Double>> alternativeWords = new ArrayList<>();
        Double[] wordProbabilities = new Double[words.length];
        int i = 0;
        for (String word : words) {
            Map<String,Double> candidates = getCandidateWords(word);
            alternativeWords.add(candidates);
            wordProbabilities[i] = candidates.getOrDefault(word, 0.);
            i++;
        }

        double bestProbability = Double.NEGATIVE_INFINITY;
        String[] bestSuggestion = words.clone();

        for (int error1 = 0; error1 < words.length; ++error1) {
            String originalWord1 = words[error1];
            Double originalProb1 = wordProbabilities[error1];
            for (Map.Entry<String, Double> entry1 : alternativeWords.get(error1).entrySet()) {
                String alternativeWord1 = entry1.getKey();
                words[error1] = alternativeWord1;
                wordProbabilities[error1] = entry1.getValue();
                // TODO: Remove Math.min() hack and just calculate probability here directly when sentence has < 3 words
                // Use +2 because there must be at least one good word in between the errors.
                for (int error2 = error1 + Math.min(words.length - 1, 2); error2 < words.length; ++error2) {
                    String originalWord2 = words[error2];
                    Double originalProb2 = wordProbabilities[error2];
                    for (Map.Entry<String, Double> entry2 : alternativeWords.get(error2).entrySet()) {
                        String alternativeWord2 = entry2.getKey();
                        words[error2] = alternativeWord2;
                        wordProbabilities[error2] = entry2.getValue();
                        // Try the sentence after correcting two errors.
                        double newP = getSuggestionProbability(words, wordProbabilities);
                        if (newP > bestProbability) {
                            bestProbability = newP;
                            bestSuggestion = words.clone();
                        }
                    }
                    words[error2] = originalWord2;
                    wordProbabilities[error2] = originalProb2;
                }
            }
            words[error1] = originalWord1;
            wordProbabilities[error1] = originalProb1;
        }

        System.out.println(bestProbability);
        return String.join(" ", bestSuggestion);
    }

    double getSuggestionProbability(String[] suggestion, Double[] wordProbabilities) {
        double probability = 1;
        for (int i = 0; i < wordProbabilities.length; ++i) {
            probability *= wordProbabilities[i];
        }
        return probability;
    }

    /**
     * Generate a set of possible corrections for the given word.
     *
     * @return A map where the keys are correction candidates, and the values the noisy channel probability.
     */
    private Map<String,Double> getCandidateWords(String wordWithoutWhitespace)
    {
        // The whitespace is necessary for add/substitution
        final String word = " " + wordWithoutWhitespace + " ";

        Map<String,Double> candidates = new HashMap<>();

        /**
         * @param start - Start position of word to be replaced.
         * @param end - Position after word to be replaced.
         * @param replacement
         */
        TriConsumer<Integer,Integer,String> collector = (start, end, replacement) ->
        {
            String candidate = word.substring(0, start) + replacement + word.substring(end);
            candidate = candidate.trim();
            if (!cr.inVocabulary(candidate)) {
                // Ignore non-words.
                return;
            }
            String original = word.substring(start, end);
            int charsCount = cmr.getCharsCount(original);
            if (charsCount == 0) {
                // The given character sequence is never misspelled in this way - ignore it.
                return;
            }
            // This is the scoring method as described by Kernighan et al. (1990) in
            // "A spelling correction program based on a noisy channel model".
            double prior = (cr.getNGramCount(candidate) + 0.5) / cr.getVocabularySize();
            double editProbability = cmr.getConfusionCount(original, replacement) / (double)charsCount;
            if (original.equals(replacement)) editProbability = 0.95;
            double wordProbability = prior * editProbability;
//System.out.println(candidate + ": " + wordProbability);
            candidates.put(candidate, wordProbability);
        };
        
        // We only have to find words with Damerau-Levenshtein distance of at
        // most 1 which means that each input word needs only be altered by at
        // most 1 insertion, deletion, transposition or substitution

        // Insertion
        for (int i = 1; i < word.length(); ++i) {
            for (char newLetter : ALPHABET) {
                String replacement = new String(new char[]{word.charAt(i - 1), newLetter});
                collector.call(i - 1, i, replacement);
            }
        }

        // Deletion
        for (int i = 1; i < word.length() - 1; ++i) {
            collector.call(i - 1, i + 1, word.substring(i - 1, i));
        }

        // Transposition
        for (int i = 1; i < word.length() - 2; ++i) {
            String replacement = new String(new char[]{word.charAt(i + 1), word.charAt(i)});
            collector.call(i, i + 2, replacement);
        }

        // Substitution
        for (int i = 1; i < word.length() - 1; ++i) {
            for (char newLetter : ALPHABET) {
                collector.call(i, i + 1, Character.toString(newLetter));
            }
        }

        return candidates;
    }

    private interface TriConsumer<T, U, V> {
        void call(T t, U u, V v);
    };
}
