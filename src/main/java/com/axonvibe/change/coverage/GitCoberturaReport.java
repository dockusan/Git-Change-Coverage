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

public class GitCoberturaReport {

	static CoverageResult generateGitReport(String jacocoReportFile, List<ChangeEntity> gitChanges) {
		CoverageResult result = new CoverageResult();
		List<ChangeEntity> changes = new ArrayList<ChangeEntity>(gitChanges);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setValidating(true);
	    factory.setIgnoringElementContentWhitespace(true);
	    DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		    File file = new File(jacocoReportFile);
		    Document document = builder.parse(file);
		    NodeList  childs = document.getChildNodes();
		    List<Node> packageListNode = new ArrayList<Node>();
		    List<Node> packageNodes = new ArrayList<Node>();

		    for (int i = 0; i < childs.getLength(); i++) {
		    	packageListNode.addAll(getNode(childs.item(i), "packages"));
			}
		    
		    for (int i = 0; i < packageListNode.size(); i++) {
		    	packageNodes.addAll(getNode(packageListNode.get(i), "package"));
			}
		    
		    packageNodes.forEach( packageNode -> {
		    	if (packageNode.hasAttributes()) {
		    		String packageName = packageNode.getAttributes().getNamedItem("name").getNodeValue();
		    		
		    		List<ChangeEntity> packageChanges = changes.stream().filter( changeEntity -> {
		    			String changeEntityFilePath = changeEntity.getFilePath().replace("/", ".");
		    			boolean isChanged = changeEntityFilePath.endsWith(packageName);
		    			return isChanged;
		    		}).collect(Collectors.toList());
		    		
		    		if (!packageChanges.isEmpty()) {
		    			List<Node> classesNodes = new ArrayList<Node>();
			 		    for (int i = 0; i < packageNodes.size(); i++) {
			 		    	classesNodes.addAll(getNode(packageNodes.get(i), "classes"));
			 			}
		    			if(!classesNodes.isEmpty()) {
		    				classesNodes.forEach(classNode -> {
		    					getNode(classNode, "class").forEach(classFileNode -> {
		    						if (classFileNode.hasAttributes() && classFileNode.hasChildNodes()) {
				    					String sourceFile = classFileNode.getAttributes().getNamedItem("filename").getNodeValue();
				    					ChangeEntity sourceFileEntity = new ChangeEntity(sourceFile);
				    					String sourceFileName = sourceFileEntity.getFileName();
				    					if (sourceFileName != null) {
				    						ChangeEntity fileChange = packageChanges.stream().filter(changeEntity -> {
				    							return sourceFileName.trim().equals(changeEntity.getFileName().trim());
				    						}).findFirst().orElse(null);
				    						if (fileChange != null) {
				    							Log.log("Search coverage for -----> " + fileChange.toString());
				    							List<Node> lineNodes = getNode(classFileNode, "lines");

				    							lineNodes.forEach(line -> {
				    								getNode(line, "line")
					    							.forEach(lineNode -> {
					    								CoberturaLineCoverage lineCoverage = mapLineNodeToLineCoverage(lineNode);
					    								//Log.log("Line coverage " + lineCoverage);
					    								if (lineCoverage != null && fileChange.getAddedLines().stream().anyMatch( lineNumber -> {
					    									return lineNumber == lineCoverage.lineNumber; 
					    								})) {
					    									Log.log(lineCoverage.toString());
					    									addCoverageResult(result, lineCoverage);
					    								}
					    							});
				    							});
				    						}
				    					}
				    				}
			    				});
		    				});
		    			}
		    		}
		    	}
		    });
		} catch (ParserConfigurationException | SAXException | IOException e) {
			Log.debug(e.getMessage());
		}
		Log.debug("Finish "+jacocoReportFile+" "+result.coveredLines+"  "+result.totalAddLines);

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
	
	private static CoberturaLineCoverage mapLineNodeToLineCoverage(Node lineNode) {
		if (lineNode.hasAttributes()) {
			try {
				CoberturaLineCoverage result = new CoberturaLineCoverage();
				NamedNodeMap attributes = lineNode.getAttributes();
				result.lineNumber = Integer.parseInt(attributes.getNamedItem("number").getNodeValue());
				result.hits = Integer.parseInt(attributes.getNamedItem("hits").getNodeValue());
				result.branch = Boolean.valueOf(attributes.getNamedItem("branch").getNodeValue());
				return result;
			} catch (Exception e) {
				Log.log("mapLineNodeToLineCoverage " + e.getMessage());
			}
		}
		return null;
	}

	private static CoverageResult addCoverageResult(CoverageResult initialResult, CoberturaLineCoverage lineCoverage) {
		initialResult.coveredBranchs += lineCoverage.branch ? 1 : 0;;
		initialResult.coveredLines += lineCoverage.hits > 0 ? 1 : 0;
		initialResult.totalAddLines += 1;
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

class CoberturaLineCoverage {
	int lineNumber = 0;
	int hits = 0;
	boolean branch = false;
	@Override
	public String toString() {
		return "Line " + lineNumber + ": lineNumber=" + lineNumber + ", hits=" + hits + ", branch=" + branch;
	}
	
}

