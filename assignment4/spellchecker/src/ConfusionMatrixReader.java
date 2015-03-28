import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfusionMatrixReader {
    
    final static String DATAFILE_LOC = "confusion_matrix_Kernighan.txt";
    final private HashMap<String,Integer> confusionMatrix = new HashMap<>();
    final private HashMap<String,Integer> countMatrix = new HashMap<>();
    public ConfusionMatrixReader() 
    {
        try {
            readConfusionMatrix();
        } catch (Exception ex) {
            Logger.getLogger(ConfusionMatrixReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readConfusionMatrix() 
            throws FileNotFoundException, IOException
    {
        FileInputStream fis;
        fis = new FileInputStream(DATAFILE_LOC);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        // RegEx that matches "<error>|<correct> <count>"
        Pattern p = Pattern.compile(
                // Group 1: error|correct
                "(" +
                    // Group 2: error
                    "([^|]+)" +
                    "\\|" +
                    // Group 3: correct
                    "(.+)" +
                ")" +
                " " +
                // Group 4: Count
                "([0-9]+)");
        
        while( in.ready() )
        {
            String line = in.readLine();
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String errorAndCorrect = m.group(1);
                String error = m.group(2);
                int count = Integer.parseInt(m.group(4));

                assert confusionMatrix.get(errorAndCorrect) == null;
                confusionMatrix.put(errorAndCorrect, count);
                countMatrix.put(error, countMatrix.getOrDefault(error, 0) + count);
            } else {
                System.err.println("problems with string <"+line+">");
            }
        }
    }
    
    /**
     * Returns the count for the pair <error>|<correct> in the confusion
     * matrix, e.g. "c|ct" is 36
     * 
     * @param error
     * @param correct
     * @return
     */
    public int getConfusionCount(String error, String correct) 
    {
        String errorAndCorrect = error + "|" + correct;
        return confusionMatrix.getOrDefault(errorAndCorrect, 0);
    }

    /**
     * @param charSequence
     * @return The number of (erroneous) occurrences of |charSequence| in the training data.
     */
    public int getCharsCount(String charSequence) {
        return countMatrix.getOrDefault(charSequence, 0);
    }
}
