import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        IntermediateAnswer answer = new IntermediateAnswer(phrase);

        // Suggest the original sentence, without any corrections.
        answer.update(phrase);

        // For each word, generate a list of candidates.
        List<Set<String>> alternativeWords = new ArrayList<>();
        for (String word : words) {
            alternativeWords.add(getCandidateWords(word));
        }

        // Suggest sentences with one or two errors corrected.
        for (int error1 = 0; error1 < words.length; ++error1) {
            // Save word, in order to restore the original words at the end of the loop.
            String original1 = words[error1];
            for (String alternativeWord1 : alternativeWords.get(error1)) {
                words[error1] = alternativeWord1;
                // Try the sentence after correcting one error.
                answer.update(String.join(" ", words));

                // Try two errors if possible.
                // Use +2 because there must be at least one good word in between the errors.
                for (int error2 = error1 + 2; error2 < words.length; ++error2) {
                    String original2 = words[error2];
                    for (String alternativeWord2 : alternativeWords.get(error2)) {
                        words[error2] = alternativeWord2;
                        // Try the sentence after correcting two errors.
                        answer.update(String.join(" ", words));
                    }
                    words[error2] = original2;
                }
            }
            words[error1] = original1;
        }

        return answer.getSuggestion();
    }

    public double calculateChannelModelProbability(String suggested, String incorrect) 
    {
        // 5.2.b: The method calculateChannel is meant to calculate the conditional
        // probability of a presumably incorrect word given a
        // correction. You need to decide whether a candidate suggestion
        // for an aledgedly incorrect word is a deletion, insertion,
        // substitution or a transposition, and what is the likelihood for
        // this to occur based on the values in the confusion matrix (for
        // which code is provided at the end of the method).
        String[] sWords = suggested.split(" ");
        double probability = 1;
        for (int i = 0; i < sWords.length - 1; ++i) {
            // Probability is op dit moment niet in de range [0..1],
            // maar is wel hoger voor zinnen met grotere kans op correctheid (hoop ik)
            probability *= cr.getSmoothedCount(sWords[i] + " " + sWords[i + 1]);
        }
        // TODO: Incorporate data from confusion matrix
        // Could be usefull to refactor this class a bit and add the data already at getCandidateWords

        return probability;
    }

    /**
     * Generate a set of possible corrections for the given word.
     *
     * @return A map where the words are keys, and the values the noisy channel probability. (TODO implement this)
     * @return A set consisting of possible candidate corrections for a word.
     */
    private Set<String> getCandidateWords(String word)
    {
        Map<String,Double> candidates = new HashMap<>();
        
        // We only have to find words with Damerau-Levenshtein distance of at
        // most 1 which means that each input word needs only be altered by at
        // most 1 insertion, deletion, transposition or substitution

        // Insertion
        for (int i = 0; i <= word.length(); ++i) {
            for (char newLetter : ALPHABET) {
                maybeAddCandidate(candidates, word.substring(0, i) + newLetter + word.substring(i));
            }
        }

        // Deletion
        for (int i = 0; i < word.length(); ++i) {
            maybeAddCandidate(candidates, word.substring(0, i) + word.substring(i + 1));
        }

        // Transposition
        for (int i = 0; i < word.length() - 1; ++i) {
            char[] mutableWord = word.toCharArray();
            char first = mutableWord[i];
            mutableWord[i] = mutableWord[i + 1];
            mutableWord[i + 1] = first;
            maybeAddCandidate(candidates, new String(mutableWord));
        }

        // Substitution
        for (int i = 0; i < word.length(); ++i) {
            for (char newLetter : ALPHABET) {
                maybeAddCandidate(candidates, word.substring(0, i) + newLetter + word.substring(i + 1));
            }
        }

        // TODO: Just return candidates.
        return candidates.keySet();
    }

    /**
     * Add <code>candidate</code> and its 
     */
    private void maybeAddCandidate(Map<String,Double> candidates, String candidate) {
        if (!cr.inVocabulary(candidate)) {
            return;
        }
        double wordProbability = 0.0;
        // TODO: Calculate word probability.
        candidates.put(candidate, wordProbability);
    }

    private class IntermediateAnswer {
        private final String original;
        private String suggestion = "";
        private double probability = 0;

        IntermediateAnswer(String original) {
            this.original = original;
        }

        /**
         * Propose an alternative to the current phrase. If the suggestion has a higher
         * probability of being correct, the instance's state is updated with the new suggestion.
         *
         * @param suggestion - The suggested sentence.
         */
        void update(final String suggestion) {
            double probability = calculateChannelModelProbability(suggestion, original);
            if (probability > this.probability) {
                this.suggestion = suggestion;
                this.probability = probability;
            }
        }

        String getSuggestion() {
            return suggestion.trim();
        }
    };
}
