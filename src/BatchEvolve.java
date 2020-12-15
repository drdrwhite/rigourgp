package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import com.Ostermiller.util.LabeledCSVParser;

import ec.EvolutionState;
import ec.Evolve;
import ec.Individual;
import ec.gp.koza.KozaFitness;
import ec.util.Output;
import ec.util.Parameter;
import ec.util.ParameterDatabase;

/**
 * This class will read in a CSV file containing jobs to be run within ECJ.
 * Each line is a single job, and details the parameter settings for that
 * experiment.  The class will read each job, run it through ECJ, and
 * write out the results.  Commandline arguments control the
 * start and end point in the file.
 * @author drw
 *
 */
public class BatchEvolve extends Evolve {
	
	/** Commandline argument for first line to process in the input file **/
    public static final String A_START = "-start";
    
    /** Commandline argument for last line to process in the input file **/
    public static final String A_END = "-end";
    
    /** Commandline argument for batch file **/
    public static final String A_BATCHFILE = "-batchfile";
    
    /** Commandline argument for output file **/
    public static final String A_OUTPUT_FILE = "-resultfile";
    
    /** Path to the ECJ default parameter files **/
    public static final String PARAMETER_PATH = 
    					"/n/staffstore/drw/GPTest/ParameterFiles/problem_";
    
    /** Header labels in input file **/
    public static final String L_ID = "ID";
    public static final String L_ALGORITHM = "Algorithm";
    public static final String L_PROBLEM = "Problem";
    public static final String L_SEED = "seed.0";
    public static final String L_HALF_GROW_PROB = "gp.koza.half.growp";
    public static final String L_HALF_MAX_DEPTH = "gp.koza.half.max-depth";
    public static final String L_HALF_MIN_DEPTH = "gp.koza.half.min-depth";
    public static final String L_ROOT = "gp.koza.ns.root";
    public static final String L_TERMINALS = "gp.koza.ns.terminals";
    public static final String L_XOVER_MAXDEPTH = "gp.koza.xover.maxdepth";
    public static final String L_MUTATE_MAXDEPTH = "gp.koza.mutate.maxdepth";
    public static final String L_POP_SIZE = "pop.subpop.0.size";
    public static final String L_OP_PROBABILITY = "pop.subpop.0.species.pipe.source.0.prob";
    public static final String L_TS = "select.tournament.size";
    public static final String L_GROW_MIN_DEPTH = "gp.koza.grow.min-depth";
    public static final String L_GROW_MAX_DEPTH = "gp.koza.grow.max-depth";
    public static final String L_GENERATIONS = "generations";
    public static final String L_NONTERMINALS = "gp.koza.ns.nonterminals";
    public static final String L_REPRO_PROBABILITY = "pop.subpop.0.species.pipe.source.1.prob";
    public static final String L_OP = "pop.subpop.0.species.pipe.source.0";
    							       
    /** Constants for algorithm types **/
    public static final String ALG_CROSSOVER = "C";
    public static final String ALG_MUTATION = "M";
    public static final String ALG_DEFAULTS = "D";
    
    /** Part to append to header of output file **/
    public static final String[] RESULTS_HEADER_PART = {"Raw Fitness","Adjusted Fitness", "Hits"};
        
    /** Value for fields that do not apply to a particular row **/
    public static final String NULL_SYMBOL = "NaN";
	
