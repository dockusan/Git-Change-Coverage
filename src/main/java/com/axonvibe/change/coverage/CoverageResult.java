package com.axonvibe.change.coverage;

import java.util.Locale;

public class CoverageResult {

	int totalAddLines = 0;
	int coveredLines = 0;
	
	int totalInstructions = 0;
	int coveredInstructions = 0;
	
	int totalBranchs = 0;
	int coveredBranchs = 0;
	
	float getLineCoveredPercent() {
		return totalAddLines == 0 ? 100f : (float)(coveredLines * 100) / (float)totalAddLines;
	}
	
	float getInstructionConveredPercent() {
		return totalInstructions == 0 ? 100f : (float)(coveredInstructions * 100) / (float)totalInstructions;
	}
	
	float getBranchCoveredPercent() {
		return totalBranchs == 0 ? 100f : (float)(coveredBranchs * 100) / (float)totalBranchs;
	}
	
	@Override
	public String toString() {
		String percentFormat = "JacocoBranchChangeCoverage.%s=%.2f%%\n";
		StringBuilder builder = new StringBuilder();
		builder.append(String.format(Locale.US, percentFormat, "Lines", getLineCoveredPercent()));
		builder.append(String.format(Locale.US, percentFormat, "Instructions", getInstructionConveredPercent()));
		builder.append(String.format(Locale.US, percentFormat, "Branchs", getBranchCoveredPercent()));
		
		builder.append("\n");
		builder.append("Jacoco Branch Change Coverage overall: \n"
				+ "Lines: " + coveredLines + "/" + totalAddLines + "\n"
						+ "Instructions: " + coveredInstructions + "/" + totalInstructions + "\n"
								+ "Branchs: " + coveredBranchs + "/" + totalBranchs);
		return builder.toString();
	}
	
	
}
