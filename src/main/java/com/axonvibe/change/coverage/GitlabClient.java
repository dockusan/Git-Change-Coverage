package com.axonvibe.change.coverage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import javax.net.ssl.HttpsURLConnection;

import hudson.model.Result;

public class GitlabClient {

	static void pushResultToGitlab(String gitlabToken, String gitlabUrl, String projectId, String commitHash, Result result, String name, float coveragePercent, String targetUrl, String desciption) {
		try {
		
			String url = gitlabUrl + "/projects/" + encodeUrlParam(projectId) + "/statuses/" + encodeUrlParam(commitHash); 
			URL obj = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
	
			//add reuqest header
			con.setRequestMethod("POST");
			//con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			con.setRequestProperty("private-token", gitlabToken);
			
			Map<String,String> arguments = new HashMap<>();
			arguments.put("state", result.toExportedObject().toLowerCase());
			arguments.put("name", name);
			arguments.put("coverage", String.format(Locale.US, "%.2f", coveragePercent));
			arguments.put("target_url", targetUrl);
			arguments.put("desciption", desciption);
			StringJoiner sj = new StringJoiner("&");
			for(Map.Entry<String,String> entry : arguments.entrySet())
			    sj.add(encodeUrlParam(entry.getKey()) + "=" + encodeUrlParam(entry.getValue()));
			byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
			int length = out.length;
			
			// Send post request
			con.setDoOutput(true);
			con.setFixedLengthStreamingMode(length);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.write(out);
			wr.flush();
			wr.close();
	
			int responseCode = con.getResponseCode();
			Log.log("\nSending 'POST' request to URL : " + url);
			Log.log("Post parameters : " + sj.toString());
			Log.log("Response Code : " + responseCode);
	
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			Log.log(response.toString());
		} catch (Exception e) {
			Log.log(e.getMessage());
		}
	}
	
	private static String encodeUrlParam(String param) {
		try {
			return URLEncoder.encode(param, "UTF-8");
		} catch (Exception e) {
			Log.log(e.getMessage());
		}
		return param;
	}
}