    /**
     * Main procedure.
     * 	Readings in a batch file of experiments to run, given by the
     *  "-batch filename" commandline argument.
     *  Also requires "-start xxxx" and "-end xxxx" to indicate which
     *  IDs (the first column of the file) this process should process, 
     *  inclusively.
     * @param args
     */
	public static void main(String[] args) {
		
		// Input and output filenames
		String batchfile = getBatchFile(args);
		String outputFile = getOutputFile(args);
		
		// Parse arguments for the start and end line numbers
		int[] lines = getStartEnd(args);
		int start = lines[0];
		int end = lines[1];
				
		// Open output file
		CSVPrinter resultPrinter = null;
		try {
			FileOutputStream outStream = new FileOutputStream(outputFile);
			resultPrinter = new CSVPrinter(outStream);
		} catch (Exception e) {
			System.err.println("Error opening output file: " + e);
			System.exit(-1);
		}

		// Open CSV Input File
		LabeledCSVParser batchReader = null;
		try {
			FileInputStream inStream = new FileInputStream(batchfile);
			batchReader = new LabeledCSVParser(new CSVParser(inStream));
		} catch (Exception e) {
			System.err.println("Can't open batch input file: " + e);
			System.exit(-1);
		}
		
		// Read in header and output to result file
		try {
			String[] header = batchReader.getLabels();
			resultPrinter.write(header);
			resultPrinter.writeln(RESULTS_HEADER_PART);
		} catch (Exception e) {
			System.err.println("Error reading and writing header.");
			System.err.println(e);
			System.exit(-1);
		}
		
		// Read CSV file until we reach start
		skipToStart(batchReader,start);
		
		// Flag for when we've finished processing
		boolean finished = false;
		
		// Current Line and corresponding ID column values
		int current = start;
		int currentLineID;
		
		// Whilst not finished, run each experiment
		while (!finished) {
			
			currentLineID = Integer.parseInt(batchReader.getValueByLabel(L_ID));
			System.out.println("Processing Line " + current + " with ID: " + currentLineID);
			
			// Set problem number
			String problemNumber = batchReader.getValueByLabel(L_PROBLEM);
			
			// Create parameter database using ECJ default problem file
			ParameterDatabase parameterDatabase = readParam(problemNumber,args);
						
			parameterDatabase.set(new Parameter("verbosity"), Integer.toString(Output.V_NO_GENERAL));
			
			//	Set the seed in the database
			String seedString = batchReader.getValueByLabel(L_SEED);
			parameterDatabase.set(new Parameter(L_SEED), seedString);
			
			String algorithm = batchReader.getValueByLabel(L_ALGORITHM);		
			
			if (algorithm.equals(ALG_CROSSOVER)) {
				// crossover - set parameters
				setSharedParameters(parameterDatabase,batchReader);
				setCrossoverParameters(parameterDatabase,batchReader);
			} else if (algorithm.equals(ALG_MUTATION)) {
				// mutation - set parameters
				setSharedParameters(parameterDatabase,batchReader);
				setMutationParameters(parameterDatabase,batchReader);
			} else if (algorithm.equals(ALG_DEFAULTS)) {
				checkDefaultParameters(batchReader);
			} else {
				System.err.println("Error: unrecognised algorithm type");
				System.exit(-1);
			}
			
			// Initialise parameter database
			EvolutionState state = initialize(parameterDatabase, 0);
			
			// Run Experiment
		    state.run(EvolutionState.C_STARTED_FRESH);
		    
		    // Read parameters back from the parameter database (sanity check)
		    // Use the read ones to build the output
		    // Write experiment's result to file
		    String[] usedParams = null;
		    if (!(algorithm.equals(ALG_DEFAULTS))) {
		    	    usedParams = readParametersBack(parameterDatabase,
		    						Integer.toString(currentLineID),problemNumber);
		    } else if (algorithm.equals(ALG_DEFAULTS)) {
		    	usedParams=defaultParameters(parameterDatabase,Integer.toString(currentLineID),problemNumber);
		    } else {
		    	System.err.println("Invalid algorithm type " + algorithm);
		    	System.exit(-1);
		    }
		    
		    // Get Response measure
		    String[] result = getResponse(state);
		    
		    writeResult(usedParams,result,resultPrinter);
		    
		    // Clean-up
		    cleanup(state);
		    
		    // If at the end, finish.  If not, keep going or raise error if eof
		    if (current == end) {
		    	finished = true;
		    } else {
		    	try {
		    		String[] newLine = batchReader.getLine();
		    		current++;
			    	if (newLine == null) {
			    		System.err.println("Reached end of file without processing to end line " + end);
			    		System.exit(-1);
			    	}
		    	} catch (Exception e) {
		    		System.err.println("Exception reading input file");
		    		System.err.println(e);
		    		System.exit(-1);
		    	}
		    }

		}
		
		// We're done
	    System.exit(0);

	}
	
	
	/**
	 * Return response - the best individual in the population at the end
	 * of the run.
	 * @param state
	 * @return
	 */
	public static String[] getResponse(EvolutionState state) {
		
		String[] response = new String[3];
		Individual[] population = state.population.subpops[0].individuals;
		Individual best = population[0];	
		
		for(int i=0; i<population.length; i++) {
			if(population[i].fitness.betterThan(best.fitness)) {
				best = population[i];
			}
		}
		
		KozaFitness kf = ((KozaFitness)best.fitness);
		response[0] = Float.toString(kf.rawFitness());
		response[1] = Float.toString(kf.adjustedFitness());
		response[2] = Float.toString(kf.hits);
		
		return response;
	}
	
