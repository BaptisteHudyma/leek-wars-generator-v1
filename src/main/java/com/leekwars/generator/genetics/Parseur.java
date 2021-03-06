package com.leekwars.generator.genetics;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Parseur {
//parse a file containing vectors to train on
    public String m_fileToParse;

    private List<Double> coefficients;
    private List<String> lines;
    //match a number. Can be have a - and a decimal place
    private static Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public Parseur (String fileToParse) {
        lines = parse_file(fileToParse);
        coefficients = get_double_vector(lines);
        m_fileToParse = fileToParse;
    }

    public List<Double> get_coefficient_list() {
        return coefficients;
    }


    public void recreate_file_new_coeffs(String destinationFolder, double []newCoeffs) {
        //System.out.println(destinationFolder);
        try {
            File newFile = new File(destinationFolder);
            newFile.createNewFile();
        }
        catch (IOException e) {
            e.printStackTrace(); 
        }
        try {
            FileWriter writer = new FileWriter(destinationFolder);
            int newCoeffIndex = 0;
            //each line of the original file
            for (String line:  lines) {
                String copy = "";

                Matcher matcher = pattern.matcher(line);
                int lastIndex = 0;
                //each coefficient
                while (matcher.find()) {
                    copy += line.substring(lastIndex, matcher.start())
                        + newCoeffs[newCoeffIndex];

                    lastIndex = matcher.end();
                    newCoeffIndex+=1;
                }
                copy += line.substring(lastIndex) + "\n";
                //System.out.println(copy);

                writer.write(copy);
            }
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private List<Double> get_double_vector(List<String> lines) {
        List<Double> coeffs = new ArrayList<Double>();

        for(String line: lines) {
            Matcher matcher = pattern.matcher(line);
            //for each line, get doubles
            while (matcher.find()) {
                try {
                    double coeff = Double.parseDouble(line.substring(matcher.start(), matcher.end()));
                    coeffs.add(coeff);
                }
                catch (NumberFormatException e) {
                    continue;   //could not parse this double
                }
            }
        }
        return coeffs;
    }


    private List<String> parse_file(String name) {

        FileInputStream stream = null;
        try {
            stream = new FileInputStream(name);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        ArrayList<String> lines = new ArrayList<String>();
        try {
            while((line = reader.readLine()) != null) {
                lines.add(line);
            }
            stream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }




}
