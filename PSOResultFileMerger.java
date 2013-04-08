/* Copyright 2009-2012 David Hadka
 * 
 * This file is part of the MOEA Framework.
 * 
 * The MOEA Framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or (at your 
 * option) any later version.
 * 
 * The MOEA Framework is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License 
 * along with the MOEA Framework.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.moeaframework.core.CoreUtils;
import org.moeaframework.core.EpsilonBoxDominanceArchive;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PopulationIO;
import org.moeaframework.core.Problem;
import org.moeaframework.core.comparator.EpsilonBoxDominanceComparator;
import org.moeaframework.core.spi.ProblemFactory;
import org.moeaframework.util.CommandLineUtility;
import org.moeaframework.util.TypedProperties;
import org.moeaframework.analysis.sensitivity.*;

import org.moeaframework.core.Solution;

/**
 * Command line utility for merging the approximation sets stored in one or more
 * result files.
 */
public class PSOResultFileMerger extends CommandLineUtility {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private PSOResultFileMerger() {
		super();
	}
	
	@SuppressWarnings("static-access")
	@Override
	public Options getOptions() {
		Options options = super.getOptions();
		
		OptionGroup group = new OptionGroup();
		group.setRequired(true);
		group.addOption(OptionBuilder
				.withLongOpt("problem")
				.hasArg()
				.withArgName("name")
				.withDescription("Problem name")
				.create('b'));
		group.addOption(OptionBuilder
				.withLongOpt("dimension")
				.hasArg()
				.withArgName("number")
				.withDescription("Number of objectives")
				.create('d'));
		options.addOptionGroup(group);
		
		options.addOption(OptionBuilder
				.withLongOpt("epsilon")
				.hasArg()
				.withArgName("e1,e2,...")
				.withDescription("Epsilon values for epsilon-dominance")
				.create('e'));
		options.addOption(OptionBuilder
				.withLongOpt("output")
				.hasArg()
				.withArgName("file")
				.withDescription("Output file containing the merged set")
				.isRequired()
				.create('o'));
		options.addOption(OptionBuilder
				.withLongOpt("vars")
				.hasArg()
				.withArgName("vars")
				.withDescription("Number of decision variables.")
				.isRequired()
				.create('v'));
		options.addOption(OptionBuilder
				.withLongOpt("resultFile")
				.withDescription("Output result file instead of reference set")
				.create('r'));
		
		return options;
	}

	@Override
	public void run(CommandLine commandLine) throws Exception {
		Problem problem = null;
		NondominatedPopulation mergedSet = null;
		PSOResultFileReader reader = null;

		// setup the merged non-dominated population
		if (commandLine.hasOption("epsilon")) {
			double[] epsilon = TypedProperties.withProperty("epsilon",
					commandLine.getOptionValue("epsilon")).getDoubleArray(
					"epsilon", null);
			mergedSet = new EpsilonBoxDominanceArchive(
					new EpsilonBoxDominanceComparator(epsilon));
		} else {
			mergedSet = new NondominatedPopulation();
		}
		System.out.println("setup initial archive");
		try {
			// setup the problem
			if (commandLine.hasOption("problem")) {
				problem = ProblemFactory.getInstance().getProblem(commandLine
						.getOptionValue("problem"));
			} else {
				problem = new PSOProblemStub(Integer.parseInt(commandLine.getOptionValue("vars")), Integer.parseInt(commandLine.getOptionValue("dimension")));
			}
		System.out.println("setup problem");			
			boolean changed = false;
			
			// read in result files
			for (String filename : commandLine.getArgs()) {
				try {
					reader = new PSOResultFileReader(problem, new File(filename));
		System.out.println("initialized reader");	
					while (reader.hasNext()) {
						changed = mergedSet.addAll(reader.next().getPopulation());
						System.out.println("Added to mergedSet. Changed: " + changed);
					}
				} finally {
					if (reader != null) {
						reader.close();
					}
				}
			}
			
			File output = new File(commandLine.getOptionValue("output"));
		System.out.println("outputting");	
			// output merged set
			if (commandLine.hasOption("resultFile")) {
				ResultFileWriter writer = null;
				
				//delete the file to avoid appending
				CoreUtils.delete(output);
				
				try {
					writer = new ResultFileWriter(problem, output);
					
					writer.append(new ResultEntry(mergedSet));
						
				} finally {
					if (writer != null) {
						writer.close();
					}
				}
			} else {
				PopulationIO.writeObjectives(output, mergedSet);
			}

		} finally {
			if (problem != null) {
				problem.close();
			}
		}
	}
	
	/**
	 * Starts the command line utility for merging the approximation sets 
	 * stored in one or more result files.
	 * 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new PSOResultFileMerger().start(args);
	}

}
