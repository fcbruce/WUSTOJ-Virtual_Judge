package Submitter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.TruncatedChunkException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class UVA_Submitter extends Submitter{
	
	static private boolean using[];
	static private int maxrunid[];
	static private String[] usernameList;
	static private String[] passwordList;
	static private DefaultHttpClient clientList[];
	
	protected HttpPost post;
	protected HttpResponse response;
	protected HttpClient client = new DefaultHttpClient();
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
	    	   	if (info.length >= 3 && info[0].equalsIgnoreCase("UVA"))
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
       maxrunid = new int[usernameList.length];
       	
       clientList = new DefaultHttpClient[usernameList.length];
		HttpHost proxy = new HttpHost("127.0.0.1", 80);
		for (int i = 0; i < clientList.length; i++){
			clientList[i] = new DefaultHttpClient();
			clientList[i].getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
			clientList[i].getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
			//clientList[i].getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {
				public boolean retryRequest(IOException exception,
				int executionCount,HttpContext context) {
					System.out.println("                                  YICHANG");
					
					if (executionCount >= 3) {
						// 如果超过最大重试次数，那么就不要继续了
						return false;
					}

					return true;
				}
			};
			clientList[i].setHttpRequestRetryHandler(myRetryHandler);
			
			//clientList[i].getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/4.4.3.4000 Chrome/30.0.1599.101 Safari/537.36");
		}
       
	}
	
	private void login(String username, String password) throws ClientProtocolException, IOException{
		try{
			
			get = new HttpGet("https://uva.onlinejudge.org/");
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
			
			Pattern p = Pattern.compile("name=\"cbsecuritym3\" value=\"(.*?)\"");
			
			Matcher m = p.matcher(html);
			m.find();
			
			post = new HttpPost("https://uva.onlinejudge.org/index.php?option=com_comprofiler&task=login");
			
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");
			
			List<NameValuePair> nvps = new ArrayList<NameValuePair> ();
			
			nvps.add(new BasicNameValuePair("op2","login"));
			nvps.add(new BasicNameValuePair("username",username));
			nvps.add(new BasicNameValuePair("passwd",password));
			nvps.add(new BasicNameValuePair("force_session","1"));
			nvps.add(new BasicNameValuePair("message","0"));
			nvps.add(new BasicNameValuePair("loginfrom","loginmodule"));
			nvps.add(new BasicNameValuePair("cbsecuritym3",m.group(1)));
			nvps.add(new BasicNameValuePair("remember","yes"));
			nvps.add(new BasicNameValuePair("Submit","Login"));
			
			post.setEntity(new UrlEncodedFormEntity(nvps,Charset.forName("UTF-8")));
			
			response = client.execute(post);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
			System.out.println("login done statuscode="+response.getStatusLine().getStatusCode());
			
			if(response.getStatusLine().getStatusCode() != 301){
				throw new RuntimeException();
			}
		}finally{
			EntityUtils.consume(entity);
		}
	}
	
	public static String regFind(String text, String reg, int i){
		Matcher m = Pattern.compile(reg, Pattern.CASE_INSENSITIVE).matcher(text);
		return m.find() ? m.group(i) : "";
	}
	public static String regFind(String text, String reg){
		return regFind(text, reg, 1);
	}
	
	private void getAdditionalInfo(String runId) throws HttpException, IOException {
		try{
			get = new HttpGet("https://uva.onlinejudge.org/index.php?option=com_onlinejudge&Itemid=9&page=show_compilationerror&submission="+runId);
			
			response = client.execute(get);
			entity = response.getEntity();

			html = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			
		}finally{
			EntityUtils.consume(entity);
		}
		String info = regFind(html, "(<pre>[\\s\\S]*?</pre>)");
		info = info.replaceAll("<pre>","");
		info = info.replaceAll("</pre>","");
		submission.setAdditionalInfo(info);
	}
	
	public void getResult(int idx) throws Exception{
		Pattern p = Pattern.compile("<td>(\\d*)</td>[\\S\\s]*?<td[\\S\\s]*?</td>[\\S\\s]*?<td[\\S\\s]*?</td>[\\S\\s]*?<td>([\\S\\s]*?)</td>[\\S\\s]*?<td[\\S\\s]*?</td>[\\S\\s]*?<td>(\\d*)\\.(\\d*)</td>");
		
		long cur = new Date().getTime(), interval = 1000;
		
		int errorcnt=0;
		
		while(new Date().getTime() - cur < 600000){
			try{
				get = new HttpGet("https://uva.onlinejudge.org/index.php?option=com_onlinejudge&Itemid=9");
				response = client.execute(get);
				entity = response.getEntity();
				html = EntityUtils.toString(entity);
			}catch(TruncatedChunkException e){
				sleep(200);
				get = new HttpGet("https://uva.onlinejudge.org/index.php?option=com_onlinejudge&Itemid=9");
				response = client.execute(get);
				entity = response.getEntity();
				html = EntityUtils.toString(entity);
			}finally{
				EntityUtils.consume(entity);
			}
			
			Matcher m = p.matcher(html);
			if(m.find() && Integer.parseInt(m.group(1)) > maxrunid[idx]){
				String result = m.group(2).replaceAll("<[\\s\\S]*?>", "").trim();
				if(result.length()>0) submission.setStatus(result);
				
				if(result.contains("Submission error")) throw new Exception();	
				
				submission.setRealRunId(m.group(1));
				System.out.println("id="+submission.getRealRunId());
				if(result.length()>0 && !result.contains("ing") && !result.contains("queue") && !result.contains("judge") && !result.contains("Received"))
				{
					if(result.contains("Compilation"))
					{
						getAdditionalInfo(submission.getRealRunId());
					}
					else
					{
						submission.setMemory(0);
						submission.setTime(Integer.parseInt(m.group(3))*1000+Integer.parseInt(m.group(4)));
					}
					
					System.out.println(submission.getId()+" got it");
					System.out.println(result);
					
					System.out.println("MaxRunId="+maxrunid[idx]);
					maxrunid[idx]=Integer.parseInt(m.group(1));
					System.out.println("MaxRunId="+maxrunid[idx]);
					
					mysql.addOrModify(submission);
					return;
				}else mysql.addOrModify(submission);
				
				errorcnt=0;
				
			}else errorcnt++;
			
			if(errorcnt>=5) throw new Exception();	
			
			System.out.println("still finding");
			Thread.sleep(interval);
			interval += 500;
		}
		throw new Exception();	
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
	
	private void submit() throws IOException, SQLException{
		try{
			post = new HttpPost("https://uva.onlinejudge.org/index.php?option=com_onlinejudge&Itemid=8&page=save_submission");
			
			MultipartEntity mutiEntity = new MultipartEntity();
			mutiEntity.addPart("problemid",new StringBody("", Charset.forName("utf-8")));
			mutiEntity.addPart("category",new StringBody("", Charset.forName("utf-8")));
			mutiEntity.addPart("localid",new StringBody(submission.getPid(), Charset.forName("utf-8")));
			mutiEntity.addPart("language",new StringBody(submission.getLanguage(), Charset.forName("utf-8")));
			mutiEntity.addPart("code",new StringBody(submission.getSource(), Charset.forName("utf-8")));
			mutiEntity.addPart("codeupl",new StringBody("", Charset.forName("utf-8")));
			 
			post.setEntity(mutiEntity);
			response = client.execute(post);
			HttpEntity httpEntity =  response.getEntity();
			String content = EntityUtils.toString(httpEntity);
			
			System.out.println(response.getStatusLine().getStatusCode());
			
			if(response.getStatusLine().getStatusCode() != 301){
				throw new RuntimeException();
			}
			
			System.out.println("submitted");
			submission.setStatus("Submitted");
			mysql.addOrModify(submission);
		}finally{
			EntityUtils.consume(entity);
		}
	}
	
	private void getMaxRunId(int idx) throws Exception {
		Pattern p = Pattern.compile("<td>(\\d*)</td>");
		System.out.println("getting maxrunid");
		try{
			get = new HttpGet("https://uva.onlinejudge.org/index.php?option=com_onlinejudge&Itemid=19");
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
		}finally{
			EntityUtils.consume(entity);
		}
		
		Matcher m = p.matcher(html);
		if(m.find()){
			maxrunid[idx] = Integer.parseInt(m.group(1));
			//System.out.println("Date: "+time+" Get maxRunId : "+maxRunId);
		}else{
			throw new RuntimeException();
		}
	}
	
	public void work() throws SQLException {
		// TODO Auto-generated method stub
		idx = getIdleClient();
		int errorCode = 1;
		System.out.println("UVA "+usernameList[idx]+" suozhu");
		try {
			submission.setStatus("Submitting");
			mysql.addOrModify(submission);
			if(maxrunid[idx]==0) getMaxRunId(idx);
			try {
				submit();
			} catch (Exception e1) {
				e1.printStackTrace();
				login(usernameList[idx], passwordList[idx]);
				submit();
			}
			
			errorCode = 2;
			getResult(idx);
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
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		synchronized (using) {
			using[idx] = false;
			System.out.println(submission.getId()+" tijiaowanbi use "+usernameList[idx]);
		}
	}

}
