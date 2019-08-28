package com.axonvibe.change.coverage;

import java.util.ArrayList;
import java.util.List;

public class ChangeEntity {
	
	private String fileName;
	private String filePath;
	private List<Integer> addedLines = new ArrayList<Integer>();
	
	public ChangeEntity(String file) {
		int lastIndex = file.lastIndexOf("/") ; 
		if (lastIndex >= 0 && lastIndex < file.length() - 1) {
			fileName = file.substring(lastIndex + 1);
			filePath = file.substring(0, lastIndex);
		} else {
			fileName = file;
			filePath = "";
		}
		
	}

	public String getFileName() {
		return fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public List<Integer> getAddedLines() {
		return addedLines;
	}
	
	public void addAddedLine(int fromLine, int count) {
		for (int i = 0; i < count; i++) {
			addedLines.add(new Integer(fromLine+i));
		}
	}

	@Override
	public String toString() {
		String addedLineString = "";
		for (int i = 0; i < addedLines.size(); i++) {
			addedLineString += ("" + addedLines.get(i));
			if (i < addedLines.size() - 1) {
				addedLineString += ", ";
			}
		}
		return filePath + "/" + fileName + "\n"
				+ addedLineString;
	}
	
	
}
