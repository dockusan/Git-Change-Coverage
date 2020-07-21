package com.axonvibe.change.coverage;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.tools.ant.DirectoryScanner;

public class MainDemo {
	public static void main(String[] args) {
		
		String coberturaFile = "./fastlane/test_output/coverage/MapUtilsTest/cobertura.xml";
		String jacocoReportFile = "jacocoTestReport.xml";
		
//		
//		//For Android
//		List<ChangeEntity> changeEntitiesForJacoco = GitDiffParser.getBranchChanged("fix/test-ut", "develop", "/Users/ducnguyen/PROJECTS/vibe-android-sojo-travel");
//		Log.debug("changeEntities jacoco"+ changeEntitiesForJacoco);
//		GitJacocoReport.generateGitReport(jacocoReportFile, changeEntitiesForJacoco);
//		Log.debug("Finish jacoco");
		
		//For iOS

//			DirectoryScanner scanner = new DirectoryScanner();
//			scanner.setIncludes(new String[]{"**/*.xml"});
//			scanner.setBasedir("fastlane/test_output/");
//			scanner.setCaseSensitive(false);
//			scanner.scan();
//			String[] xmlFiles = scanner.getIncludedFiles();
//			List<CoverageResult> iosResults = new ArrayList<CoverageResult>();
//
//			for (String xmlFile : xmlFiles) {
//				String fullPath = scanner.getBasedir()+File.separator+xmlFile;
//				List<ChangeEntity> changeEntities = GitDiffParser.getBranchChanged("fix/test-ut", "develop", "/Users/ducnguyen/Documents/Projects/vibe-ios-sojo-travel");
//				iosResults.add(GitCoberturaReport.generateGitReport(fullPath, changeEntities));
//			}
//			CoverageResult result = new CoverageResult();
//			for (CoverageResult coverageResult : iosResults) {
//				result.coveredLines += coverageResult.coveredLines;
//				result.totalAddLines += coverageResult.totalAddLines;
//			}
//			Log.debug("Finish cobertura "+result.coveredLines+"  "+result.totalAddLines);



	}
}
