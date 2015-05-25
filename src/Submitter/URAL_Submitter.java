package Submitter;

import java.io.BufferedReader;
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

public class URAL_Submitter extends Submitter{
	
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
	    	   	if (info.length >= 3 && info[0].equalsIgnoreCase("URAL"))
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
			
			//clientList[i].getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/4.4.3.4000 Chrome/30.0.1599.101 Safari/537.36");
			clientList[i].getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
			clientList[i].getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);

			//clientList[i].getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
       
	}
	
	private void login(String username, String password) throws ClientProtocolException, IOException{
		try{
			post = new HttpPost("http://acm.timus.ru/authedit.aspx");
			
			post.setHeader("charset", "UTF-8");
			
			List<NameValuePair> nvps = new ArrayList<NameValuePair> ();
			
			nvps.add(new BasicNameValuePair("JudgeID",username));
			nvps.add(new BasicNameValuePair("Password",password));
			nvps.add(new BasicNameValuePair("Action","edit"));
			
			post.setEntity(new UrlEncodedFormEntity(nvps,Charset.forName("UTF-8")));
			
			response = client.execute(post);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
			System.out.println("login done statuscode="+response.getStatusLine().getStatusCode());

			if(response.getStatusLine().getStatusCode() != 302){
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
			get = new HttpGet("http://acm.timus.ru/ce.aspx?id="+runId);
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
			
			if(response.getStatusLine().getStatusCode() != 200){
				throw new RuntimeException();
			}
			
		}finally{
			EntityUtils.consume(entity);
		}

		submission.setAdditionalInfo(html);
	}
	
	public void getResult(int idx) throws Exception{
		Pattern p = Pattern.compile("<TD class=\"id\".*?>(\\d+).*?<TD class=\"verdict_\\w{2,}\">(.+?)</TD><TD.*?>(.+?)</TD>" +
                "<TD.*?>(.+?)</TD>" +
                "<TD.*?>(.+?)</TD>");
		
		long cur = new Date().getTime(), interval = 1000;
		
		int errorcnt=0;
		
		while(new Date().getTime() - cur < 600000){
			try{
				get = new HttpGet("http://acm.timus.ru/status.aspx?Author="+usernameList[idx].replaceAll("\\D", ""));
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
				submission.setRealRunId(m.group(1));
				System.out.println("id="+submission.getRealRunId());
				if(result.length()>0 && !result.contains("ing"))
				{
					if(result.contains("Compilation"))
					{
						getAdditionalInfo(submission.getRealRunId());
					}
					else
					{
						submission.setTime((int) (Float.parseFloat(m.group(4).replaceAll("<[\\s\\S]*?>", "").replaceAll(" ", "").trim())*1000));
						submission.setMemory((int) (Float.parseFloat(m.group(5).replaceAll("<[\\s\\S]*?>", "").replaceAll("[KB ]", "").trim())));
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
	
	private void submit(int idx) throws IOException, SQLException{
		try{
			post = new HttpPost("http://acm.timus.ru/submit.aspx?space=1");
			
			post.setHeader("Origin", "http://acm.timus.ru");    
			
			MultipartEntity mutiEntity = new MultipartEntity();
			mutiEntity.addPart("ProblemNum",new StringBody(submission.getPid(), Charset.forName("utf-8")));
			mutiEntity.addPart("Language",new StringBody(submission.getLanguage(), Charset.forName("utf-8")));
			mutiEntity.addPart("Source",new StringBody(submission.getSource(), Charset.forName("utf-8")));
			mutiEntity.addPart("JudgeID",new StringBody(usernameList[idx], Charset.forName("utf-8")));
			mutiEntity.addPart("SpaceID",new StringBody("1", Charset.forName("utf-8")));
			mutiEntity.addPart("Action",new StringBody("submit", Charset.forName("utf-8")));
			 
			post.setEntity(mutiEntity);
			response = client.execute(post);
			HttpEntity httpEntity =  response.getEntity();
			String content = EntityUtils.toString(httpEntity);
			System.out.println(response.getStatusLine().getStatusCode());
			if(response.getStatusLine().getStatusCode() != 200){
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
		Pattern p = Pattern.compile("<TD class=\"id\".*?>(\\d+)");
		System.out.println("getting maxrunid");
		try{
			get = new HttpGet("http://acm.timus.ru/status.aspx");
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
		}finally{
			EntityUtils.consume(entity);
		}
		
		Matcher m = p.matcher(html);
		if(m.find()){
			maxrunid[idx] = Integer.parseInt(m.group(1));
			System.out.println("maxrunid="+maxrunid[idx]);
			//System.out.println("Date: "+time+" Get maxRunId : "+maxRunId);
		}else{
			throw new RuntimeException();
		}
	}
	
	public void work() throws SQLException {
		// TODO Auto-generated method stub
		idx = getIdleClient();
		int errorCode = 1;
		System.out.println("URAL "+usernameList[idx]+" suozhu");
		try {
			submission.setStatus("Submitting");
			mysql.addOrModify(submission);
			if(maxrunid[idx]==0) getMaxRunId(idx);
			try {
				submit(idx);
			} catch (Exception e1) {
				e1.printStackTrace();
				login(usernameList[idx], passwordList[idx]);
				submit(idx);
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
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		synchronized (using) {
			using[idx] = false;
			System.out.println(submission.getId()+" tijiaowanbi use "+usernameList[idx]);
		}
	}

}
