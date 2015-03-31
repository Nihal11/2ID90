import java.io.IOException;
import java.util.Scanner;


public class SpellChecker {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        boolean inPeach = false; // set this to true if you submit to peach!!!
        
        try {
            CorpusReader cr = new CorpusReader();
            ConfusionMatrixReader cmr = new ConfusionMatrixReader();
            SpellCorrector sc = new SpellCorrector(cr, cmr);
            if (inPeach) {
                peachTest(sc);
            } else {
                nonPeachTest(sc);
            }
        } catch (Exception ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }
    }
    
    static void nonPeachTest(SpellCorrector sc) throws IOException { 
            String[] sentences = {
                "at the hme locations there were traces of water",
                "helo ladies and gentelmen",
                "kind retards",
                "this assay allowed us to measure a wide variety of conditions",
                "this assay allowed us to measure a wide variety of conitions",
                "this assay allowed us to meassure a wide variety of conditions",
                "this assay allowed us to measure a wide vareity of conditions",
                "at the home locations there were traces of water",
                "at the hme locations there were traces of water",
                "at the hoome locations there were traces of water",
                "at the home locasions there were traces of water",
                "the development of diabetes is present in mice that carry a transgen",
                "the development of diabetes is present in moce that carry a transgen",
                "the development of idabetes is present in mice that carry a transgen",
                "the development of diabetes us present in mice that harry a transgen",
        /* Following are tests from peach */
                "this assay allowed us to measure a wide variety of conitions",
                "at the hme locations there were traces of water",
                "this assay allowed us to meassure a wide variety of conditions",
                "this assay allowed us to measure a wide vareity of conditions",
                "at the hoome locations there were traces of water",
                "at the home locasions there were traces of water",
                "the development of diabetes is present in moce that carry a transgen",
                "the development of idabetes is present in mice that carry a transgen",
                "the development of diabetes us present in mice that harry a transgene",
                "boxing glowes shield the knockles not the head",
                "boxing loves shield the knuckles nots the head",
                "boing gloves shield the knuckles nut the head",
                "she still refers to me has a friend but i fel i am treated quite badly",
                "she still refers to me as a friendd but i feel i am traeted quite badly",
                "she still refers too me as a friend but i feel i am treated quite batly",
                "she still refers to me as a fiend but i feel i am treated quite badly",
                "essentially here has bien no change in japan",
                "this advise is taking into consideration the fact that the govenrment bans political parties",
                "ancient china wqs one of the longest lasting societies in the histori of the world",

            };
            String[] correctSentences = {
                "at the home locations there were traces of water",
                "hello ladies and gentlemen",
                "kind regards",
                "this assay allowed us to measure a wide variety of conditions",
                "this assay allowed us to measure a wide variety of conditions",
                "this assay allowed us to measure a wide variety of conditions",
                "this assay allowed us to measure a wide variety of conditions",
                "at the home locations there were traces of water",
                "at the home locations there were traces of water",
                "at the home locations there were traces of water",
                "at the home locations there were traces of water",
                "the development of diabetes is present in mice that carry a transgene",
                "the development of diabetes is present in mice that carry a transgene",
                "the development of diabetes is present in mice that carry a transgene",
                "the development of diabetes is present in mice that carry a transgene",
        /* Following are tests from peach */
                "this assay allowed us to measure a wide variety of conditions",
                "at the home locations there were traces of water",
                "this assay allowed us to measure a wide variety of conditions",
                "this assay allowed us to measure a wide variety of conditions",
                "at the home locations there were traces of water",
                "at the home locations there were traces of water",
                "the development of diabetes is present in mice that carry a transgene",
                "the development of diabetes is present in mice that carry a transgene",
                "the development of diabetes is present in mice that carry a transgene",
                "boxing gloves shield the knuckles not the head",
                "boxing gloves shield the knuckles not the head",
                "boxing gloves shield the knuckles not the head",
                "she still refers to me as a friend but i feel i am treated quite badly",
                "she still refers to me as a friend but i feel i am treated quite badly",
                "she still refers to me as a friend but i feel i am treated quite badly",
                "she still refers to me as a friend but i feel i am treated quite badly",
                "essentially there has been no change in japan",
                "this advice is taking into consideration the fact that the government bans political parties",
                "ancient china was one of the longest lasting societies in the history of the world",
            };
            
            for(int i = 0; i < sentences.length; ++i) {
                String original = sentences[i];
                String correct = correctSentences[i];
                String result = sc.correctPhrase(original);

                if (! result.equals(correct)) {
                    System.out.println("Input  : " + original);
                    System.out.println("Answer : " + result);
                    System.out.println("Correct: " + correct);
                    System.out.println();
                }
            }
    }
    
    static void peachTest(SpellCorrector sc) throws IOException {
            Scanner input = new Scanner(System.in);
            
            String sentence = input.nextLine();
            System.out.println("Answer: " + sc.correctPhrase(sentence));  
    } 
}
