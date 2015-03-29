import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        for (String word : words) {
            alternativeWords.add(getCandidateWords(word));
        }

        IntermediateAnswer answer = new IntermediateAnswer(words, alternativeWords);

        // Suggest sentences with one or two errors corrected.
        for (int error1 = 0; error1 < words.length; ++error1) {
            // Save word, in order to restore the original words at the end of the loop.
            for (String alternativeWord1 : alternativeWords.get(error1).keySet()) {
                // Try the sentence after correcting one error.
                answer.update(error1, alternativeWord1);

                // Try two errors if possible.
                // Use +2 because there must be at least one good word in between the errors.
                for (int error2 = error1 + 2; error2 < words.length; ++error2) {
                    for (String alternativeWord2 : alternativeWords.get(error2).keySet()) {
                        // Try the sentence after correcting two errors.
                        answer.update(error2, alternativeWord2);
                    }
                    answer.restore(error2);
                }
            }
            answer.restore(error1);
        }

        return answer.getSuggestion();
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
            double wordProbability = prior * editProbability;

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
            collector.call(i - 1, i + 1, word.substring(i, i + 1));
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

    private class IntermediateAnswer {
        // The words of the original sentence.
        private final String[] original;
        // A list of suggestions, stored in a map (key = candidate, value = word probability).
        private final List<Map<String,Double>> alternativeWords;

        // The current suggestion, per-word probability and summed probability.
        private String[] suggestion;
        private double[] probabilities;
        private double probabilitySum;

        // The best suggestion and its probability.
        private String[] bestSuggestion = {};
        private double bestProbabilitySum = 0;

        IntermediateAnswer(String[] original, List<Map<String,Double>> alternativeWords) {
            this.original = original.clone();
            this.alternativeWords = alternativeWords;
            this.suggestion = original.clone();

            // Calculate the probabilities of the original word, without any correction.
            probabilities = new double[original.length];
            Arrays.fill(probabilities, 0);
            probabilitySum = 0;
            for (int i = 0; i < suggestion.length; ++i) {
                double probability = getWordProbabilityAt(i);
                probabilities[i] = probability;
                probabilitySum += probability;
            }

            this.bestSuggestion = suggestion.clone();
            this.bestProbabilitySum = probabilitySum;
        }

        /**
         * Propose an alternative to the current phrase. If the suggestion has a higher
         * probability of being correct, the instance's state is updated with the new suggestion.
         *
         * @param wordIndex - The position where the word will be changed.
         * @param suggestion - The suggested word.
         */
        void update(int wordIndex, String suggestedWord) {
            suggestion[wordIndex] = suggestedWord;

            if (recalculateProbabilityAt(wordIndex)) {
                bestSuggestion = suggestion.clone();
                bestProbabilitySum = probabilitySum;
            }
        }

        /**
         * Restore the original word at the given index.
         *
         * @param wordIndex
         */
        void restore(int wordIndex) {
            suggestion[wordIndex] = original[wordIndex];
            recalculateProbabilityAt(wordIndex);
        }

        String getSuggestion() {
            return String.join(" ", bestSuggestion);
        }

        /**
         * @param wordIndex A number in the range [0, number of words in |original|]
         * @return Whether the overall probability increased since the last check.
         */
        private boolean recalculateProbabilityAt(int wordIndex) {
            double pOriginal = probabilities[wordIndex];
            probabilities[wordIndex] = getWordProbabilityAt(wordIndex);
            double diff = probabilities[wordIndex] - pOriginal;

            // Changing a word may affect the probability of the next word, because the
            // algorithm takes the previous word into account in the calculation of word
            // probability. So, if this is not the last word, recalculate the probability
            // of the next word as well.
            if (wordIndex != probabilities.length - 1) {
                double pNextOriginal = probabilities[wordIndex + 1];
                probabilities[wordIndex + 1] = getWordProbabilityAt(wordIndex + 1);
                diff += probabilities[wordIndex + 1] - pNextOriginal;
            }
            probabilitySum += diff;
            //System.out.println(String.format("%s %s", suggestion[wordIndex], diff));
            return diff > 0;
        }

        /**
         * Calculate the probability that a word at the given location is correct.
         */
        private double getWordProbabilityAt(int wordIndex) {
            String word = suggestion[wordIndex];
            double probability;
            if (!cr.inVocabulary(word)) {
                // Words that are not in the dictionary MUST be corrected.
                return 0;
            }
            if (wordIndex == 0) {
                // There is no word before the first word.
                probability = 1;
            } else {
                probability = cr.getSmoothedCount(suggestion[wordIndex - 1] + " " + word) /
                    cr.getSmoothedCount(suggestion[wordIndex - 1]);
            }
            // TODO: Replace "1" with a constant. If it is too low (0), then correct words
            // may be replaced. If it is too high (1), then existing (but incorrect) words
            // could still stick around.
            // For now, be conversative by not lowering the probability if the word does
            // exist in the dictionary.
            probability *= alternativeWords.get(wordIndex).getOrDefault(word, 1.0);
            return probability;
        }
    };
}
