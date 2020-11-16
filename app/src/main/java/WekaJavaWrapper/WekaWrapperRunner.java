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

import java.io.*;
import java.util.*;
import java.io.IOException;

public class WekaWrapperRunner {
    //todo apachi cli arguments
    //todo clean up
    //todo readme
    //TODO better comments remove useless code

    //TODO make printing distributions optional
    //TODO print statistics
    //TODO versioning

    private final String modelFile = "MetaStackingRandomForest-j48-randomForest-bayes-logistic-(j48_cfs_subset).model";
    public final String classLabels = "low,mid,high";
    public final static String[] extractingAttr = {"B.PUFAS.gr", "B.Calcium","B.Carbohydrates.gr","B.Linoleicacid.gr","Gender",
            "Neocate"};

    public final static Hashtable<String, Double>  means = new Hashtable<>() {
            {put("B.PUFAS.gr", 3.9372174); put("B.Calcium", 9.7965213);
            put("B.Carbohydrates.gr", 7.7369304); put("B.Linoleicacid.gr", 3.6468098); }};

    public final static Hashtable<String, Double> sd = new Hashtable<>() {
        { put("B.PUFAS.gr", 0.6352127); put("B.Calcium", 0.5101335);
        put("B.Carbohydrates.gr", 0.4715097); put("B.Linoleicacid.gr", 0.4715097); }};

    public static Hashtable<String, Integer> attrIndexes = new Hashtable<String, Integer>();


    public static void main(String[] args) {
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
                    String header = String.join(",",extractingAttr);
                    attrIndexes = getAttrIndexes(header,extractingAttr);
                    Normalizer normalizer = new Normalizer(means,sd,header,attrIndexes);
                    System.out.println("classifying instance from terminal");
                    String instance = op.getInstance();
                    normalizer.createNormCsvFromInstance(instance, normalizedCsv);
                } else {
                    String header = getHeader(op.getInputfile());
                    attrIndexes = getAttrIndexes(header,extractingAttr);
                    Normalizer normalizer = new Normalizer(means,sd,header,attrIndexes);
                    String inputFile = op.getInputfile();
                    if (inputFile.toLowerCase().endsWith(".csv")) {
                        System.out.println("started normalizing");
                        normalizer.createNormCsvFromCsvFile(inputFile, normalizedCsv);
                    } else if (inputFile.toLowerCase().endsWith("arff")) {
                        normalizer.createNormCsvFromArfFile(inputFile,normalizedCsv);
                    } else {throw new IllegalAccessException("File type is unknown");}
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
     * get the header from a given csv file
     * @param fileLocation
     * @return
     * @throws IOException
     */
    private static String getHeader(String fileLocation) throws IOException {
        BufferedReader brTest = new BufferedReader(new FileReader(fileLocation));
        return brTest.readLine();
    }

    /**
     * Retrieves the indexes of the attributes needed for classification
     * @param header
     * @param extractingAttr
     * @return
     */
    private static Hashtable<String, Integer> getAttrIndexes(String header, String[] extractingAttr) {
        Hashtable<String, Integer> attrIndexes = new Hashtable<String, Integer>();
        String[] splitHeader = header.split("[,;]");
        for (int i = 0; i < splitHeader.length; i++) {
            for (int j = 0; j < extractingAttr.length; j++) {
                if (splitHeader[i].replaceAll(" ","").matches(extractingAttr[j])){
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
            System.out.println("instance "+ (i+1) + " classified as: " + labeled.get(i).stringValue(6));
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

}
