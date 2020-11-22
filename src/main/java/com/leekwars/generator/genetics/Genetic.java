package com.leekwars.generator.genetics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import com.leekwars.generator.Log;
import com.leekwars.generator.genetics.Chromosome;
import com.leekwars.generator.genetics.Parseur;

import leekscript.compiler.RandomGenerator;

public class Genetic {
    private static final String TAG = Genetic.class.getSimpleName();
    private static double CHOSEN_KEEP_BEST_PERCENT = 0.3;   //keep 30% of best scores
    private static double CHOSEN_KEEP_RANDOM_PERCENT = 0.2;   //keep 20% of random chromosomes

    private Chromosome ref; //reference that contains the best chromosome of the last iteration
    private boolean m_verbose;
    private int m_popSize;
    private int m_iterations;
    private int chromosomeSize;
    private Parseur parser;
    private List<Chromosome> chromosomePopulation = new ArrayList<Chromosome>();
    private static RandomGenerator randomGenerator = new RandomGenerator() {
        private long n = 0;

        public void seed(long seed) {
            n = seed;
        }

        @Override
        public double getDouble() {
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

    public Genetic(String vectorFile, Parseur p, int iterations, int popSize, boolean verbose) {
        m_verbose = verbose; 
        m_popSize = popSize;
        m_iterations = iterations;
        parser = p;
        List<Double> coefficients = p.get_coefficient_list();
        chromosomeSize = coefficients.size();

        empty_compiled_ia_folder();

        //load chromosome from file
        for (int i = 0; i < popSize; i++) {
            Chromosome c = new Chromosome(chromosomeSize, (int)(randomGenerator.getDouble() * 1000));
            //set initial population from initial vector
            //c.set_from_list(coefficients);
            c.set_random();
            //mutate it a little bit
            //c.mutate(3, 1);
            chromosomePopulation.add(c);
        }

        double learningRate = 10.0;
        //begin iterations
        for (int i = 0; i < iterations; i++) {
            System.out.println("Iteration " + i + " " +learningRate);
            iterate(i, learningRate);
            learningRate *= (double)(iterations - i)/(double)iterations;
            parser.recreate_file_new_coeffs("best_score.leek", chromosomePopulation.get(0).genes);
        }
        parser.recreate_file_new_coeffs(p.m_fileToParse, chromosomePopulation.get(0).genes);
    }


    public void iterate(int iteration, double learningRate) {
        //get next population
        if (iteration == 0) {
            System.out.println("Vector loaded");
            return;
        }
        List<Chromosome> newPop = create_next_population(chromosomePopulation, learningRate);

        //check this population scores
        double mean = 0.0;
        int index = 0;
        String []rot = {"|", "/", "-", "\\"};
        for (Chromosome c: newPop) {
            System.out.print("\r");
            System.out.print(index+1 + "/" + newPop.size() + " " + rot[index%rot.length]);
            parser.recreate_file_new_coeffs(parser.m_fileToParse, c.genes);
            empty_compiled_ia_folder(); //force recompile
            mean += c.get_score(1);
            index += 1;
        }
        Collections.sort(newPop);   //sort population by score
        ref = newPop.get(0);        //get best score

        //print chromosomes
        /*
        for (Chromosome c: newPop) {
            System.out.println(c.toString());
        }
        */

        //evaluate the mean score of the new pop against the best of last pop
        System.out.println("\rIteration " + iteration + " max score: " + ref.score + " mean score: " + mean/(double)newPop.size() + "\n");
        chromosomePopulation = newPop;
        empty_compiled_ia_folder();
    }

    public List<Chromosome> create_next_population(List<Chromosome> population, double mutationRate) {
        List<Chromosome> chosen = new ArrayList<Chromosome>();
        int bestIndexes = (int)(population.size() * CHOSEN_KEEP_BEST_PERCENT);
        Collections.sort(population);   //sort population by score

        //get a % of the best scores
        for (int i = 0; i < bestIndexes; i++) {
            population.get(i).isScoreSet = false;
            chosen.add(population.get(i));
        }

        //get a % of random results
        for (int i = 0; i < (int)(population.size() * CHOSEN_KEEP_RANDOM_PERCENT); i++) {
            int rIndex = randomGenerator.getInt(
                    bestIndexes,
                    population.size() - 1
                    );
            population.get(rIndex).isScoreSet = false;
            chosen.add(population.get(rIndex));
        }

        int limit = population.size() - chosen.size();
        //generate new chromosomes from those in the chosen list
        for (int i = 0; i < limit; i++) {
            Chromosome c = new Chromosome(chromosomeSize, (int)(randomGenerator.getDouble()*1000));
            //get 2 randoms
            int index1 = randomGenerator.getInt(0, chosen.size() - 1);
            int index2 = randomGenerator.getInt(0, chosen.size() - 1);

            c.set_from_parents(chosen.get(index1), chosen.get(index2));
            c.mutate(chromosomeSize/2, mutationRate) ;
            chosen.add(c);
        }
        return chosen;
    }


    private void empty_compiled_ia_folder() {
        //remove compiled ais 
        String ais = "ai";  //compiled ais
        File index = new File(ais);
        for (String str: index.list()) {
            File f = new File(index.getPath(), str);
            if (! f.delete()) 
                System.out.println("Could not remove " + str);
        }
    }

}
