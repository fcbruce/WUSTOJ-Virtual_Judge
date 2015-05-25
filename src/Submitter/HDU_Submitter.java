package Submitter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

public class HDU_Submitter extends Submitter{
	
	static private boolean using[];
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
	    	   	if (info.length >= 3 && info[0].equalsIgnoreCase("HDU"))
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
       	
       clientList = new DefaultHttpClient[usernameList.length];
		HttpHost proxy = new HttpHost("127.0.0.1", 80);
		for (int i = 0; i < clientList.length; i++){
			clientList[i] = new DefaultHttpClient();
			//clientList[i].getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.83 Safari/537.1");
			clientList[i].getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
			clientList[i].getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
			//clientList[i].getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
       
	}
	
	private void getMaxRunId() throws ParseException, IOException{
		Pattern p = Pattern.compile("<td height=22px>(\\d+)");
		
		try{
			get = new HttpGet("http://acm.hdu.edu.cn/status.php");
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
		}finally{
			EntityUtils.consume(entity);
		}
		
		Matcher m = p.matcher(html);
		if(m.find()){
			maxRunId = Integer.parseInt(m.group(1));
			//System.out.println("Date: "+time+" Get maxRunId : "+maxRunId);
		}else{
			throw new RuntimeException();
		}
	}
	
	private void login(String username, String password) throws ClientProtocolException, IOException{
		try{
			post = new HttpPost("http://acm.hdu.edu.cn/userloginex.php?action=login&cid=0&notice=0");
			List<NameValuePair> nvps = new ArrayList<NameValuePair> ();
			//System.out.println("user is "+username);
			nvps.add(new BasicNameValuePair("username",username));
			nvps.add(new BasicNameValuePair("userpass",password));
			
			post.setEntity(new UrlEncodedFormEntity(nvps,Charset.forName("gb2312")));
			
			response = client.execute(post);
			entity = response.getEntity();
			
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY){
				throw new RuntimeException();
			}
		}finally{
			EntityUtils.consume(entity);
		}
	}
	
	private boolean isLoggedIn() throws IOException{
		try{
			get = new HttpGet("http://acm.hdu.edu.cn/");
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
		}finally{
			EntityUtils.consume(entity);
		}
		
		if (html.contains("href=\"/userloginex.php?action=logout\"")) {
            return true;
		} else {
            return false;
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
		try {
			get = new HttpGet("http://acm.hdu.edu.cn/viewerror.php?rid=" + runId);
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity, Charset.forName("gb2312"));
		} finally {
			EntityUtils.consume(entity);
		}
		String info = regFind(html, "(<pre>[\\s\\S]*?</pre>)");
		info = info.replaceAll("<pre>","");
		info = info.replaceAll("</pre>","");
		submission.setAdditionalInfo(info);
	}
	
	public void getResult(String username) throws Exception{
		Pattern p = Pattern.compile(">(\\d{7,})</td><td>[\\s\\S]*?</td><td>([\\s\\S]*?)</td><td>[\\s\\S]*?</td><td>(\\d*?)MS</td><td>(\\d*?)K</td>");
		
		long cur = new Date().getTime(), interval = 1000;
		while(new Date().getTime() - cur < 600000){
			try{
				get = new HttpGet("http://acm.hdu.edu.cn/status.php?user="+username);
				response = client.execute(get);
				entity = response.getEntity();
				html = EntityUtils.toString(entity);
			}finally{
				EntityUtils.consume(entity);
			}
			
			Matcher m = p.matcher(html);
			if(m.find() && Integer.parseInt(m.group(1)) > maxRunId){
				String result = m.group(2).replaceAll("<[\\s\\S]*?>", "").trim();
				submission.setStatus(result);
				submission.setRealRunId(m.group(1));
				if(!result.contains("ing"))
				{
					if(result.contains("Compilation"))
					{
						getAdditionalInfo(submission.getRealRunId());
					}
					else
					{
						submission.setMemory(Math.abs(Integer.parseInt(m.group(4).replaceAll("K", ""))));
						submission.setTime(Integer.parseInt(m.group(3).replaceAll("MS", "")));
					}
					
					System.out.println(submission.getId()+" got it");
					System.out.println(result);
					
					mysql.addOrModify(submission);
					return;
				}
			}else mysql.addOrModify(submission);
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
			post = new HttpPost("http://acm.hdu.edu.cn/submit.php?action=submit");
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("check", "0"));
			nvps.add(new BasicNameValuePair("language", submission.getLanguage()));
			//nvps.add(new BasicNameValuePair("language", changeLanguage(submission.getLanguage())));
			nvps.add(new BasicNameValuePair("problemid",submission.getPid()));
			nvps.add(new BasicNameValuePair("usercode", submission.getSource()));
			
			post.setEntity(new UrlEncodedFormEntity(nvps,Charset.forName("gb2312")));
			
			response = client.execute(post);
			entity = response.getEntity();
			submission.setStatus("Submitted");
			mysql.addOrModify(submission);
			System.out.println("language is "+submission.getLanguage()
			+" and pid is "+submission.getPid());
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY){
				throw new RuntimeException();
			}
		}finally{
			EntityUtils.consume(entity);
		}
	}
	
	
	public void work() throws SQLException {
		// TODO Auto-generated method stub
		idx = getIdleClient();
		int errorCode = 1;
		System.out.println("HDU "+idx+" suozhu");
		try {
			submission.setStatus("Submitting");
			mysql.addOrModify(submission);
			getMaxRunId();
			if (!isLoggedIn()) {
				login(usernameList[idx], passwordList[idx]);
			}
			submit();
			errorCode = 2;
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
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		synchronized (using) {
			using[idx] = false;
			System.out.println(submission.getId()+" tijiaowanbi use "+idx);
		}
	}

}
