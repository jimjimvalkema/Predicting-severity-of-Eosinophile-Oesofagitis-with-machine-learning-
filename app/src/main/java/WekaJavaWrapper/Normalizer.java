package WekaJavaWrapper;

import javax.lang.model.type.ArrayType;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A object with functions to normalize a arff or csv file or single instance to a new csv file.
 */
public class Normalizer {
    private Hashtable<String, Double> means;// = new double[10]; //{64.87487,4.381235,69.19741,3.606538,36.82245,3.221768,64.61564,3.182324,39.30981,2.944891};
    private Hashtable<String, Double> sd; //{54.05608,2.86146,58.92949,2.19015,19.89936,2.021169,37.97793,2.084124,23.2727,2.194744};\
    private String header;
    private Hashtable<String, Integer> attrIndexes;

    /**
     * A object with functions to normalize a arff or csv file or single instance to a new csv file.
     * @param means a list of means as doubles used for scaling normalization
     * @param sd a list of standard deviations as doubles used for scaling normalization
     * @param header a comma separated header for the output csv or also input file if it is a csv
     */
    public Normalizer(Hashtable means, Hashtable sd, String header, Hashtable attrIndexes) {
        this.setMeans(means);
        this.setSd(sd);
        this.setHeader(header);
        this.setAttrIndexes(attrIndexes);
    }

    /**
     * does a log2 transform
     * @param x :  a double to be transformed
     * @return
     */
    private static double log2(double x)
    {
        return (Math.log(x) / Math.log(2));
    }

    /**
     * @param data : as a string[]
     * @return : a String with a items comma separated
     */
    private static String convertToCSV(String[] data) {
        return Stream.of(data).collect(Collectors.joining(","));
    }

//    /**
//     * Normalises one instance from a String[] and returns it a list<String>
//     * @param instance :  the instance as a String
//     * @return : normalised instance as a list<String>
//     */
//    private  String[] normalizeInstance(String instance) throws NumberFormatException {
//        double[] validInstance = getValidInstance(instance);
//        double res;
//        String[] normalizedInstance = new String[10];
//        for (int i = 0; i < validInstance.length; i ++) {
//            res = log2(validInstance[i]);
//            res = (res - this.means[i]) / this.sd[i];
//            normalizedInstance[i] = String.valueOf(res);
//        }
//        return normalizedInstance;
//    }

    private String[] normalizeInstance(String instance) throws NumberFormatException {
        double[] validInstance = getValidInstance(instance);
        double res;

        List<String> normalizedInstanceList = new ArrayList<String>();
        //String[] normalizedInstance = new String[4];
        for (int i = 0; i < validInstance.length; i ++) {
            String currentAttr = this.attrIndexes.keySet().toArray()[i].toString();
            if (currentAttr.matches("Gender") || currentAttr.matches("Neocate")){
                res = validInstance[i];
            } else {
                //TODO maybe handle 0 values differently?
                res = log2(validInstance[i]+0.001);
                res = (res - this.means.get(currentAttr)) / this.sd.get(currentAttr);
            }
            normalizedInstanceList.add(String.valueOf(res));
        }
        String[] normalizedInstanceArray = new String[normalizedInstanceList.size()];
        normalizedInstanceList.toArray(normalizedInstanceArray);
        return normalizedInstanceArray;
    }

