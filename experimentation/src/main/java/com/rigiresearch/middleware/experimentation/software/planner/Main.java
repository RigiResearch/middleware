package com.rigiresearch.middleware.experimentation.software.planner;

import com.rigiresearch.middleware.experimentation.software.planner.generation.Chromosome;
import com.rigiresearch.middleware.experimentation.software.planner.generation.Constraint;
import com.rigiresearch.middleware.experimentation.software.planner.generation.ExclusionConstraint;
import com.rigiresearch.middleware.experimentation.software.planner.generation.GeneticGeneration;
import com.rigiresearch.middleware.experimentation.software.planner.generation.PositionConstraint;
import com.rigiresearch.middleware.experimentation.software.planner.generation.PrecedenceConstraint;
import com.rigiresearch.middleware.experimentation.software.planner.generation.UniqueConstraint;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Main class of the program.
 *
 * @author lfrivera
 * @version $Id$
 * @since 0.1.0
 */
public class Main {

    /**
     * Main entry point of the program.
     *
     * @param args The argument to pass to the program.
     */
    public static void main(String[] args) {

        boolean error = false;
        String userMessage = "Please provide the location of the file containing the list of patterns and system elements.";
        String[] sysElements = null;
        String[] patterns = null;
        Integer generationDepth = null;

        final List<Constraint> constraints = new ObjectArrayList<>();
        // Alternative patterns must be exclusive (only one may be applied)
        constraints.add(new ExclusionConstraint("Master-Worker", "Producer-Consumer"));
        // Guarantee a unique instance of MW, PD and RLP
        constraints.add(new UniqueConstraint("Master-Worker"));
        constraints.add(new UniqueConstraint("Producer-Consumer"));
        constraints.add(new UniqueConstraint("Rate-Limiting-Proxy"));
        // No pattern can precede the RLP
        constraints.add(new PositionConstraint("Rate-Limiting-Proxy", 1));
        // MW or PC must not be load-balanced
        // The distance guarantees that the load-balanced element must precede the MW or PC
        constraints.add(new PrecedenceConstraint("Master-Worker", "Load-Balancer", 2));
        constraints.add(new PrecedenceConstraint("Producer-Consumer", "Load-Balancer", 2));
        // Avoid a redundant Load-Balancer after MW or PC
        constraints.add(new PrecedenceConstraint("Load-Balancer", "Master-Worker"));
        constraints.add(new PrecedenceConstraint("Load-Balancer", "Producer-Consumer"));

        // Validating arguments
        if (args == null) {

            error = true;

        } else {

            if (args.length < 1) {

                error = true;

            } else {

                int argsLength = args.length;
                Object[] loadedElements = null;

                switch (argsLength) {

                    // File
                    case 1:

                        File file = new File(args[0]);

                        // Validating whether the file exists
                        if (!file.exists()) {
                            error = true;
                            userMessage = "Unable to locate the specified file.";
                        } else {

                            loadedElements = loadElementsAndPatterns(file);

                        }

                        break;

                    // List of patterns and list of system elements or generation depth.
                    case 2:

                        try {

                            int depthArgument = Integer.parseInt(args[1]);

                            if (depthArgument < 1) {

                                error = true;
                                userMessage = "The generation depth argument must be greater than 0.";

                            } else {

                                generationDepth = depthArgument;

                                File theFile = new File(args[0]);

                                // Validating whether the file exists
                                if (!theFile.exists()) {
                                    error = true;
                                    userMessage = "Unable to locate the specified file.";
                                } else {

                                    loadedElements = loadElementsAndPatterns(theFile);

                                }

                            }

                        } catch (NumberFormatException e) {


                            loadedElements = loadElementsAndPatterns(args[0], args[1]);

                        }

                        break;

                    case 3:

                        try {

                            int depthArgument = Integer.parseInt(args[2]);

                            if (depthArgument < 1) {

                                error = true;
                                userMessage = "The generation depth argument must be greater than 0.";

                            } else {

                                generationDepth = depthArgument;
                                loadedElements = loadElementsAndPatterns(args[0], args[1]);

                            }

                        } catch (ClassCastException e) {


                            loadedElements = loadElementsAndPatterns(args[0], args[1]);

                        }

                        break;

                    default:

                        error = true;
                        userMessage = "Invalid number of parameters.";

                        break;

                }

                if (error != true) {

                    // Validating the extracted patterns and initial system elements.

                    if (loadedElements == null) {
                        error = true;
                        userMessage = "Error loading the system elements and the patterns to apply.";

                    } else {

                        sysElements = (String[]) loadedElements[0];
                        patterns = (String[]) loadedElements[1];

                        if (sysElements.length < 2 || patterns.length < 1) {

                            error = true;
                            userMessage = "Not enough system elements or patterns to perform the combinations.";

                        } else {

                            GeneticGeneration<String> generation = new GeneticGeneration<String>(sysElements, patterns, generationDepth, constraints);

                            int i = 1;
                            StringBuilder stringBuilder = new StringBuilder("");
                            boolean first = true;

                            try {

                                for (Chromosome chromosome : generation.generatePopulation()) {

                                    String chromosomeString = i + ". " + chromosome.toString(Character.toString((char) 62));

                                    if (first) {

                                        stringBuilder.append(chromosomeString);
                                        first = false;

                                    } else {

                                        stringBuilder.append("\n");
                                        stringBuilder.append(chromosomeString);

                                    }
                                    i++;
                                }

                                processOutput(stringBuilder.toString());

                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                            }


                        }

                    }

                }

            }

        }

        //Showing message to the user
        if (error) {

            System.out.println(userMessage);

        }

    }