	/**
	 * Write out latest result to output file.
	 * @param params
	 * @param result
	 * @param outPrinter
	 */
	public static void writeResult(String[] params, String[] result, 
													CSVPrinter outPrinter) {
		try {
			outPrinter.write(params);
			outPrinter.writeln(result);
		} catch (Exception e) {
			System.err.println("Error writing result " + e);
			System.exit(-1);
		}
		
	}
	
	/**
	 * Return an array mostly composed of "NaN" for the case where
	 * the default parameter settings have been requested.
	 * @param p
	 * @param id
	 * @param problem
	 * @return
	 */
	public static String[] defaultParameters(ParameterDatabase p,
											String id, String problem) {
		String seed = p.getString(new Parameter(L_SEED), null);
    	String nan = "NaN";
    	String[] usedParams = {id,"D",problem,seed,nan,nan,nan,nan,nan,
    											nan,nan,nan,nan,nan,
    											nan,nan,nan,nan,nan,nan};
    	return usedParams;
	}
	/**
	 * Read parameters from ECJ for output to the results file.
	 * This is done as a sanity check, the parameters should be
	 * the same as those read from the input file.
	 * @param p
	 * @return
	 */
	public static String[] readParametersBack(ParameterDatabase p, 
											  String id, String problem) {
		
		String operator = p.getString(new Parameter(L_OP),null);
		
		String algorithm = "";
		if (operator.equals("ec.gp.koza.CrossoverPipeline")) {
			algorithm = "C";
		} else if (operator.equals("ec.gp.koza.MutationPipeline")) {
			algorithm = "M";
		} else if (operator.equals(NULL_SYMBOL)) {
			algorithm = "D";
		} else {
			System.err.println("Invalid algorithm type read back: " + operator);
			System.exit(-1); 
		}
		
		String seed = p.getString(new Parameter(L_SEED), null);
		String growProb = p.getString(new Parameter(L_HALF_GROW_PROB),null);
		String halfMax = p.getString(new Parameter(L_HALF_MAX_DEPTH),null);
		String halfMin = p.getString(new Parameter(L_HALF_MIN_DEPTH), null);
		String root = p.getString(new Parameter(L_ROOT), null);
		String terminals = p.getString(new Parameter(L_TERMINALS),null);
		
		String xoverMaxDepth;
		if (algorithm.equals("C")) {
			xoverMaxDepth = p.getString(new Parameter(L_XOVER_MAXDEPTH),null);	
		} else {
			 xoverMaxDepth = "NaN";
		}
		
		String mutMaxDepth;
		if (algorithm.equals("M")) {
			mutMaxDepth = p.getString(new Parameter(L_MUTATE_MAXDEPTH),null);
		} else {
			mutMaxDepth = "NaN";
		}
		
		String popsize = p.getString(new Parameter(L_POP_SIZE),null);
		String opProb = p.getString(new Parameter(L_OP_PROBABILITY),null);
		String ts = p.getString(new Parameter(L_TS),null);
		
		String growMinDepth;
		String growMaxDepth;
		
		if (algorithm.equals("M")) {
			growMinDepth = p.getString(new Parameter(L_GROW_MIN_DEPTH),null);
			growMaxDepth = p.getString(new Parameter(L_GROW_MAX_DEPTH),null);
		} else {
			growMinDepth = "NaN";
			growMaxDepth = "NaN";
		}
		
		String gen = p.getString(new Parameter(L_GENERATIONS),null);
		String nonterm = p.getString(new Parameter(L_NONTERMINALS),null);
		String reproProb = p.getString(new Parameter(L_REPRO_PROBABILITY),null);
				
		String [] params = {id,algorithm,problem,seed, growProb, halfMax, halfMin,
				root,terminals,xoverMaxDepth,mutMaxDepth,popsize,opProb,ts,
				growMinDepth,growMaxDepth,gen,nonterm,reproProb,operator};
		
		return params;
		
	}
	
