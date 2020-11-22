package com.leekwars.generator.genetics;

import com.alibaba.fastjson.JSON;
import java.io.File;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import com.leekwars.generator.Log;
import com.leekwars.generator.outcome.Outcome;
import com.leekwars.generator.Generator;
import com.leekwars.generator.scenario.Scenario;

import leekscript.compiler.RandomGenerator;


public class Chromosome implements Comparable<Chromosome> {

    static public final String TAG = Chromosome.class.getSimpleName();

    public double genes[];
    public double score;
    public boolean isScoreSet;

    private static String []scenarios = {".tmp/test/scenario/scenario_farmer.json", ".tmp/test/scenario/scenario_solo.json"};

    private int m_size;
	private RandomGenerator randomGenerator = new RandomGenerator() {
		private long n = 0;

		public void seed(long seed) {
			n = seed;
		}

		@Override
		public double getDouble() {
            //0 - 1
			n = n * 1103515245 + 12345;
			long r = (n / 65536) % 32768 + 32768;
			return (double) r / 65536;
		}

		@Override
		public int getInt(int min, int max) {
			if (max - min + 1 <= 0)
				return 0;
			return min + (int) (getDouble() * (max - min + 1));
		}
	};
    public double getDouble(double min, double max) {
		if (max - min + 1 <= 0)
			return 0;
        return min + (randomGenerator.getDouble() * (max - min + 1));
    }

    public Chromosome(int size, int seed) {
        m_size = size;
        genes = new double[m_size];
        isScoreSet = false;

		randomGenerator.seed(seed);
    }

    public void set_from_list(List<Double> coeffs) {
        for (int i = 0; i < coeffs.size(); i++) {
            genes[i] = coeffs.get(i);
        }
    }

    public void set_random() {
        //fill with a chromosome of size size with random values
        for (int i = 0; i < genes.length; i++) {
            genes[i] = randomGenerator.getDouble();
        }
    }


    public void set_from_parents(Chromosome parent1, Chromosome parent2) {
        //Fill the genes by taking random genes from parents
        for (int i = 0; i < genes.length; i++) {
            if (randomGenerator.getInt(0, 1) > 0) {
                genes[i] = parent1.genes[i];
            }
            else {
                genes[i] = parent2.genes[i];
            }
        }
    }

    public String toString() {
        return score + " " + Arrays.toString(genes);
    }


    public void mutate(int nbMax, double learningRate) {
        //mutate the chromosome nbMax times
        for (int i = 0; i < nbMax; i++) {
            int rand = randomGenerator.getInt(0, genes.length - 1);
            genes[rand] += getDouble(-learningRate, learningRate);
        }
    }

    public double get_score(int iterations) {
        //Compute a score from N iterations combats against parents
        score = 0.0;
        //for each iteration
        for (int i = 0; i < scenarios.length; i++) {
            //generate a program file with the new values
            
            //create a scenario

            //replace ai path with ou new path

            //get score (1 win, 0.5 draw, 0 loose)
            Outcome res = fight(scenarios[i], false);
            //System.out.println(res.toJson());
    
            //score += calculated score
            if (res.winner == 0) {  //I won
                score += 1.0; 
                score -= res.duration/64.0;
                //System.out.println("Victoire: " + res.winner);
            }
            else if (res.winner < 0) {  //egalite
                score -= 0.5;
                //System.out.println("Egalité: " + res.winner);
            }  
            else if (res.winner == 1) {  //defaite
                score -= 1.0;
                score += res.duration/64.0;
                //System.out.println("Defaite: " + res.winner);
            }
            else
                System.out.println("Error: " + res.winner);
        }
        isScoreSet = true;
        score = score/(double)iterations;
        return score; 
    }

    public Outcome fight(String scenarioFile, boolean verbose) {
        Generator generator = new Generator();
        Scenario scenario = Scenario.fromFile(new File(scenarios[0]));
        Log.i(TAG, "Generator v1");
        if (scenario == null) {
            Log.e(TAG, "Failed to parse scenario!");
            return null;
        }
        return generator.runScenario(scenario, null);
    }
    
    @Override
    public int compareTo(Chromosome c) {
        /*if (!isScoreSet) {
            get_score(1);
        }
        if (!c.isScoreSet) {
            c.get_score(1);
        }*/
        return (int)(100000 * (c.score - score));
    }

}


