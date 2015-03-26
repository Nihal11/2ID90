import java.util.HashSet;
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
        String finalSuggestion = "";
        
        Set<String> suggestions = new HashSet();
        // Generate all sentences with 0, 1, or 2 errors
        for (int error1 = -1; error1 < words.length; ++error1) {
            for (int error2 = error1; error2 < words.length; ++error2) {
                // Error index -1 means no error
                if (error2 == error1) continue; // Two errors at the same location is useless
                if (error1 >= 0 && error2 - error1 < 2) continue; // There can't be 2 consecutive errors
                generateSuggestions(suggestions, words, error1, error2, 0, "");
            }
        }
        System.out.println("Number of suggestions: " + suggestions.size()); // TODO: Remove debugging output

        // Select sentence suggestion with highest probability of being correct
        double highestProbability = Double.NEGATIVE_INFINITY;
        String bestSuggestion = "";
        for (String suggestion : suggestions) {
            double probability = calculateChannelModelProbability(suggestion, phrase);
            if (probability > highestProbability) {
                highestProbability = probability;
                bestSuggestion = suggestion;
            }
        }
        finalSuggestion = bestSuggestion;
        System.out.println("Probability: " + highestProbability);
        
        return finalSuggestion.trim();
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

        return probability;
    }
         
    void generateSuggestions(Set<String> suggestions, String[] words, int error1, int error2, int index, String current) {
        if (index == words.length) {
            suggestions.add(current.trim());
            return;
        }
        if (index == error1 || index == error2) {
            Set<String> candidates = getCandidateWords(words[index]);
            for (String candidate : candidates) {
                generateSuggestions(suggestions, words, error1, error2, index + 1, current + candidate + " ");
            }
        } else {
            generateSuggestions(suggestions, words, error1, error2, index + 1, current + words[index] + " ");
        }
    }
      
    public HashSet<String> getCandidateWords(String word)
    {
        HashSet<String> ListOfWords = new HashSet<String>();
        
        // Original word could be correct
        ListOfWords.add(word);
        
        // We only have to find words with Damerau-Levenshtein distance of at
        // most 1 which means that each input word needs only be altered by at
        // most 1 insertion, deletion, transposition or substitution

        // Insertion
        for (int i = 0; i <= word.length(); ++i) {
            for (char newLetter : ALPHABET) {
                ListOfWords.add(word.substring(0, i) + newLetter + word.substring(i));
            }
        }

        // Deletion
        for (int i = 0; i < word.length(); ++i) {
            ListOfWords.add(word.substring(0, i) + word.substring(i + 1));
        }

        // Transposition
        for (int i = 0; i < word.length() - 1; ++i) {
            char[] mutableWord = word.toCharArray();
            char first = mutableWord[i];
            mutableWord[i] = mutableWord[i + 1];
            mutableWord[i + 1] = first;
            ListOfWords.add(new String(mutableWord));
        }

        // Substitution
        for (int i = 0; i < word.length(); ++i) {
            for (char newLetter : ALPHABET) {
                ListOfWords.add(word.substring(0, i) + newLetter + word.substring(i + 1));
            }
        }

        return cr.inVocabulary(ListOfWords);
    }          
}
