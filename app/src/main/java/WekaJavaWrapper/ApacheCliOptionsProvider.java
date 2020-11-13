/*
 * Copyright (c) 2020 Jimjim Valkema
 * All rights reserved
 * www.bioinf.nl, www.cellingo.net
 */
package WekaJavaWrapper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * This class implements the OptionsProvider interface by parsing the passed command line arguments.
 *
 */
public class ApacheCliOptionsProvider {
    private static final String HELP = "help";
    private static final String VERBOSITY = "verbosity";
    private static final String INPUTFILE = "inputfile";
    private static final String INSTANCE = "instance";

    private final String[] clArguments;
    private Options options;
    private CommandLine commandLine;

    /**
     * constructs with the command line array.
     *
     * @param args the CL array
     */
    public ApacheCliOptionsProvider(final String[] args) {
        this.clArguments = args;
        initialize();
    }

    public boolean haNosUserOptions() {
        return clArguments.length == 0;
    }

    /**
     * Options initialization and processing.
     */
    private void initialize() {
        buildOptions();
        processCommandLine();
    }

    /**
     * check if help was requested; if so, return true.
     * @return helpRequested
     */
    public boolean helpRequested() {
        return this.commandLine.hasOption(HELP);
    }

    /**
     * builds the Options object.
     */
    private void buildOptions() {
        // create Options object
        this.options = new Options();
        Option helpOption = new Option("h", HELP, false, "Prints this message");
        //TODO implement verbosity
        Option levelOption = new Option("v", VERBOSITY, true, "[NOT IMPLEMENTED]Verbosity level; choose "
                + "\n1: Quiet (or the strong silent type)\n2: normal\n3: talk too much\nDefaults to normal");
        Option inputFileOption = new Option("f", INPUTFILE, true, "File path to input file in csv format");
        Option instanceOption = new Option("i", INSTANCE, true, "One instance to be classified (comma separated values)");
        options.addOption(helpOption);
        //options.addOption(levelOption);
        options.addOption(inputFileOption);
        options.addOption(instanceOption);
    }

    /**
     * processes the command line arguments.
     */
    private void processCommandLine() {
        try {
            CommandLineParser parser = new DefaultParser();
            this.commandLine = parser.parse(this.options, this.clArguments);

            if (commandLine.hasOption(VERBOSITY)) {
                String s = commandLine.getOptionValue(VERBOSITY).trim();
                if (isLegalVerbosityValue(s)) {
                    int i = Integer.parseInt(s);
                    int levelindex = Integer.parseInt(commandLine.getOptionValue(VERBOSITY).trim());
                } else {
                    throw new IllegalArgumentException("Verbosity argument is not legal: \"" + s + "\"");
                }
            } else {
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * legal values are 1, 2, 3
     * @return
     * @param s
     */
    private boolean isLegalVerbosityValue(String s) {
        try{
            int i = Integer.parseInt(s);
            if (i > 0 && i <= 3) {
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException ex) {
            return false;
        }
    }
    /**
     * prints help.
     */
    public void printHelp() {
        //TODO number of classes wil be different
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "gjavaWrapper-1.0-SNAPSHOT-all.jar -f inputfile.csv or \n" +
                        " javaWrapper-1.0-SNAPSHOT-all.jar -i 1,2,3,4,5,6,7,8,9,10", options);
    }

    /**
     * getter for the inputFile option
     * @return inputFile path as a string
     */
    //TODO check if csv
    public String getInputfile() throws FileNotFoundException {
        if (new File(this.commandLine.getOptionValue(INPUTFILE)).exists()) {
            return this.commandLine.getOptionValue(INPUTFILE);

        } else {
            throw new FileNotFoundException("couldn't find file: "+this.commandLine.getOptionValue(INPUTFILE));
        }
    }

    /**
     * getter for the instance option
     * @return instance as a string[]
     */
    public String getInstance() {
        return this.commandLine.getOptionValue(INSTANCE);
    }

    /**
     * to check if a instance is provided
     * @return returns bool
     */
    public boolean instanceProvided() {
        return this.commandLine.hasOption(INSTANCE);
    }


}
