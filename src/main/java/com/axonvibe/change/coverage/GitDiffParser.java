package com.axonvibe.change.coverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.TrueFileFilter;

public class GitDiffParser {
	
	private static String startAddPattern = "+++ b";
	private static Pattern addLinePattern = Pattern.compile("\\+(.*?)\\@@");
	
	static List<ChangeEntity> getBranchChanged(String sourceBranch, String targetBranch, String workspace) {
		String gitChanged = getGitChange(sourceBranch, targetBranch, workspace);
//		Log.log("------Git Changed-----------");
//		Log.log(gitChanged);
//		Log.log("----------------------------");
		return parseDiff(gitChanged);
	}
	
	static List<ChangeEntity> parseDiff(final String gitDiff) {
		List<ChangeEntity> results = new ArrayList<ChangeEntity>();
		String diff = gitDiff;
		do {
			int startIndex = diff.indexOf(startAddPattern);
			if (startIndex >= 0) {
				int endIndex = diff.indexOf("diff --git", startIndex);
				String fileDiff = "";
				if (endIndex > startIndex) {
					fileDiff = diff.substring(startIndex, endIndex);
					diff = diff.substring(endIndex);
				} else {
					fileDiff = diff.substring(startIndex);
					diff = null;
				}
//				Log.log("--------------------");
//				Log.log(fileDiff);
				ChangeEntity entity = mapToChangeEntity(fileDiff);
				if (entity != null) {
					results.add(entity);
//					Log.log(entity.toString());
				}
			} else {
				diff = null;
			}
		} while (diff != null);
		return results;
	}
	
	static String getLastHash(final String branch, final String workspace) {
		try {
			return executeShellCommand("git", "--git-dir", workspace + File.separator + ".git", "rev-parse", "origin/"+branch);
		} catch (Exception e) {
			Log.log(e.getMessage());
		}
		return "";
	}
	
	private static ChangeEntity mapToChangeEntity(String diff) {
		ChangeEntity entity = null;
		int startIndex = diff.indexOf(startAddPattern);
		if (startIndex >= 0 && diff.indexOf("\n", startIndex) > startIndex + startAddPattern.length()) {
			String file = diff.substring(startIndex + startAddPattern.length(), diff.indexOf("\n", startIndex));
			Matcher matcher = addLinePattern.matcher(diff);			
			while (matcher.find()) {
				if (entity == null) {
					entity = new ChangeEntity(file);
				}
				for (int i = 0; i < matcher.groupCount(); i++) {
					String addLine = matcher.group(i).replaceAll("\\+", "").replaceAll(" @@", "");
					//Log.log(addLine);
					if (addLine.contains(",")) {
						try {
							String[] lineNumber = addLine.split(",");
							entity.addAddedLine(Integer.parseInt(lineNumber[0]), Integer.parseInt(lineNumber[1]));
						} catch (Exception e) {}
					} else {
						try {
							entity.addAddedLine(Integer.parseInt(addLine), 1);
						} catch (Exception e) {}
					}
				}
			}
		}
		return entity;
	}
	
	private static String getGitChange(String sourceBranch, String targetBranch, String workspace) {
		try {
			return executeShellCommand("git", "--git-dir", workspace + File.separator + ".git", "diff", "-U0", "origin/"+targetBranch, "origin/" + sourceBranch);
		} catch (Exception e) {
			Log.log(e.getMessage());
		}
		return "";
	}
	
	private static String executeShellCommand(String... cmds) {
		try {
			Runtime rt = Runtime.getRuntime();
			//String[] commands = {"system.exe", "-get t"};
			Process proc = rt.exec(cmds);
	
			BufferedReader stdInput = new BufferedReader(new 
			     InputStreamReader(proc.getInputStream()));
	
			BufferedReader stdError = new BufferedReader(new 
			     InputStreamReader(proc.getErrorStream()));
	
			// Read the output from the command
			StringBuilder builder = new StringBuilder();
			String s = null;
			while ((s = stdInput.readLine()) != null) {
			    builder.append(s + "\n");
			}
			// Read any errors from the attempted command
			while ((s = stdError.readLine()) != null) {
			    Log.log(s);
			}
			return builder.toString();
		} catch (Exception e) {
			Log.log("Cannot execute the shell command " + e.getMessage());
			for (int i = 0; i < cmds.length; i++) {
				Log.log(cmds[i]);
			}
		}
		return "";
	}
}