	/**
	 * Validate parameters are all "NaN" for default algorithm.
	 * @param batchReader
	 */
	public static void checkDefaultParameters(LabeledCSVParser batchReader) {
		
		String halfGrowProb = batchReader.getValueByLabel(L_HALF_GROW_PROB);
		if (!(halfGrowProb.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected grow probability");
			System.exit(-1);
		}
		
		String halfMaxDepth = batchReader.getValueByLabel(L_HALF_MAX_DEPTH);
		if (!(halfMaxDepth.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected half max depth");
			System.exit(-1);
		}
		
		String halfMinDepth = batchReader.getValueByLabel(L_HALF_MIN_DEPTH);
		if (!(halfMinDepth.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected half min depth");
			System.exit(-1);
		}
		
		String rootProb = batchReader.getValueByLabel(L_ROOT);
		if (!(rootProb.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected root probability");
			System.exit(-1);
		}
		
		String termProb = batchReader.getValueByLabel(L_TERMINALS);
		if (!(termProb.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected terminals probability");
			System.exit(-1);
		}
		
		String xoMaxDepth = batchReader.getValueByLabel(L_XOVER_MAXDEPTH);
		if (!(xoMaxDepth.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected xo max depth");
			System.exit(-1);
		}
		
		String mutMaxDepth = batchReader.getValueByLabel(L_MUTATE_MAXDEPTH);
		if (!(mutMaxDepth.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected mutation max depth");
			System.exit(-1);
		}
		
		String popSize = batchReader.getValueByLabel(L_POP_SIZE);
		if (!(popSize.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected pop size");
			System.exit(-1);
		}
		
		String opProb = batchReader.getValueByLabel(L_OP_PROBABILITY);
		if (!(opProb.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected operator probability");
			System.exit(-1);
		}
		
		String ts = batchReader.getValueByLabel(L_TS);
		if (!(ts.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected tournament selection size");
			System.exit(-1);
		}
		
		String growMin = batchReader.getValueByLabel(L_GROW_MIN_DEPTH);
		if (!(growMin.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected min grow size");
			System.exit(-1);
		}
		
		String growMax = batchReader.getValueByLabel(L_GROW_MAX_DEPTH);
		if (!(growMax.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected max grow size");
			System.exit(-1);
		}
		
		String generations = batchReader.getValueByLabel(L_GENERATIONS);
		if (!(generations.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected generations");
			System.exit(-1);
		}
		
		String nonterm = batchReader.getValueByLabel(L_NONTERMINALS);
		if (!(nonterm.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected nonterm probability");
			System.exit(-1);
		}
		
		String reproProb = batchReader.getValueByLabel(L_REPRO_PROBABILITY);
		if (!(reproProb.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected reproduction probability");
			System.exit(-1);
		}
		
		String source = batchReader.getValueByLabel(L_OP);
		if (!(source.equals(NULL_SYMBOL))) {
			System.err.println("Unexpected operator" + source);
			System.exit(-1);
		}
		
	}
	
	/**
	 * Set shared parameters - write those parameters relevant to xo and mut
	 * into the database.  Also validate as we go.
	 * @param parameterDatabase
	 * @param batchReader
	 * @return
	 */
	public static void setSharedParameters(ParameterDatabase parameterDatabase,
										LabeledCSVParser batchReader) {
											
		String halfGrowProbString = batchReader.getValueByLabel(L_HALF_GROW_PROB);		
		float halfGrowProb = Float.parseFloat(halfGrowProbString);
		if ((halfGrowProb < 0) || (halfGrowProb > 1)) {
			System.err.println("Invalid Grow probability");
			System.err.println("Invalid value was " + halfGrowProb);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_HALF_GROW_PROB), halfGrowProbString);
		
		String halfMaxString = batchReader.getValueByLabel(L_HALF_MAX_DEPTH);
		int halfMax = Integer.parseInt(halfMaxString);
		if (halfMax < 1) {
			System.err.println("Invalid half maximum depth");
			System.err.println("Invalid value was " + halfMax);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_HALF_MAX_DEPTH), halfMaxString);
		
		String halfMinString = batchReader.getValueByLabel(L_HALF_MIN_DEPTH);
		int halfMin = Integer.parseInt(halfMinString);
		if ((halfMin < 1) || (halfMin > halfMax)) {
			System.err.println("Invalid half minimum depth");
			System.err.println("Invalid value was " + halfMin);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_HALF_MIN_DEPTH), halfMinString);
		
		String nsRootString = batchReader.getValueByLabel(L_ROOT);
		float nsRoot = Float.parseFloat(nsRootString);
		if ((nsRoot < 0) || (nsRoot > 1)) {
			System.err.println("Invalid root selection prob");
			System.err.println("Invalid value was " + nsRootString);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_ROOT), nsRootString);
		
		String nsTerminalsString = batchReader.getValueByLabel(L_TERMINALS);
		float nsTerminals = Float.parseFloat(nsTerminalsString);
		if ((nsTerminals < 0) || (nsTerminals > 1) || ((nsTerminals + nsRoot) > 1)) {
			System.err.println("Invalid terminals selection prob");
			System.err.println("Invalid value was " + nsTerminalsString);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_TERMINALS), nsTerminalsString);
		
		String popSizeString = batchReader.getValueByLabel(L_POP_SIZE);
		int popSize = Integer.parseInt(popSizeString);
		if (popSize < 1) {
			System.err.println("Invalid population size: " + popSizeString);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_POP_SIZE), popSizeString);
		
		String opProbString = batchReader.getValueByLabel(L_OP_PROBABILITY);
		float opProb = Float.parseFloat(opProbString);
		if ((opProb < 0) || (opProb > 1)) {
			System.err.println("Invalid operator probability: " + opProb);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_OP_PROBABILITY), opProbString);
		
		String tsString = batchReader.getValueByLabel(L_TS);
		int ts = Integer.parseInt(tsString);
		if (ts <= 0) {
			System.err.println("Invalid tournament size: " + ts);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_TS), tsString);
		
		String generationsString = batchReader.getValueByLabel(L_GENERATIONS);
		int generations = Integer.parseInt(generationsString);
		if (generations < 1) {
			System.err.println("Invalid generations: " + generations);
			System.err.println("Pop size was " + popSize);
			System.err.println("gen * pop is " + (generations*popSize));
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_GENERATIONS), generationsString);
		
		String reproProbString = batchReader.getValueByLabel(L_REPRO_PROBABILITY);
		float reproProb = Float.parseFloat(reproProbString);
		if (reproProb < 0 || reproProb > 1 || ((reproProb + opProb) != 1)) {
			System.err.println("Invalid reproduction probability " + reproProb);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_REPRO_PROBABILITY), reproProbString);
		
		String nonterminalsString = batchReader.getValueByLabel(L_NONTERMINALS);
		float nonterminals = Float.parseFloat(nonterminalsString);
		if (nonterminals < 0 || nonterminals > 1 || (nonterminals + nsTerminals + nsRoot) != 1 ) {
			System.err.println("Invalid nonterminals probability " + nonterminals);
			System.err.println("Terminals prob was " + nsTerminals);
			System.err.println("Root was " + nsRoot);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_NONTERMINALS), nonterminalsString);
				
	}
	
	/**
	 * Set parameters specific to crossover-based algorithm, and also
	 * do some sanity checks on the non-employed mutation arguments.
	 * @param parameterDatabase
	 * @param batchReader
	 */
	public static void setCrossoverParameters(ParameterDatabase parameterDatabase,
										LabeledCSVParser batchReader) {
	
		String xoMaxDepthString = batchReader.getValueByLabel(L_XOVER_MAXDEPTH);
		int xoMaxDepth = Integer.parseInt(xoMaxDepthString);
		if (xoMaxDepth < 1) {
			System.err.println("Invalid XO Max Depth: " + xoMaxDepth);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_XOVER_MAXDEPTH), xoMaxDepthString);
		
		String source = batchReader.getValueByLabel(L_OP);
		if (!(source.equals("ec.gp.koza.CrossoverPipeline"))) {
			System.err.println("Invalid genetic operator " + source);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_OP), source);
		
		// Sanity checks
		String mutMaxDepth = batchReader.getValueByLabel(L_MUTATE_MAXDEPTH);
		if (!(mutMaxDepth.equals(NULL_SYMBOL))) {
			System.err.println("Mutation max depth not expected");
			System.exit(-1);
		}
		
		String mutGrowMinDepth = batchReader.getValueByLabel(L_GROW_MIN_DEPTH);
		if (!(mutGrowMinDepth.equals(NULL_SYMBOL))) {
			System.err.println("Mutation grow min depth not expected");
			System.exit(-1);
		}
		
		String mutGrowMaxDepth = batchReader.getValueByLabel(L_GROW_MAX_DEPTH);
		if (!(mutGrowMaxDepth.equals(NULL_SYMBOL))) {
			System.err.println("Mutation grow max depth not expected");
			System.exit(-1);
		}
		
	}
	
	/**
	 * Set mutation-algorithm specific parameters, and do some sanity checks
	 * on the non-employed crossover parameters.
	 * @param parameterDatabase
	 * @param batchReader
	 */
	public static void setMutationParameters(ParameterDatabase parameterDatabase,
			LabeledCSVParser batchReader) {

		String mutMaxDepthString = batchReader.getValueByLabel(L_MUTATE_MAXDEPTH);
		int mutMaxDepth = Integer.parseInt(mutMaxDepthString);
		if (mutMaxDepth < 1) {
			System.err.println("Invalid mutation max depth: " + mutMaxDepth);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_MUTATE_MAXDEPTH), mutMaxDepthString);
		
		String growMinDepthString = batchReader.getValueByLabel(L_GROW_MIN_DEPTH);
		int growMinDepth = Integer.parseInt(growMinDepthString);
		if (growMinDepth < 1) {
			System.err.println("Invalid grow min depth:" + growMinDepth);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_GROW_MIN_DEPTH), growMinDepthString);
				
		String growMaxDepthString = batchReader.getValueByLabel(L_GROW_MAX_DEPTH);
		int growMaxDepth = Integer.parseInt(growMaxDepthString);
		if (growMaxDepth != growMinDepth) {
			System.err.println("Invalid grow max depth: " + growMaxDepth);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_GROW_MAX_DEPTH), growMaxDepthString );
		
