package com.axonvibe.change.coverage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.attribute.IfBlankAttribute;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

public class GitChangeCoverageWrapper extends Recorder {

	private boolean applyAndroid;
	private String sourceBranch;
	private String targetBranch;
	private String reportFilePath;

	private float lineCoveredPercent = 0.0f;
	private float instructionCoveredPercent = 0.0f;
	private float branchCoveredPercent = 0.0f;

	private boolean pushResultToGitlab;
	private String gitlabUrl = "";
	private String gitlabProjectId = "";
	private String gitlabToken = "";

	private String jenkinName = "Jenkins";

	@DataBoundConstructor
	public GitChangeCoverageWrapper(boolean applyAndroid, String sourceBranch, String targetBranch,
			String reportFilePath) {
		this.applyAndroid = applyAndroid;
		this.sourceBranch = sourceBranch;
		this.targetBranch = targetBranch;
		this.reportFilePath = reportFilePath;
	}

	static private String readInput(String input, EnvVars vars) {
		Log.log("Read input " + input);
		if (input.startsWith("$")) {
			// Read from env
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

	public boolean isApplyAndroid() {
		return applyAndroid;
	}

	@DataBoundSetter
	public void setApplyAndroid(boolean applyAndroid) {
		this.applyAndroid = applyAndroid;
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

	public boolean isPushResultToGitlab() {
		return pushResultToGitlab;
	}

	@DataBoundSetter
	public void setPushResultToGitlab(boolean pushResultToGitlab) {
		this.pushResultToGitlab = pushResultToGitlab;
	}

	public String getGitlabUrl() {
		return gitlabUrl;
	}

	@DataBoundSetter
	public void setGitlabUrl(String gitlabUrl) {
		this.gitlabUrl = gitlabUrl;
	}

	public String getGitlabProjectId() {
		return gitlabProjectId;
	}

	@DataBoundSetter
	public void setGitlabProjectId(String gitlabProjectId) {
		this.gitlabProjectId = gitlabProjectId;
	}

	public String getGitlabToken() {
		return gitlabToken;
	}

	@DataBoundSetter
	public void setGitlabToken(String gitlabToken) {
		this.gitlabToken = gitlabToken;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		// TODO Auto-generated method stub
		return BuildStepMonitor.NONE;
	}

	public String getJenkinName() {
		return jenkinName;
	}

	@DataBoundSetter
	public void setJenkinName(String jenkinName) {
		this.jenkinName = jenkinName;
	}

	private boolean isCodeCoveragePass(CoverageResult result) {
		return result.getLineCoveredPercent() >= lineCoveredPercent
				&& result.getInstructionConveredPercent() >= instructionCoveredPercent
				&& result.getBranchCoveredPercent() >= branchCoveredPercent;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		// TODO Auto-generated method stub
		Log.logger = listener.getLogger();
		final EnvVars envVars = build.getEnvironment(listener);
//		envVars.entrySet().forEach(entry -> {
//			Log.log("Environment variable : " + entry.getKey() + " = " + entry.getValue());
//		});

		final String srcBranch = readInput(sourceBranch, envVars);
		final String tarBranch = readInput(targetBranch, envVars);

		final String workSpace = envVars.get("WORKSPACE");
		List<ChangeEntity> changeEntities = GitDiffParser.getBranchChanged(srcBranch, tarBranch, workSpace);
		CoverageResult result = new CoverageResult();
		if (applyAndroid) {
			result = GitJacocoReport.generateGitReport(workSpace + File.separator + reportFilePath, changeEntities);
		} else {
			List<CoverageResult> iosResults = new ArrayList<CoverageResult>();
			getListXmlReport(workSpace + File.separator + reportFilePath).forEach(report -> {
				iosResults.add(GitCoberturaReport.generateGitReport(report, changeEntities));
			});
			for (CoverageResult coverageResult : iosResults) {
				result.totalAddLines += coverageResult.totalAddLines;
				result.coveredLines += coverageResult.coveredLines;
			}

		}
		Log.log("---------------------------Jacoco Git change result---------------------------");
		Log.log(result.toString());

		if (!isCodeCoveragePass(result)) {
			Log.log(String.format(Locale.US,
					"Build faile by code coverage below thresholds(Line: %.2f%%, Instruction: %.2f%%, Branch: %.2f%%)",
					lineCoveredPercent, instructionCoveredPercent, branchCoveredPercent));
			Log.log("---------------------------Jacoco Git change result---------------------------");
			// throw new AbortException("Code coverage doesn't pass");
		} else {
			Log.log("---------------------------Jacoco Git change result---------------------------");
		}

		if (pushResultToGitlab) {
			boolean isPassCoverage = isCodeCoveragePass(result);
			Result buildResult = build.getResult();
			if (buildResult == Result.SUCCESS && !isPassCoverage) {
				buildResult = Result.FAILURE;
			}
			String commitHash = envVars.get("GIT_COMMIT") != null ? envVars.get("GIT_COMMIT")
					: GitDiffParser.getLastHash(srcBranch, workSpace);
			GitlabClient.pushResultToGitlab(gitlabToken, gitlabUrl, gitlabProjectId, commitHash, buildResult,
					jenkinName, applyAndroid ? result.getInstructionConveredPercent() : result.getLineCoveredPercent(), envVars.get("BUILD_URL"),
					!isPassCoverage ? "Fail code coverage for change on this branch" : "");
		}

		return true;
	}

	private List<String> getListXmlReport(String folderPath) {
		List<String> list = new ArrayList<String>();

		if (folderPath.endsWith("xml")) {
			//This is xml file
			list.add(folderPath);
			return list;
		}
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setIncludes(new String[] { "**/*.xml" });
		scanner.setBasedir(folderPath);
		scanner.setCaseSensitive(false);
		scanner.scan();
		String[] xmlFiles = scanner.getIncludedFiles();
		for (String xmlFile : xmlFiles) {
			list.add(scanner.getBasedir() + File.separator + xmlFile);
		}
		return list;
	}

	@Symbol("greet")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public FormValidation doCheckSourceBranch(@QueryParameter String sourceBranch) {
			if (sourceBranch.isEmpty()) {
				return FormValidation.error(
						"Please input branch name or environment variable of branch name like this ${branchName}");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckTargetBranch(@QueryParameter String targetBranch) {
			if (targetBranch.isEmpty()) {
				return FormValidation.error(
						"Please input branch name or environment variable of branch name like this ${branchName}");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckReportFilePath(@QueryParameter String reportFilePath) {
			if (reportFilePath.isEmpty()) {
				return FormValidation.error("Please input valid Jacoco report file path, or folder xml coverage iOS");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckGitlabUrl(@QueryParameter String gitlabUrl,
				@QueryParameter boolean pushResultToGitlab) {
			if (pushResultToGitlab && (gitlabUrl.isEmpty() || !gitlabUrl.startsWith("http"))) {
				return FormValidation.error("Please input the base gitlab url to call api");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckGitlabProjectId(@QueryParameter String gitlabProjectId,
				@QueryParameter boolean pushResultToGitlab) {
			if (pushResultToGitlab && gitlabProjectId.isEmpty()) {
				return FormValidation.error("Please input the gitlab project id");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckGitlabToken(@QueryParameter String gitlabToken,
				@QueryParameter boolean pushResultToGitlab) {
			if (pushResultToGitlab && gitlabToken.isEmpty()) {
				return FormValidation.error("Please filled the Gitlab access token");
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckJenkinName(@QueryParameter String jenkinName,
				@QueryParameter boolean pushResultToGitlab) {
			if (pushResultToGitlab && jenkinName.isEmpty()) {
				return FormValidation.error("Please input Jenkin name");
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
