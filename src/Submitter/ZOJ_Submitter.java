package Submitter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

public class ZOJ_Submitter extends Submitter{
	
	static private boolean using[];
	static private String[] usernameList;
	static private String[] passwordList;
	static private HttpClient[] clientList;
	
	protected HttpPost post;
	protected HttpResponse response;
	protected HttpClient client;
	protected HttpGet get;
	protected String html;
	protected String result;
	protected  int realRunId;
	protected  int realTime;
	protected  int realMemory;
	protected HttpEntity entity;
	
	static{
		List<String> uList = new ArrayList<String>(), pList = new ArrayList<String>();
		
		try {
			FileReader fr = new FileReader("/home/judge/vjudge/accounts.conf");
			BufferedReader br = new BufferedReader(fr);
	    	while (br.ready()) 
	       	{
	    	   	String info[] = br.readLine().split("\\s+");
	    	   	if (info.length >= 3 && info[0].equalsIgnoreCase("ZOJ"))
	    	   	{
	    	   		uList.add(info[1]);
	              pList.add(info[2]);
	            }
	        }
	       br.close();
	       fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		usernameList = uList.toArray(new String[0]);
       passwordList = pList.toArray(new String[0]);
       using = new boolean[usernameList.length];
       	
       clientList = new HttpClient[usernameList.length];
		HttpHost proxy = new HttpHost("127.0.0.1", 80);
		for (int i = 0; i < clientList.length; i++){
			clientList[i] = new HttpClient();
			//clientList[i].getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.83 Safari/537.1");
			clientList[i].getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
			clientList[i].getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
			//clientList[i].getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
       
	}
	
	private void getMaxRunId() throws Exception {
		GetMethod getMethod = new GetMethod("http://acm.zju.edu.cn/onlinejudge/showRuns.do?contestId=1");
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		Pattern p = Pattern.compile("<td class=\"runId\">(\\d+)");

		int count = 0;
		while (true) {
			try {
				client.executeMethod(getMethod);
				break;
			} catch (SocketException e) {
				if (!e.getMessage().contains("reset") || ++count > 5) {
					getMethod.releaseConnection();
					throw e;
				}
				Thread.sleep(4000);
			}
		}

		byte[] responseBody = getMethod.getResponseBody();
		String tLine = new String(responseBody, "UTF-8");
		Matcher m = p.matcher(tLine);
		if (m.find()) {
			maxRunId = Integer.parseInt(m.group(1));
			System.out.println("maxRunId : " + maxRunId);
		} else {
			throw new Exception();
		}
	}
	
	private void login(String username, String password) throws Exception{
		try{
			PostMethod postMethod = new PostMethod("http://acm.zju.edu.cn/onlinejudge/login.do");
			postMethod.addParameter("password", password);
			postMethod.addParameter("rememberMe", "on");
			postMethod.addParameter("handle", username);
			postMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

			System.out.println("login...");
			int statusCode = client.executeMethod(postMethod);
			System.out.println("statusCode = " + statusCode);

			if (statusCode != HttpStatus.SC_MOVED_TEMPORARILY){
				throw new Exception();
			}

			System.out.println("Login Done");
		}finally{
			EntityUtils.consume(entity);
		}
	}
	
	private int getIdleClient() {
		int length = usernameList.length;
		int begIdx = (int) (Math.random() * length);

		while(true) {
			synchronized (using) {
				for (int i = begIdx, j; i < begIdx + length; i++) {
					j = i % length;
					if (!using[j]) {
						using[j] = true;
						client = clientList[j];
						return j;
					}
				}
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void submit() throws Exception{
		System.out.println("getting SubmitID...");
		GetMethod getMethod = new GetMethod("http://acm.zju.edu.cn/onlinejudge/showProblem.do?problemCode="+submission.getPid());
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		Pattern p = Pattern.compile("problemId=(\\d+)\">");
		client.executeMethod(getMethod);
		byte[] responseBody = getMethod.getResponseBody();
		String tLine = new String(responseBody, "UTF-8");
		Matcher m = p.matcher(tLine);
		int SubmitID;
		if (m.find()) {
			SubmitID = Integer.parseInt(m.group(1));
			System.out.println("SubmitID : " + SubmitID);
		} else {
			throw new Exception();
		}
		//↑getsubmitid
		
		PostMethod postMethod = new PostMethod("http://acm.zju.edu.cn/onlinejudge/submit.do");
		postMethod.addParameter("languageId", submission.getLanguage());
		postMethod.addParameter("problemId", String.valueOf(SubmitID));
		postMethod.addParameter("source", submission.getSource());

		postMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		client.getParams().setContentCharset("UTF-8");

		System.out.println("submit...");
		int statusCode = client.executeMethod(postMethod);
		System.out.println("statusCode = " + statusCode);

		if (statusCode == 200){
			responseBody = postMethod.getResponseBody();
			String html = new String(responseBody, "UTF-8");
			
			//System.out.println(html);
			
			if(html.contains("<div id=\"content_title\">Submit Successfully</div>")==false) throw new Exception();
			
			submission.setStatus("Submitted");
			mysql.addOrModify(submission);
			
			System.out.println("Submit Successfully");
		}else 
			throw new Exception();
	}
	

	public void getResult(String username) throws Exception{
		String reg = "<td class=\"runId\">(\\d+)</td>[\\s\\S]*?span class=\"judgeRep[\\s\\S]*?>([\\s\\S]+?)</span>[\\s\\S]*?runTime\">(\\d+)</td>[\\s\\S]*?runMemory\">(\\d+)</td>", result;
		Pattern p = Pattern.compile(reg);

		GetMethod getMethod = new GetMethod("http://acm.zju.edu.cn/onlinejudge/showRuns.do?contestId=1&search=true&firstId=-1&lastId=-1&handle=" + username);
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
		long cur = new Date().getTime(), interval = 2000;
		while (new Date().getTime() - cur < 600000){
			System.out.println("getResult...");
			client.executeMethod(getMethod);
			byte[] responseBody = getMethod.getResponseBody();
			String tLine = new String(responseBody, "UTF-8");

			Matcher m = p.matcher(tLine);
			if (m.find() && Integer.parseInt(m.group(1)) > maxRunId) {
				result = m.group(2).replaceAll("<[\\s\\S]*?>", "").trim();
				submission.setStatus(result);
				submission.setRealRunId(m.group(1));
				
				
				System.out.println("id="+m.group(1));
				System.out.println("result="+m.group(2));
				System.out.println("t="+m.group(3));
				System.out.println("m="+m.group(4));
				
				if (!result.contains("ing")){
					if (result.contains("Compilation")){
						getAdditionalInfo(submission.getRealRunId());
					} else{
						submission.setMemory(Math.abs(Integer.parseInt(m.group(4).replaceAll("K", ""))));
						submission.setTime(Integer.parseInt(m.group(3).replaceAll("MS", "")));
					}
					mysql.addOrModify(submission);
					System.out.println("Got it and result is "+result);
					return;
				}else mysql.addOrModify(submission);
				mysql.addOrModify(submission);
			}
			Thread.sleep(interval);
			interval += 500;
		}
		throw new Exception();
	}
	
	

	public static String regFind(String text, String reg, int i){
		Matcher m = Pattern.compile(reg, Pattern.CASE_INSENSITIVE).matcher(text);
		return m.find() ? m.group(i) : "";
	}
	public static String regFind(String text, String reg){
		return regFind(text, reg, 1);
	}
	public static String getHtml(HttpMethodBase method, String proposedCharset) throws IOException {
		byte[] contentInByte = IOUtils.toByteArray(method.getResponseBodyAsStream());
		Charset charset = null;
		try {
			charset = Charset.forName(proposedCharset);
		} catch (Exception e) {}
		if (charset == null) {
			Header header = method.getResponseHeader("Content-Type");
			if (header != null) {
				Matcher matcher = Pattern.compile("(?i)charset=([-_\\w]+)").matcher(header.getValue());
				if (matcher.find()) {
					try {
						charset = Charset.forName(matcher.group(1));
					} catch (Exception e) {}
				}
			}
		}
		if (charset == null) {
			String tmpHtml = new String(contentInByte, "UTF-8");
			Matcher matcher = Pattern.compile("(?i)charset=([-_\\w]+)").matcher(tmpHtml);
			if (matcher.find()) {
				try {
					charset = Charset.forName(matcher.group(1));
				} catch (Exception e) {}
			}
		}
		if (charset == null) {
			charset = Charset.forName("UTF-8");
		}
		System.out.println(charset.name());
		return new String(contentInByte, charset);
	}
	private void getAdditionalInfo(String runId) throws HttpException, IOException {
		GetMethod getMethod = new GetMethod("http://acm.zju.edu.cn/onlinejudge/showJudgeComment.do?submissionId=" + (Integer.parseInt(runId)+311437));
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

		client.executeMethod(getMethod);
		String html = getHtml(getMethod, null);

		submission.setAdditionalInfo(html);
	}

	@Override
	public void work() throws SQLException {
		idx = getIdleClient();
		int errorCode = 1;

		try {
			submission.setStatus("Submitting");
			mysql.addOrModify(submission);
			getMaxRunId();
			try {
				submit();
			} catch (Exception e1) {
				e1.printStackTrace();
				login(usernameList[idx], passwordList[idx]);
				submit();
			}
			errorCode = 2;
			Thread.sleep(2000);
			getResult(usernameList[idx]);
		} catch (Exception e) {
			e.printStackTrace();
			submission.setStatus("Judge Error " + errorCode);
			mysql.addOrModify(submission);
		}
		
		mysql.UpdateUserAndProblem(submission.getUser(), submission.getSoj(), submission.getPid());
	}

	@Override
	public void waitForUnfreeze() {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
		synchronized (using) {
			using[idx] = false;
		}
	}
	
}