		String source = batchReader.getValueByLabel(L_OP);
		if (!(source.equals("ec.gp.koza.MutationPipeline"))) {
			System.err.println("Invalid genetic operator " + source);
			System.exit(-1);
		}
		parameterDatabase.set(new Parameter(L_OP), source);
		
		// Sanity Check
		String xoMaxDepth = batchReader.getValueByLabel(L_XOVER_MAXDEPTH);
		if (!(xoMaxDepth.equals(NULL_SYMBOL))) {
			System.err.println("Crossover max depth not expected");
			System.exit(-1);
		}
	}
	
	/**
	 * Read in a parameter file from PARAMETER_PATH for this experiment.
	 * Create a parameter database.
	 * @param problemNumber Problem number given in experiment.
	 * @param args Commandline arguments
	 * @return
	 */
	private static ParameterDatabase readParam(String problemNumber,
												String[] gs) {
		
		ParameterDatabase p = null;
		String parameterFilename = PARAMETER_PATH + problemNumber + ".params"; 
		
        try  {
        	p = new ParameterDatabase(new File(parameterFilename));
        } catch(Exception e) {
        	System.err.println("Exception reading the parameter file " 
        			+ parameterFilename);
        	System.err.println(e);
        	System.exit(-1);
        }
        
        return p;
	}
	
	/**
	 * Skip through inReader until we reach the row with ID=start
	 * 
	 * @param inReader CSVParser for the input file
	 * @param start ID value to begin processing at
	 */
	private static void skipToStart(LabeledCSVParser inReader, int start) {
	
		try {
			for (int i=1;i<=start;i++) {
				String[] line = inReader.getLine();
				if (line == null) {
					System.err.println("Unable to locate start line: " + start);
					System.exit(-1);
				}
			}
			
		} catch (Exception e) {
			System.err.println("Error skipping file");
			System.err.println(e);
			System.exit(-1);
		}
		
	}
	
	/**
	 * Parse the commandline arguments and return the parameter filename 
	 * @param args
	 * @return
	 */
	private static String getBatchFile(String args[]) {
		
		boolean readBatch = false;
		
		String batchFilename = "";
				
		for(int x=0;x<args.length-1;x++) {
			
			if (args[x].equals(A_BATCHFILE)) {
				if (readBatch) {
					System.err.println("Duplicate batch filename given");
					System.exit(-1);
				}
				readBatch = true;
				if (args.length < (x+2)) {
					System.err.println("Missing batch filename");
					System.exit(-1);
				}
				batchFilename = args[x+1];
			}
		}
		
		if (!readBatch) {
			System.err.println("No batchfilename argument provided");
			System.exit(-1);
		}
		
		return batchFilename;
		
	}
	
	/**
	 * Parse the commandline arguments and return the parameter filename 
	 * @param args
	 * @return
	 */
	private static String getOutputFile(String args[]) {
		
		boolean readFilename = false;
		
		String outputFile = "";
				
		for(int x=0;x<args.length-1;x++) {
			
			if (args[x].equals(A_OUTPUT_FILE)) {
				if (readFilename) {
					System.err.println("Duplicate output filename given");
					System.exit(-1);
				}
				readFilename = true;
				if (args.length < (x+2)) {
					System.err.println("Missing output filename");
					System.exit(-1);
				}
				outputFile = args[x+1];
			}
		}
		
		if (!readFilename) {
			System.err.println("No output argument provided");
			System.exit(-1);
		}
		
		return outputFile;
		
	}
	
	/**
	 * Parse commandline arguments to retrieve start and end line numbers to 
	 * process from the input file.  Window is inclusive.
	 * @param args
	 * @return
	 */
	private static int[] getStartEnd(String args[]) {
		
		boolean readStart = false;
		boolean readEnd = false;
		
		int start = 0;
		int end = 0;
		
		int[] lines = new int[2];
		
		for(int x=0;x<args.length-1;x++) {
			
            if (args[x].equals(A_START)) {
            	if (readStart) {
            		System.err.println("Duplicate start line argument.");
            		System.exit(-1);
            	}
            	if (args.length < (x+2)) {
            		System.err.println("Missing start line number");
            		System.exit(-1);
            	}
            	start = Integer.parseInt(args[x+1]);
            	System.err.println("Starting at line " + start);
            	readStart=true;
            }
            
            if (args[x].equals(A_END)) {
            	if (readEnd) {
            		System.err.println("Duplicate end line argument.");
            		System.exit(-1);
            	}
            	if (args.length < (x+2)) {
            		System.err.println("Missing end line number");
            		System.exit(-1);
            	}
            	end = Integer.parseInt(args[x+1]);
            	System.err.println("Ending at line " + end);
            	readEnd=true;
            }
                  
		}
		
		if (!(readStart && readEnd)) {
			System.err.println("Missing start/end line numbers");
			System.exit(-1);
		}
		
        lines[0] = start;
        lines[1] = end;
        return lines;
        
	}
		
	
}