    /**
     * Allows to read the system elements and the patterns to apply from a text file.
     *
     * @param file The text file containing the system elements and the patterns to apply.
     * @return The system elements and the patterns to apply from a text file.
     */
    private static Object[] loadElementsAndPatterns(File file) {

        Object[] response = new Object[2];

        BufferedReader br = null;

        try {

            br = new BufferedReader(new FileReader(file));
            String line1 = null;
            String line2 = null;

            for (int i = 0; i < 2; i++) {

                if (i == 0) {

                    line1 = br.readLine();

                } else {

                    line2 = br.readLine();

                }
            }

            response = loadElementsAndPatterns(line1, line2);

        } catch (Exception e) {

            response = null;

        } finally {
            try {

                br.close();

            } catch (IOException e) {

                e.printStackTrace();

            }
        }

        return response;
    }

    /**
     * Allows to read the system elements and the patterns to apply from the arguments passed to the program.
     *
     * @param sysElements The systems elements provided by the user.
     * @param patterns    The patterns to apply provided by the user.
     * @return The system elements and the patterns to apply from the arguments passed to the program.
     */
    private static Object[] loadElementsAndPatterns(String sysElements, String patterns) {

        Object[] response = new Object[2];
        response[0] = sysElements.split(",");
        response[1] = patterns.split(",");

        return response;

    }

    /**
     * Allows to process the program output.
     *
     * @param programOutput The output to be processed.
     */
    private static void processOutput(String programOutput) {

        // Printing the output using the console
        System.out.println(programOutput);

        // Creating a new file with the program output
        String fileName = "generatedConfigurations_output.txt";
        File file = new File(fileName);

        if (file.exists()) {

            file.delete();

        }

        String fileWritingMessage = null;

        try {

            file.createNewFile();
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(programOutput);
            fileWritingMessage = "Output file created: " + file.getAbsolutePath();
            fileWriter.close();

        } catch (IOException e) {

            fileWritingMessage = "An error occurred when accessing the output file:\n" + e.getMessage();

        } finally {

            System.out.println(fileWritingMessage);

        }


    }


}


