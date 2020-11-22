package com.leekwars;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.alibaba.fastjson.JSON;
import com.leekwars.generator.Log;
import com.leekwars.generator.genetics.Genetic;
import com.leekwars.generator.genetics.Parseur;

public class Main {
    private static final String TAG = Main.class.getSimpleName();

    public static void main(String[] args) {
        boolean verbose = false;
        int iterations = Integer.parseInt(System.getProperty("i")); //iterations
        int populationSize = Integer.parseInt(System.getProperty("p")); //iterations
        String file = System.getProperty("c");                      //coefficients file
        String scenario = "test/scenario"; //System.getProperty("s");                  //scenario
        String iaFolder = "test";//System.getProperty("f");                  //ia folder

		for (String arg : args) {
			if (arg.startsWith("--")) {
				switch (arg.substring(2)) {
					case "verbose": verbose = true; break;
				}
			}
		}
        Log.enable(verbose);

        System.out.println(iaFolder);

        //copy ia to destination folder
        String destination = ".tmp";
        try {
            File f = new File(destination);
            if (!f.exists()) 
                f.mkdir();
            copyFolder(new File(iaFolder), new File(destination + "/test"));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(destination + "/" + file);
        Parseur p = new Parseur(destination + "/" + file);
        Genetic gen = new Genetic(scenario, p, iterations, populationSize, verbose);

        //System.out.print(JSON.toJSONString(gen.fight(file, true), false));
    }


    public static void copyFolder(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }
            for (String f: source.list()) {
                File srcFile = new File(source, f);
                File destFile = new File(dest, f);
                
                copyFolder(srcFile, destFile);
            }
        }
        else {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}


