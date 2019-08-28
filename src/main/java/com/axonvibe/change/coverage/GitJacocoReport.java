package com.axonvibe.change.coverage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GitJacocoReport {

	
	static CoverageResult generateGitReport(String jacocoReportFile, List<ChangeEntity> gitChanges) {
		CoverageResult result = new CoverageResult();
		List<ChangeEntity> changes = new ArrayList<ChangeEntity>(gitChanges);
		
		downloadJacocoReportFormat(jacocoReportFile);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setValidating(true);
	    factory.setIgnoringElementContentWhitespace(true);
	    DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		    File file = new File(jacocoReportFile);
		    Document document = builder.parse(file);
		    NodeList  childs = document.getChildNodes();
		    List<Node> packageNodes = new ArrayList<Node>();
		    for (int i = 0; i < childs.getLength(); i++) {
//		    	Log.log(childs.item(i));
		    	packageNodes.addAll(getNode(childs.item(i), "package"));
			}
		    packageNodes.forEach( packageNode -> {
		    	if (packageNode.hasAttributes()) {
		    		String packageName = packageNode.getAttributes().getNamedItem("name").getNodeValue();
		    		List<ChangeEntity> packageChanges = changes.stream().filter( changeEntity -> {
		    			return changeEntity.getFilePath().endsWith(packageName);
		    		}).collect(Collectors.toList());
		    		if (!packageChanges.isEmpty()) {
		    			getNode(packageNode, "sourcefile").forEach(sourceFileNode -> {
		    				if (sourceFileNode.hasAttributes() && sourceFileNode.hasChildNodes()) {
		    					String sourceFileName = sourceFileNode.getAttributes().getNamedItem("name").getNodeValue();
		    					if (sourceFileName != null) {
		    						ChangeEntity fileChange = packageChanges.stream().filter(changeEntity -> {
		    							return sourceFileName.equals(changeEntity.getFileName());
		    						}).findFirst().orElse(null);
		    						if (fileChange != null) {
		    							Log.log("Search coverage for -----> " + fileChange.toString());
		    							getNode(sourceFileNode, "line")
		    							.forEach(lineNode -> {
		    								LineCoverage lineCoverage = mapLineNodeToLineCoverage(lineNode);
		    								//Log.log("Line coverage " + lineCoverage);
		    								if (lineCoverage != null && fileChange.getAddedLines().stream().anyMatch( lineNumber -> {
		    									return lineNumber == lineCoverage.lineNumber; 
		    								})) {
		    									Log.log(lineCoverage.toString());
		    									addCoverageResult(result, lineCoverage);
		    								}
		    							});
		    						}
		    					}
		    				}
		    			});
		    		}
		    	}
		    });
		} catch (ParserConfigurationException | SAXException | IOException e) {
			Log.log(e.getMessage());
		}

		return result;
	}
	
	private static List<Node> getNode(Node node, String... trees) {
		List<Node> results = new ArrayList<Node>();
		if (trees.length == 1) {
			if (node.hasChildNodes()) {
				NodeList nodeList = node.getChildNodes();
				for (int index = 0; index < nodeList.getLength(); index++) {
					Node childNode = nodeList.item(index);
					if (trees[0].equals(childNode.getNodeName())) {
						results.add(childNode);
					}
				}
			}
		} else if (trees.length > 1 && node.hasChildNodes()) {
			NodeList nodeList = node.getChildNodes();
			String[] subTree = Arrays.copyOfRange(trees, 1, trees.length);
			for (int index = 0; index < nodeList.getLength(); index++) {
				Node childNode = nodeList.item(index);
				if (trees[0].equals(childNode.getNodeName())) {
					results.addAll(getNode(nodeList.item(index), subTree));
				}
			}
		}
		return results;
	}
	
	private static LineCoverage mapLineNodeToLineCoverage(Node lineNode) {
		if (lineNode.hasAttributes()) {
			try {
				LineCoverage result = new LineCoverage();
				NamedNodeMap attributes = lineNode.getAttributes();
				result.lineNumber = Integer.parseInt(attributes.getNamedItem("nr").getNodeValue());
				result.missInstructions = Integer.parseInt(attributes.getNamedItem("mi").getNodeValue());
				result.coveredInstructions = Integer.parseInt(attributes.getNamedItem("ci").getNodeValue());
				result.missBranchs = Integer.parseInt(attributes.getNamedItem("mb").getNodeValue());
				result.coveredBranchs = Integer.parseInt(attributes.getNamedItem("cb").getNodeValue());
				return result;
			} catch (Exception e) {
				Log.log("mapLineNodeToLineCoverage " + e.getMessage());
			}
		}
		return null;
	}

	private static CoverageResult addCoverageResult(CoverageResult initialResult, LineCoverage lineCoverage) {
		initialResult.coveredInstructions += lineCoverage.coveredInstructions;
		initialResult.coveredBranchs += lineCoverage.coveredBranchs;
		initialResult.coveredLines += lineCoverage.coveredInstructions > lineCoverage.missInstructions ? 1 : 0;
		initialResult.totalAddLines += 1;
		initialResult.totalBranchs += lineCoverage.coveredBranchs + lineCoverage.missBranchs;
		initialResult.totalInstructions += lineCoverage.coveredInstructions + lineCoverage.missInstructions;
		return initialResult;
	}
	
	private static void downloadJacocoReportFormat(String reportFilePath) {
		try {
			int lastIndex = reportFilePath.lastIndexOf(File.separator);
			String destination = "";
			if (lastIndex >= 0) {
				destination = reportFilePath.substring(0, lastIndex);
			}
			URL website = new URL("https://www.jacoco.org/jacoco/trunk/coverage/report.dtd");
			try (InputStream in = website.openStream()) {
				Files.copy(in, Paths.get(destination + File.separator + "report.dtd"), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			Log.log(e.getMessage());
		}
	}
}

class LineCoverage {
	int lineNumber = 0;
	int missInstructions = 0;
	int coveredInstructions = 0;
	int missBranchs = 0;
	int coveredBranchs = 0;
	@Override
	public String toString() {
		return "Line " + lineNumber + ": mi=" + missInstructions + ", ci=" + coveredInstructions + ", mb=" + missBranchs + ", cb=" + coveredBranchs;
	}
	
	
}
