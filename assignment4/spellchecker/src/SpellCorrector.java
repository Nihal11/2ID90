import java.util.HashSet;

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
        
        /** CODE TO BE ADDED **/
        
        return finalSuggestion.trim();
    }
    
    public double calculateChannelModelProbability(String suggested, String incorrect) 
    {
         /** CODE TO BE ADDED **/
        
        return 0.0;
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
