/*
 * Copyright (c) 2020 Jimjim Valkema
 * All rights reserved
 */

package WekaJavaWrapper;

import weka.classifiers.meta.Stacking;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.core.converters.ConverterUtils;

import java.io.*;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WekaWrapperRunner {
    //private static final Logger log = Logger.getLogger(WekaWrapperRunner.class.getName());
    //private String[] args = null;
    //private Options options = new Options();
    //todo apachi cli arguments
    //todo clean up
    //todo readme
    //TODO better comments remove useless code

    //TODO check if csv and if data range is normal ex 0 or -1
    //TODO create options to disable scaling
    //TODO make printing distributions optional
    //TODO show help when failing to parse options
    //TODO arf mischien??
    //TODO print statistics
    //TODO print probability
    //TODO versioning

    //TODO putt normalisation functions in a separate class

    //TODO change to new model and class labels
    private final String modelFile = "MetaStackingRandomForest-j48-randomForest-bayes-logistic-(j48_cfs_subset).model";
    public final String classLabels = "low,mid,high";
    //TODO get new means and sds
    //B.PUFAS.gr           B.Calcium  B.Carbohydrates.gr   B.Linoleicacid.gr
    public final static double[] means = {3.9372174,9.7965213,7.7369304,3.6468098};
    public final static double[] sd = {0.6352127,0.5101335,0.4715097,0.6506662};
    public static Hashtable<String, Integer> attrIndexes = new Hashtable<String, Integer>();
    //Hashtable<String, String> hm = new Hashtable<String, String>();

    public static void main(String[] args) {
        //TODO get new header possibly from a file since it is long
        String header = "\"B.PUFAS.gr\"\"B.Calcium\"\"B.Carbohydrates.gr\"\"B.Linoleicacid.gr\"\"Gender\"\"Neocate\"";
        String[] extractingAttr = {"B.PUFAS.gr", "B.Calcium","B.Carbohydrates.gr","B.Linoleicacid.gr","Gender","Neocate"};
        try {
            ApacheCliOptionsProvider op = new ApacheCliOptionsProvider(args);
            if (op.helpRequested() || op.haNosUserOptions()) {
                op.printHelp();
                return;
            } else {
                WekaWrapperRunner runner = new WekaWrapperRunner();
                String normalizedCsv = "testdata/unclassified_normalised_data.csv";

                //normalise from either the terminal or CSV and store the output into a csv
                if (op.instanceProvided()) {
                    System.out.println("classifying instance from terminal");
                    attrIndexes = getAttrIndexes(header,extractingAttr);
                    String instance = op.getInstance();
                    runner.createNormCsvFromInstance(instance, normalizedCsv,header);
                } else {
                    //TODO detect if it is a csv or arff
                    String inputFile = op.getInputfile();
                    header = getHeader(inputFile);
                    attrIndexes = getAttrIndexes(header,extractingAttr);
                    createNormArffFromFile(inputFile,normalizedCsv,means,sd,header);
                    System.out.println("classifying instance from CSV");
                }
                runner.classifyCsv(normalizedCsv);
            }
        } catch (Throwable ex) {
            System.err.println("Something went wrong while processing your command line \""
                    + Arrays.toString(args) + "\"");
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
            ApacheCliOptionsProvider op = new ApacheCliOptionsProvider(new String[]{});
            op.printHelp();
        }
    }

    /**
     * Normalises one instance and puts it a csv file
     * @param instance : a single instance as a String[] with 10 atributes
     * @param outputFilePath : The file path for the normalised output as a csv
     * @param header : The header of the csv containing the names of all the attribute as a String
     */
    private void createNormCsvFromInstance(String instance, String outputFilePath, String header){
        String[] normInstance = normalizeInstance(instance, means, sd);

        BufferedWriter outPutFile = createCsvTemplateFile(outputFilePath, header);
        String newCSVLine = convertToCSV(normInstance)+"\n";

        try {
            outPutFile.write(newCSVLine);
            outPutFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String getHeader(String fileLocation) throws IOException {
        BufferedReader brTest = new BufferedReader(new FileReader(fileLocation));
        return brTest.readLine();
    }

    private static Hashtable<String, Integer> getAttrIndexes(String header, String[] extractingAttr) {
        Hashtable<String, Integer> attrIndexes = new Hashtable<String, Integer>();
        String[] splitHeader = header.split(",");

        for (int i = 0; i < splitHeader.length; i++) {
            for (int j = 0; j < extractingAttr.length; j++) {
                if (splitHeader[i] == extractingAttr[j]){
                    attrIndexes.put(extractingAttr[j],i);
                }

            }
        }

        return attrIndexes;
    }


    /**
     * classifies from a normalised csv and outputs the results in the terminal
     * @param normalizedCsv :  The filepath to the normalised csv as a String
     */
    private void classifyCsv(String normalizedCsv) {

        try {
            Stacking classifierFromFile = loadClassifier();
            CSVLoader csvLoader = new CSVLoader();
            csvLoader.setSource(new File(normalizedCsv));
            csvLoader.setNominalAttributes("first-last");
            Instances unClassifiedInstancesFromCsv = csvLoader.getDataSet();
            if (unClassifiedInstancesFromCsv.classIndex() == -1)
                unClassifiedInstancesFromCsv.setClassIndex(unClassifiedInstancesFromCsv.numAttributes() - 1);


            //define attribute and its labels, name etc
            Instances newData = new Instances(unClassifiedInstancesFromCsv);
            // 1. nominal attribute
            Add filter = new Add();
            filter.setAttributeIndex("last");
            filter.setNominalLabels(this.classLabels);
            filter.setAttributeName("type");
            filter.setInputFormat(newData);
            newData = Filter.useFilter(newData, filter);

            newData.setClassIndex(newData.numAttributes() - 1);

            classifyNewInstance(classifierFromFile, newData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * classifies instances and outputs the results in the terminal
     * @param stack : The stack machine learning model as a Stack object
     * @param unClassifiedInstances : a Instances object that contains the instances
     * @throws Exception
     */
    private void classifyNewInstance(Stacking stack, Instances unClassifiedInstances) throws Exception {
        // create copy
        Instances labeled = new Instances(unClassifiedInstances);
        // label instances
        for (int i = 0; i < unClassifiedInstances.numInstances(); i++) {
            double clsLabel = stack.classifyInstance(unClassifiedInstances.instance(i));
            labeled.instance(i).setClassValue(clsLabel);
        }
        for (int i = 0; i < labeled.size(); i++) {
            System.out.println("instance "+ (i+1) + " classified as: " + labeled.get(i).stringValue(10));
            System.out.println("With the probability distribution of: ");
            double[] distributionForInstance = stack.distributionForInstance(labeled.instance(i));
            for (int j = 0; j < distributionForInstance.length; j++) {
                System.out.println(this.classLabels.split(",")[j] + ": " + distributionForInstance[j]);
            }
        }
    }

    /**
     * Loads the model from a .model file and creates a Stacking 0bject
     * @return
     * @throws Exception
     */
    private Stacking loadClassifier() throws Exception {
        // deserialize model
        ClassLoader classLoader = getClass().getClassLoader();
        //todo get file from Resource
        //File file = new File(classLoader.getResource("j48_resource.model").getFile());
        File file = new File(modelFile);

        return (Stacking) weka.core.SerializationHelper.read(file.getAbsolutePath());
    }

    /**
     * does a log2 transform
     * @param x :  a double to be transformed
     * @return
     */
    public static double log2(double x)
    {
        return (Math.log(x) / Math.log(2));
    }

    /**
     * @param data : as a string[]
     * @return : a String with a items comma separated
     */
    public static String convertToCSV(String[] data) {
        return Stream.of(data).collect(Collectors.joining(","));
    }

    /**
     * Normalises one instance from a String[] and returns it a list<String>
     * @param instance :  the instance as a String
     * @param means : The means from the training data as a double[]
     * @param sd : The standard deviation from the training data as a double[]
     * @return : normalised instance as a list<String>
     */
    private static String[] normalizeInstance(String instance, double[] means, double[] sd) throws NumberFormatException {
        double[] validInstance = getValidInstance(instance);
        double res;

        //TODO some instances are not numerical and can't / need to be normalised
        List<String> normalizedInstanceList = new ArrayList<String>();
        String[] normalizedInstance = new String[4];
            for (int i = 0; i < validInstance.length - 2; i ++) {
                res = log2(validInstance[i]);
                res = (res - means[i]) / sd[i];
                normalizedInstanceList.add(String.valueOf(res));
            }
        List<double> nonNumerics = Arrays.asList(Arrays.copyOfRange(validInstance,4,6));
        return normalizedInstanceList.addAll(nonNumerics);
    }

    /**
     * Creates a csv file with a given header. over writes if it already exists
     * @param outputFilePath : The file path to create the file at as a String
     * @param header : The header as a string
     * @return :  the file as a buffer
     */
    private static BufferedWriter createCsvTemplateFile(String outputFilePath, String header) {
        try {
            //Create normalized file
            File outputFile = new File(outputFilePath);
            BufferedWriter outPutFile = new BufferedWriter(
                    new FileWriter(outputFilePath, true));
            outPutFile.write(header + "\n");
            if (outputFile.createNewFile()) {
            } else {
                //TODO make it optional? (probably not possible)
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

    private static double getValidAttribute(String attribute) {
        double attributeAsDouble;
        try {
            attributeAsDouble = Double.parseDouble(attribute);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Attribute: \"" + attribute + "\" is invalid, attribute is not a number.");
        }
        if (attributeAsDouble <= 0) {
            throw new IllegalArgumentException("Attribute: \"" + attribute + "\" is invalid, attributes can't be negative or zero");
        }
        return  attributeAsDouble;
    }

    private static double[] getValidInstance(String instance) {
        double[] instanceAsDouble = new double[6];
        String[] instanceAsString = instance.split(",");
        if (instanceAsString.length < 6) {
            throw new IllegalArgumentException("Instance is missing attributes. Instance has "
                    + instanceAsString.length + " attributes but needs to have 10");
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
        for(String attr: attrIndexes.keySet()) {
            int i = 0;
            instanceAsDouble[i] = getValidAttribute(instanceAsString[attrIndexes.get(attr)]);
            i ++;
        }
        return instanceAsDouble;
    }

    private static void createNormArffFromFile(String inputFile, String outputCsv, double[] means, double[] sd, String header) {
        Instances instances = null;
        try {
            instances = new ConverterUtils.DataSource(inputFile).getDataSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            //Create normalized file
            //TODO DON't use weka lol
            BufferedWriter outputFile = createCsvTemplateFile(outputCsv, header);
            for (String instance : instances.toString().split("\n")) {
                if (!(instance.startsWith("@") || instance.isBlank())) {
                    System.out.println(instance);
                    String[] normCsv = normalizeInstance(instance, means, sd);
                    //Write Content
                    String newCSVLine = convertToCSV(normCsv)+"\n";
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
     * @param header : The header as a string
     */
    private void createNormCsvFromFile(String csvFile, String outputFilePath, String header) {

        BufferedReader br = null;
        String line;
        String[] rowData;

        try {
            //Create normalized file
            BufferedWriter outputFile = createCsvTemplateFile(outputFilePath, header);

            //read input
            br = new BufferedReader(new FileReader(csvFile));

            if ((line = br.readLine()) == null) {
                throw new IllegalArgumentException("file is empty");
            }
            else if (line.equals("")) {
                throw new IllegalArgumentException("header is empty. Expected: "+ header);
            }
            else if (line.equals(header) != true) {
                throw new IllegalArgumentException("Input file doesn't contain the right header. Expected: " + header
                + "\n But got: " +  line);
            }
            while ((line = br.readLine()) != null) {

                rowData = normalizeInstance(line, means,sd);

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
}