    /**
     * Creates a csv file with a given header. over writes if it already exists
     * @param outputFilePath : The file path to create the file at as a String
     * @return :  the file as a buffer
     */
    private BufferedWriter createCsvTemplateFile(String outputFilePath) {
        try {
            //Create normalized file
            File outputFile = new File(outputFilePath);
            BufferedWriter outPutFile = new BufferedWriter(
                    new FileWriter(outputFilePath, true));
            String newHeader = this.attrIndexes.keySet().toString().replace("[", "").replace("]", "");
            outPutFile.write(newHeader+ "\n");
            if (outputFile.createNewFile()) {
            } else {
                PrintWriter writer = new PrintWriter(outputFile);
                writer.print("");
                writer.close();
            }
            return outPutFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Validates and parses a given attribute and throws IllegalArgumentExceptions if the instance contains invalid attributes
     * @param attribute The attribute as a string to parse and validate
     * @return attribute the parsed and validated attribute as a double
     */
    private static double getValidAttribute(String attribute) {
        double attributeAsDouble;

        //TODO check we can make empty attr 0
        if (attribute.replaceAll("\\s+","").matches("")) {
            return 0;
        }
        try {
            attributeAsDouble = Double.parseDouble(attribute);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Attribute: \"" + attribute + "\" is invalid, attribute is not a number.");
        }
        //TODO can attribute be 0?
        if (attributeAsDouble < 0) {
            throw new IllegalArgumentException("Attribute: \"" + attribute + "\" is invalid, attributes can't be negative");
        }
        return  attributeAsDouble;
    }

//    /**
//     * Validates and parses a given instance and throws IllegalArgumentExceptions if the instance contains invalid attributes
//     * @param instance The attribute as a string to parse and validate
//     * @return attribute the parsed and validated attribute as a double[]
//     */
//    private static double[] getValidInstance(String instance) {
//        double[] instanceAsDouble = new double[6];
//        String[] instanceAsString = instance.split(",");
//        if (instanceAsString.length < 6) {
//            throw new IllegalArgumentException("Instance is missing attributes. Instance has "
//                    + instanceAsString.length  +" attributes but needs to have 10");
//        } else if (instanceAsString.length > 6) {
//            throw new IllegalArgumentException("Instance contains too many attributes. Instance has "
//                    + instanceAsString.length  +" attributes but needs to have 10");
//        }
//        for (int i = 0; i < instanceAsString.length; i++) {
//            instanceAsDouble[i] = getValidAttribute(instanceAsString[i]);
//        }
//        return instanceAsDouble;
//    }

    private double[] getValidInstance(String instance) {
        double[] instanceAsDouble = new double[attrIndexes.size()];
        String[] instanceAsString = instance.split("[,;]",-1);
        if (instanceAsString.length < 6) {
            throw new IllegalArgumentException("Instance is missing attributes. Instance has "
                    + instanceAsString.length + " attributes but needs to have 6");
        }
        //TODO check if instances are proper
        //TODO skip instances with missing values and warn user
//        } else if (instanceAsString.length > 10) {
//            throw new IllegalArgumentException("Instance contains too many attributes. Instance has "
//                    + instanceAsString.length  +" attributes but needs to have 10");
//        }
/*        for (int i = 0; i < instanceAsString.length; i++) {
            for(String attr: attrIndexes.keySet()) {
                if (instanceAsString[i] == attr) {
                    instanceAsDouble[i] = getValidAttribute(instanceAsString[i]);
                }
            }
        }*/
        for(String attr: this.attrIndexes.keySet()) {
            int i = 0;
            //TODO do with getter?
            instanceAsDouble[i] = getValidAttribute(instanceAsString[this.attrIndexes.get(attr)]);
            i ++;
        }
        return instanceAsDouble;
    }

    /**
     * @param inputFile input file arff as a file path String
     * @param outputCsv output file csv as a file path String
     */
    public void createNormCsvFromArfFile(String inputFile, String outputCsv) {
        //TODO check if works with new format
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            //Create normalized file
            BufferedWriter outputFile = createCsvTemplateFile(outputCsv);
            if ((br.readLine()) == null) {
                throw new IllegalArgumentException("file is empty");
            }
            while ((line = br.readLine()) != null) {
                if (!(line.startsWith("@") || line.isBlank())) {
                    String[] normCsv = normalizeInstance(line);
                    //Write Content
                    String newCSVLine = convertToCSV(normCsv) + "\n";
                    outputFile.write(newCSVLine);
                }
            }
            outputFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * normalises from a csv file and creates a new normalised csv file.
     * @param csvFile :  filepath to the csv file that needs to be normalised
     * @param outputFilePath : filepath to the output csv file that is normalised
     */
    public void createNormCsvFromCsvFile(String csvFile, String outputFilePath) {
        BufferedReader br = null;
        String line;
        String[] rowData;

        try {
            //Create normalized file
            BufferedWriter outputFile = createCsvTemplateFile(outputFilePath);

            //read input
            br = new BufferedReader(new FileReader(csvFile));

            if ((line = br.readLine()) == null) {
                throw new IllegalArgumentException("file is empty");
            }
            else if (line.equals("")) {
                throw new IllegalArgumentException("header is empty. Expected: "+ this.getHeader());
            }
            //TODO look if we can find all attributes needed in the header instead of comparing the enire header
//            else if (line.equals(header) != true) {
//                throw new IllegalArgumentException("Input file doesn't contain the right header. Expected: " + header
//                        + "\n But got: " +  line);
//            }
            while ((line = br.readLine()) != null) {
                rowData = normalizeInstance(line);
                //Write Content
                String newCSVLine = convertToCSV(rowData)+"\n";
                outputFile.write(newCSVLine);
            }
            outputFile.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Normalises one instance and puts it a csv file
     * @param instance : a single instance as a String[] with 10 atributes
     * @param outputFilePath : The file path for the normalised output as a csv
     */
    public void createNormCsvFromInstance(String instance, String outputFilePath){
        String[] normInstance = normalizeInstance(instance);

        BufferedWriter outPutFile = createCsvTemplateFile(outputFilePath);
        String newCSVLine = convertToCSV(normInstance)+"\n";

        try {
            outPutFile.write(newCSVLine);
            outPutFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * set a dictionary with the attribute name as string and its mean as double
     * @param means
     */
    private void setMeans(Hashtable<String, Double>  means) {
        this.means = means;
    }

    /**
     * set a dictionary with the attribute name as string and its standard deviation as double
     * @param sd
     */
    private void setSd(Hashtable<String, Double> sd) {
        this.sd = sd;
    }

    /**
     * @return returns the header of the file that will be normalised
     */
    public String getHeader() {
        return header;
    }
    /**
     * sets the header of the file that will be normalised
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * @param attrIndexes sets a hashtable with each attributes name and index in side of the file that will be
     *                    normalised
     */
    public void setAttrIndexes(Hashtable<String, Integer> attrIndexes) {
        this.attrIndexes = attrIndexes;
    }
}
