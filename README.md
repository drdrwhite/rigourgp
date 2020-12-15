# Resources for "A Rigorous Evaluation of Crossover and Mutation in Genetic Programming"

Authors: David R. White and Simon Poulding

Update: 15/12/2020. I (David) updated this page with a feeling of melancholy, remembering working with the late, great, Simon Poulding. I know that Simon would have chastised me for letting these resources fall to "bitrot" as we even discussed at the time... sorry Simon, I've put things in a git repo, which I know you'd approve of - perhaps to be further improved at a later date.

This [paper](http://crest.cs.ucl.ac.uk/fileadmin/crest/sebasepaper/WhiteP09.pdf) compared mutation-only GP with crossover-only GP, across a range of problems taken from the [ECJ Toolkit](http://cs.gmu.edu/~eclab/projects/ecj/). Please note that *Version 16* of the toolkit was used.

This webpage contains online resources designed to enable other researchers to recreate our work.

We actively encourage feedback, debate, suggestions and constructive criticism: please feel free to email me [(David)](mailto:d.r.white@sheffield.ac.uk)

## Overview

Matlab was used to generate CSV files describing experiments to be run. These files were then run through ECJ by using a subclass of ec.Evolve. The results were written out in CSV format and read back in Matlab, which was used to perform all the analysis reported in the paper.

Each example problem given in the ec.app package of the ECJ 16 distribution was assigned a number, and our published results examine a subset of these problems. We collapsed the parameter file hierarchies for each problem into a single ECJ parameter file, and these files are required by BatchEvolve: they can be downloaded below. Note that for problem 9, the Santa Fe Ant Trail, the trail file provided with ECJ must be in the same directory as these parameter files.

## File Formats
We used a simple CSV file format to describe experiments to be run, produced by Matlab, and to return the results of those experiments. Each row in the file corresponds to a single experiment. Here's an extract of an input batch file given to test.BatchEvolve:

```
ID,Algorithm,Problem,seed.0,gp.koza.half.growp,gp.koza.half.max-depth,gp.koza.half.min-depth, gp.koza.ns.root,gp.koza.ns.terminals,gp.koza.xover.maxdepth,gp.koza.mutate.maxdepth, pop.subpop.0.size,pop.subpop.0.species.pipe.source.0.prob,select.tournament.size, gp.koza.grow.min-depth,gp.koza.grow.max-depth,generations,gp.koza.ns.nonterminals, pop.subpop.0.species.pipe.source.1.prob,pop.subpop.0.species.pipe.source.0 
101010001,C,1,84125490,0.5,10,5,0.5,0.05,19,NaN,761,0.55,2,NaN,NaN,69,0.45,0.45,ec.gp.koza.CrossoverPipeline
101010002,C,1,-1574848641,0.5,7,4,0,0.5,5,NaN,30,0.55,6,NaN,NaN,1750,0.5,0.45,ec.gp.koza.CrossoverPipeline
101010003,C,1,1429046270,0.5,10,8,0.25,0.375,19,NaN,1500,0.55,9,NaN,NaN,35,0.375,0.45,ec.gp.koza.CrossoverPipeline
101010004,C,1,1048161384,0.5,10,3,0.5,0.15,19,NaN,1500,1,6,NaN,NaN,35,0.35,0,ec.gp.koza.CrossoverPipeline

```

The first line (split over multiple lines in this page) is a header: the first three columns are specific to our work, the remainder are the ECJ names of parameters we were manipulating in our experimentation, and more information can be found in the ECJ Class documentation. ID allowed us to identifiy a single experiment, Algorithm indicates whether we're considering (C)rossover only, (M)utation only, or (D)efault ECJ parameter values. Problem is the problem number corresponding to the parameter files below.

NaN is used to indicate where a field is not applicable for that particular algorithm. Note that the seeds are included in the file, so to carry out multiple repetitions we gave multiple lines with the same parameters and a different seed. The file includes dependent parameter settings, which are derived from other parameters: see the paper for further details. The role of the final column is to select between mutation and crossover as the exclusive genetic operator used.

The corresponding output file from test.BatchEvolve is as follows:

```
ID,Algorithm,Problem,seed.0,gp.koza.half.growp,gp.koza.half.max-depth,gp.koza.half.min-depth, gp.koza.ns.root,gp.koza.ns.terminals,gp.koza.xover.maxdepth,gp.koza.mutate.maxdepth, pop.subpop.0.size,pop.subpop.0.species.pipe.source.0.prob,select.tournament.size, gp.koza.grow.min-depth,gp.koza.grow.max-depth,generations,gp.koza.ns.nonterminals, pop.subpop.0.species.pipe.source.1.prob,pop.subpop.0.species.pipe.source.0, Raw Fitness,Adjusted Fitness,Hits
101010001,C,1,84125490,0.5,10,5,0.5,0.05,19,NaN,761,0.55,2,NaN,NaN,69,0.45,0.45,ec.gp.koza.CrossoverPipeline,0.58092475,0.6325412,6.0 
101010002,C,1,-1574848641,0.5,7,4,0,0.5,5,NaN,30,0.55,6,NaN,NaN,1750,0.5,0.45,ec.gp.koza.CrossoverPipeline,10.765929,0.084991165,3.0 
101010003,C,1,1429046270,0.5,10,8,0.25,0.375,19,NaN,1500,0.55,9,NaN,NaN,35,0.375,0.45,ec.gp.koza.CrossoverPipeline,0.10179605,0.907609,18.0 
101010004,C,1,1048161384,0.5,10,3,0.5,0.15,19,NaN,1500,1,6,NaN,NaN,35,0.35,0,ec.gp.koza.CrossoverPipeline,0.198151,0.83461934,9.0
```

Note the three appended columns for the response value, the fitness of the best individual found. The rest of the file is unchanged.

## Running an Experiment

To run an experiment, your CSV file should be (for example):

`experiments/anexperiment/firstrun.csv`

The parameter files and ant trail file should be at:

`experiments/ParameterFiles/problem_1.params` etc.

Then you can launch the compiled BatchEvolve.java from the `experiments/anexperiment` directory:

```
java -cp ... test.BatchEvolve -start 1 -end 100 -batchfile test.csv -resultfile out.csv
```

An output file will be generated with the response measure (the stats of the best individual: raw fitness, adjusted fitness and number of hits) appended to each line from the input file. This can then be processed in Matlab.

## Code

`src/BatchEvolve.java` - subclass of ec.Evolve to run a CSV file of experiments.

Please note that this is not polished software! Use at your own risk. We expect this will only be useful for repeating the experimentation found in the paper, as it is mostly hard-coded validation of input. If you have any suggestions for improvements or bugfixes, please contact me.

Note also that this class relies on the [ostermillerutils](http://ostermiller.org/utils/) jar for CSV support. We used version 1.07.

## Parameter Files

* Problem 1 - Symbolic regression of x^4 + x^3 + x^2 + x with no ERCs.
* Problem 4 - Symbolic regression of x^5 - 2x^3 + x with ERCs.
* Problem 7 - Two-Box Problem
* Problem 9 - Santa Fe Ant Trail
* Problem 16 - Boolean 11 Multiplexer</td>
* Problem 17 - Lawnmower

## Results Files

The following are output CSV files from BatchEvolve that we used in published analysis. The filenames are a bit of a legacy issue! The ExpE refers to the set of experiments (the final, published set). The following character "c" "m" or "d" indicates the use of crossover-only algorithm A_c, mutation-only algorithm A_m or the ECJ defaults A_d. Succeeding that is the problem number (refer to the list above to match a problem number with its description). The index prefixed with an "i" is the "iteration number". The first iteration i01 was the full factorial: the second iteration i02 was a test of the optimised parameters that composed A_c* and A_m*.

To run the experiments yourself, you'll need to remove the last three fields from these files, which give the response values (the fitness of the best individual in the last generation).


* ExpE_d_p01_i01_responses.csv.tar.gz - Problem 1, ECJ Defaults (A_d)
* ExpE_c_p01_i02_responses.csv.tar.gz - Problem 1, Optimised Crossover (A_c*), 500 Repetitions
* ExpE_m_p01_i02_responses.csv.tar.gz - Problem 1, Optimised Mutation (A_m*), 500 Repetitions


* ExpE_c_p04_i01_responses.csv.tar.gz - Problem 4, Crossover and Reproduction (A_c), Full Factorial
* ExpE_m_p04_i01_responses.csv.tar.gz - Problem 4, Mutation and Reproduction (A_m), Full Factorial
* ExpE_d_p04_i01_responses.csv.tar.gz - Problem 4, ECJ Defaults (A_d)
* ExpE_c_p04_i02_responses.csv.tar.gz - Problem 4, Optimised Crossover (A_c*), 500 Repetitions
* ExpE_m_p04_i02_responses.csv.tar.gz - Problem 4, Optimised Mutation (A_m*), 500 Repetitions

* ExpE_c_p07_i01_responses.csv.tar.gz - Problem 7, Crossover and Reproduction (A_c), Full Factorial
* ExpE_m_p07_i01_responses.csv.tar.gz - Problem 7, Mutation and Reproduction (A_m), Full Factorial
* ExpE_d_p07_i01_responses.csv.tar.gz - Problem 7, ECJ Defaults (A_d)
* ExpE_c_p07_i02_responses.csv.tar.gz - Problem 7, Optimised Crossover (A_c*), 500 Repetitions
* ExpE_m_p07_i02_responses.csv.tar.gz - Problem 7, Optimised Mutation (A_m*), 500 Repetitions


* ExpE_c_p09_i01_responses.csv.tar.gz - Problem 9, Crossover and Reproduction (A_c), Full Factorial
* ExpE_m_p09_i01_responses.csv.tar.gz - Problem 9, Mutation and Reproduction (A_m), Full Factorial
* ExpE_d_p09_i01_responses.csv.tar.gz - Problem 9, ECJ Defaults (A_d)
* ExpE_c_p09_i02_responses.csv.tar.gz - Problem 9, Optimised Crossover (A_c*), 500 Repetitions
* ExpE_m_p09_i02_responses.csv.tar.gz - Problem 9, Optimised Mutation (A_m*), 500 Repetitions

* ExpE_c_p16_i01_responses.csv.tar.gz - Problem 16, Crossover and Reproduction (A_c), Full Factorial
* ExpE_m_p16_i01_responses.csv.tar.gz - Problem 16, Mutation and Reproduction (A_m), Full Factorial
* ExpE_d_p16_i01_responses.csv.tar.gz - Problem 16, ECJ Defaults (A_d)
* ExpE_c_p16_i02_responses.csv.tar.gz - Problem 16, Optimised Crossover (A_c*), 500 Repetitions
* ExpE_m_p16_i02_responses.csv.tar.gz - Problem 16, Optimised Mutation (A_m*), 500 Repetitions

* ExpE_c_p17_i01_responses.csv.tar.gz - Problem 17, Crossover and Reproduction (A_c), Full Factorial
* ExpE_m_p17_i01_responses.csv.tar.gz - Problem 17, Mutation and Reproduction (A_m), Full Factorial
* ExpE_d_p17_i01_responses.csv.tar.gz - Problem 17, ECJ Defaults (A_d)
* ExpE_c_p17_i02_responses.csv.tar.gz - Problem 17, Optimised Crossover (A_c*), 500 Repetitions
* ExpE_m_p17_i02_responses.csv.tar.gz - Problem 17, Optimised Mutation (A_m*), 500 Repetitions
