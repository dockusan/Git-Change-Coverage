package com.axonvibe.change.coverage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;

public class GitChangeCoverageBuilder extends Builder implements SimpleBuildStep {
	
	private String sourceBranch;
	private String targetBranch;
	private String reportFilePath;
	
	private float lineCoveredPercent = 0.0f;
	private float instructionCoveredPercent = 0.0f;
	private float branchCoveredPercent = 0.0f;
	
	@DataBoundConstructor
	public GitChangeCoverageBuilder(String sourceBranch, String targetBranch, String reportFilePath) {
		this.sourceBranch = sourceBranch;
		this.targetBranch = targetBranch;
		this.reportFilePath = reportFilePath;
	}
	
	static private String readInput(String input, EnvVars vars) {
		Log.log("Read input " + input);
		if (input.startsWith("$")) {
			//Read from env
			String variableName = input.replaceAll("\\$", "").replaceAll("\\{", "").replaceAll("\\}", "");
			return vars.get(variableName);
		}
		return input;
	}
	
	
	public String getSourceBranch() {
		return sourceBranch;
	}

	public String getTargetBranch() {
		return targetBranch;
	}

	public String getReportFilePath() {
		return reportFilePath;
	}
	
	

	public float getLineCoveredPercent() {
		return lineCoveredPercent;
	}

	@DataBoundSetter
	public void setLineCoveredPercent(float lineCoveredPercent) {
		this.lineCoveredPercent = lineCoveredPercent;
	}

	public float getInstructionCoveredPercent() {
		return instructionCoveredPercent;
	}

	@DataBoundSetter
	public void setInstructionCoveredPercent(float instructionCoveredPercent) {
		this.instructionCoveredPercent = instructionCoveredPercent;
	}

	public float getBranchCoveredPercent() {
		return branchCoveredPercent;
	}

	@DataBoundSetter
	public void setBranchCoveredPercent(float branchCoveredPercent) {
		this.branchCoveredPercent = branchCoveredPercent;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Log.logger = listener.getLogger();
		final EnvVars envVars = run.getEnvironment(listener);
//		envVars.entrySet().forEach(entry -> {
//			Log.log("Environment variable : " + entry.getKey() + " = " + entry.getValue());
//		});
		
		final String srcBranch = readInput(sourceBranch, envVars);
		final String tarBranch = readInput(targetBranch, envVars);
		
		final String workSpace = envVars.get("WORKSPACE");
		List<ChangeEntity> changeEntities = GitDiffParser.getBranchChanged(srcBranch, tarBranch, workSpace);
		CoverageResult result = GitJacocoReport.generateGitReport(workSpace + File.separator + reportFilePath, changeEntities);
		Log.log("---------------------------Jacoco Git change result---------------------------");
		Log.log(result.toString());
		
		if (result.getLineCoveredPercent() < lineCoveredPercent 
				|| result.getInstructionConveredPercent() < instructionCoveredPercent 
				|| result.getBranchCoveredPercent() < branchCoveredPercent) {
			Log.log(String.format(Locale.US, "Build faile by code coverage below thresholds(Line: %.2f%%, Instruction: %.2f%%, Branch: %.2f%%)", lineCoveredPercent, instructionCoveredPercent, branchCoveredPercent));
			Log.log("---------------------------Jacoco Git change result---------------------------");
			throw new AbortException("Code coverage doesn't pass");			
		} else {
			Log.log("---------------------------Jacoco Git change result---------------------------");
		}
	}

	@Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public FormValidation doCheckSourceBranch(@QueryParameter String sourceBranch) {
			if (sourceBranch.isEmpty()) {
				return FormValidation.error("Please input branch name or environment variable of branch name like this ${branchName}");
			}
			
			return FormValidation.ok();
		}
		
		public FormValidation doCheckTargetBranch(@QueryParameter String targetBranch) {
			if (targetBranch.isEmpty()) {
				return FormValidation.error("Please input branch name or environment variable of branch name like this ${branchName}");
			}
			
			return FormValidation.ok();
		}
		
		public FormValidation doCheckReportFilePath(@QueryParameter String reportFilePath) {
			if (reportFilePath.isEmpty() || !reportFilePath.endsWith(".xml")) {
				return FormValidation.error("Please input valid Jacoco report file path, ending with .xml");
			}
			
			return FormValidation.ok();
		}

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GitChangeCoverage_DescriptorImpl_DisplayName();
        }
    }
}
