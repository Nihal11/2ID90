import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellCorrector {
    final private CorpusReader cr;
    final private ConfusionMatrixReader cmr;
    
    final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz'".toCharArray();

    // Add constant to the results from the confusion matrix to smooth the edit probability.
    final double EDIT_PROBABILITY_K_SMOOTHING = 1;

    // Constant added to a word's count to smooth according to method as
    //   described by Kernighan et al. (1990). This improves probability for
    //   unknown words. This does not add anything useful now as we don't
    //   allow words that are not in the vocabulary anyway.
    final double ADD_K_PRIOR = 0.0;
    
    // We assume that one out of 200 words contains an error
    final double PROBABILITY_NO_EDIT_NEEDED = 0.95;
    
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
        List<Map<String,Double>> candidates = new ArrayList<>();
        for (String word : words) {
            candidates.add(getCandidateWords(word));
        }

        // Build a suggestion sentence and calculate probability for original sentence.
        IntermediateAnswer answer = new IntermediateAnswer(words, candidates);
        // The IntermediateAnswer keeps track of the best answer. Every time its evaluate()
        // method is called, the word at the given position is replaced with the suggested
        // candidate. If the new suggestion is more likely to be correct than the previous
        // one, the internal state is updated.

        // Suggest sentences with one or two errors corrected.
        for (int error1 = 0; error1 < words.length; ++error1) {
            // Save word, in order to restore the original words at the end of the loop.
            for (String candidate1 : candidates.get(error1).keySet()) {
                // Try the sentence after correcting one error.
                answer.evaluate(error1, candidate1);

                // Try two errors if possible.
                // Use +2 because there must be at least one good word in between the errors.
                for (int error2 = error1 + 2; error2 < words.length; ++error2) {
                    for (String candidate2 : candidates.get(error2).keySet()) {
                        // Try the sentence after correcting two errors.
                        answer.evaluate(error2, candidate2);
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
        // The whitespace is necessary for add/deletion.
        final String word = " " + wordWithoutWhitespace + " ";

        Map<String,Double> candidates = new HashMap<>();

        /**
         * Puts candidate corrections together with their probability into a map.
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

            double prior = calculatePrior(candidate);
            // Get count from confusion matrix and smooth the probability to allow for unseen corrections.
            double editProbability = (double)(cmr.getConfusionCount(original, replacement) + EDIT_PROBABILITY_K_SMOOTHING) / (cmr.getCharsCount(original) + EDIT_PROBABILITY_K_SMOOTHING);
            double wordProbability = prior * editProbability;

            // Sum probabilities if word can be formed in multiple ways,
            //       (e.g. acress->acres: ss|s and es|e)
            wordProbability += candidates.getOrDefault(candidate, 0.0);

            if (wordProbability > 1) {
                // This is unlikely, but just in case (ensure that probability <= 1).
                wordProbability = 1;
            }

            candidates.put(candidate, wordProbability);
        };
        
        // We only have to find words with Damerau-Levenshtein distance of at
        // most 1 which means that each input word needs only be altered by at
        // most 1 insertion, deletion, transposition or substitution.

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

        // Remove the word itself, because it should not be interpreted as a suggestion.
        // Otherwise the presence of this word prevents the next word from being corrected.
        candidates.remove(wordWithoutWhitespace);

        return candidates;
    }

    private double calculatePrior(String word) {
        // This is the scoring method as described by Kernighan et al. (1990) in
        // "A spelling correction program based on a noisy channel model".
        return (cr.getNGramCount(word) + ADD_K_PRIOR) / cr.getVocabularySize();
    }

    // Interface to enable the declaration of a lambda function with three parameters.
    private interface TriConsumer<T, U, V> {
        void call(T t, U u, V v);
    };

    private class IntermediateAnswer {
        // The words of the original sentence.
        private final String[] original;
        // A list of candidate corrections, stored in a map (key = candidate, value = word probability).
        private final List<Map<String,Double>> candidates;

        // The current suggestion, per-word likelihood and summed likelihood.
        private String[] suggestion;
        private double[] likelihoods;
        // A number in the range [-Infinity, 0]. -Infinity = improbable, 0 = very likely.
        private double likelihoodSum;

        // The best suggestion and its summed likelihood.
        private String[] bestSuggestion = {};
        private double bestLikelihoodSum = 0;

        IntermediateAnswer(String[] original, List<Map<String,Double>> candidates) {
            this.original = original.clone();
            this.candidates = candidates;
            this.suggestion = original.clone();

            // Calculate the likelihoods of the original word, without any correction.
            likelihoods = new double[original.length];
            likelihoodSum = 0;
            for (int i = 0; i < suggestion.length; ++i) {
                double likelihood = getWordLikelihoodAt(i);
                likelihoods[i] = likelihood;
                likelihoodSum += likelihood;
            }

            this.bestSuggestion = suggestion.clone();
            this.bestLikelihoodSum = likelihoodSum;
        }

        /**
         * Propose an alternative to the current phrase. If the suggestion has a higher
         * probability of being correct, the instance's state is evaluated with the new suggestion.
         *
         * @param wordIndex - The position where the word will be changed.
         * @param suggestion - The suggested word.
         */
        void evaluate(int wordIndex, String suggestedWord) {
            suggestion[wordIndex] = suggestedWord;

            if (recalculateLikelihoodAt(wordIndex)) {
                bestSuggestion = suggestion.clone();
                bestLikelihoodSum = likelihoodSum;
            }
        }

        /**
         * Restore the original word at the given index.
         *
         * @param wordIndex
         */
        void restore(int wordIndex) {
            suggestion[wordIndex] = original[wordIndex];
            recalculateLikelihoodAt(wordIndex);
        }

        String getSuggestion() {
            if (!Double.isFinite(bestLikelihoodSum)) {
                return "";
            }
            return String.join(" ", bestSuggestion);
        }

        /**
         * @param wordIndex A number in the range [0, number of words in |original|]
         * @return Whether the overall probability increased since the last check.
         */
        private boolean recalculateLikelihoodAt(int wordIndex) {
            double diff = -likelihoods[wordIndex];
            likelihoods[wordIndex] = getWordLikelihoodAt(wordIndex);
            diff += likelihoods[wordIndex];

            // Changing a word may affect the probability of the next word, because the
            // algorithm takes the previous word into account in the calculation of word
            // probability. So, if this is not the last word, recalculate the probability
            // of the next word as well.
            if (wordIndex != likelihoods.length - 1) {
                diff -= likelihoods[wordIndex + 1];
                likelihoods[wordIndex + 1] = getWordLikelihoodAt(wordIndex + 1);
                diff += likelihoods[wordIndex + 1];
            }

            if (Double.isFinite(likelihoodSum)) {
                // Previous sum is finite. Re-use result.
                likelihoodSum += diff;
            } else {
                // Previous calculation is unusable. Recalculate full sum.
                likelihoodSum = 0;
                for (double s : likelihoods) {
                    likelihoodSum += s;
                }
            }
            return likelihoodSum > bestLikelihoodSum;
        }

        /**
         * Calculate the likelihood that the word at a given location is correct.
         * @return a number in the range [-Infinity, 0]
         *         -Infinity = improbable
         *         0 = very likely
         */
        private double getWordLikelihoodAt(int wordIndex) {
            double probability = getWordProbabilityAt(wordIndex);
            double likelihood = Math.log(probability);
            return likelihood;
        }

        /**
         * Calculate the probability that a word at the given location is correct.
         * @return a number in the range [0,1]
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

            if (word.equals(original[wordIndex])) {
                probability *= PROBABILITY_NO_EDIT_NEEDED * calculatePrior(word);
            } else {
                // Note: Using .get instead of .getOrDefault because the word should either
                // be the original word, or a suggestion.
                probability *= candidates.get(wordIndex).get(word);
            }
            return probability;
        }
    };
}
