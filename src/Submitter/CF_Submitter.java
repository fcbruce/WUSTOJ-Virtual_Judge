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
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.EncoderException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
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
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class CF_Submitter extends Submitter{
	
	static private boolean using[];
	static private int maxrunid[];
	static private String[] csrf;
	static private boolean first[];
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
	    	   	if (info.length >= 3 && info[0].equalsIgnoreCase("CodeForces"))
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
       csrf = new String[usernameList.length];
       first = new boolean[usernameList.length];
       	
       clientList = new DefaultHttpClient[usernameList.length];

		for (int i = 0; i < clientList.length; i++){
			csrf[i]="4033153aa65478f00fdd144c1c282213";
			first[i]=true;
			clientList[i] = new DefaultHttpClient();
			clientList[i].getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/4.4.3.4000 Chrome/30.0.1599.101 Safari/537.36");
			clientList[i].getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
			clientList[i].getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
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
			
			//clientList[i].getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
       
	}
	
	private void login(int idx) throws ClientProtocolException, IOException{
		try{
			System.out.println("login");
			
			get = new HttpGet("http://codeforces.com/enter");
			response = client.execute(get);
			entity = response.getEntity();
			html = EntityUtils.toString(entity);
			html=html.substring(html.indexOf("'csrf_token' value='")+"'csrf_token' value='".length());
			html=html.substring(0,html.indexOf("'"));
			csrf[idx]=html;
			//System.out.println(html);
			
			
			post = new HttpPost("http://codeforces.com/enter");
			List<NameValuePair> nvps = new ArrayList<NameValuePair> ();

			nvps.add(new BasicNameValuePair("action","enter"));
			nvps.add(new BasicNameValuePair("csrf_token",csrf[idx]));
			nvps.add(new BasicNameValuePair("handle",usernameList[idx]));
			nvps.add(new BasicNameValuePair("password",passwordList[idx]));
			nvps.add(new BasicNameValuePair("remember","on"));
			
			post.setEntity(new UrlEncodedFormEntity(nvps,Charset.forName("UTF-8")));
			
			response = client.execute(post);
			entity = response.getEntity();

			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY){
				throw new RuntimeException();
			}
			System.out.println("login done");
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
	
	public static String unicodeToUtf8(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len;) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            value = (value << 4) + aChar - '0';
                            break;
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                            value = (value << 4) + 10 + aChar - 'a';
                            break;
                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
                            value = (value << 4) + 10 + aChar - 'A';
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    "Malformed   \\uxxxx   encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }

	
	private void getAdditionalInfo(String runId,int idx) throws HttpException, IOException {
		try{
			post = new HttpPost("http://codeforces.com/data/judgeProtocol");
			List<NameValuePair> nvps = new ArrayList<NameValuePair> ();

			nvps.add(new BasicNameValuePair("submissionId",runId));
			nvps.add(new BasicNameValuePair("csrf_token",csrf[idx]));
			
			post.setEntity(new UrlEncodedFormEntity(nvps,Charset.forName("UTF-8")));
			
			response = client.execute(post);
			entity = response.getEntity();

			html = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			
		}finally{
			EntityUtils.consume(entity);
		}
		String info =unicodeToUtf8(html.substring(1, html.length()-1));
			
		submission.setAdditionalInfo(info);
	}
	
	public void getResult(int idx) throws Exception{
		Pattern p = Pattern.compile("submissionId=\"(\\d*)\">[\\S\\s]*?(<td class=\"status-cell status-small status-verdict-cell\"[\\S\\s]*?)</td>[\\S\\s]*?<td class=\"time-consumed-cell\">\\s*(\\d*)&nbsp;ms[\\S\\s]*?<td class=\"memory-consumed-cell\">\\s*(\\d*)&nbsp;KB");
		
		long cur = new Date().getTime(), interval = 500;
		
		int errorcnt=0;
		
		while(new Date().getTime() - cur < 600000){
			try{
				get = new HttpGet("http://codeforces.com/problemset/status?friends=on");
				response = client.execute(get);
				entity = response.getEntity();
				html = EntityUtils.toString(entity);
			}finally{
				EntityUtils.consume(entity);
			}
			
			Matcher m = p.matcher(html);
			if(m.find() && Integer.parseInt(m.group(1)) > maxrunid[idx]){
				String result = m.group(2).replaceAll("<[\\s\\S]*?>", "").trim();
				submission.setStatus(result);
				submission.setRealRunId(m.group(1));
				
				System.out.println("id="+submission.getRealRunId());
				if(!result.contains("ing") && !result.contains("queue"))
				{
					if(result.contains("Compilation"))
					{
						getAdditionalInfo(submission.getRealRunId(),idx);
					}
					else
					{
						submission.setMemory(Math.abs(Integer.parseInt(m.group(4).replaceAll("K", ""))));
						submission.setTime(Integer.parseInt(m.group(3).replaceAll("MS", "")));
					}
					
					System.out.println(submission.getId()+" got it");
					System.out.println(result);
					
					System.out.println("MaxRunId="+maxrunid[idx]);
					maxrunid[idx]=Integer.parseInt(m.group(1));
					System.out.println("MaxRunId="+maxrunid[idx]);
					
					mysql.addOrModify(submission);
					return;
				}
				else mysql.addOrModify(submission);
					
				errorcnt=0;
				
			}else {
				errorcnt++;
				System.out.println(html);
			}
			
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
			System.out.println("Submit");
		    post = new HttpPost("http://codeforces.com/problemset/submit");
			
			int tempid=Integer.parseInt(submission.getPid());
			String strtemp=String.valueOf(tempid/121);
			tempid%=121;
			strtemp+=(char)('A'+tempid/11);
			tempid%=11;
			if(tempid>0) strtemp+=tempid;
			System.out.println("id="+strtemp);
			
			MultipartEntity mutiEntity = new MultipartEntity();
			mutiEntity.addPart("csrf_token",new StringBody(csrf[idx], Charset.forName("utf-8")));
			mutiEntity.addPart("action",new StringBody("submitSolutionFormSubmitted", Charset.forName("utf-8")));
			mutiEntity.addPart("submittedProblemCode",new StringBody(strtemp, Charset.forName("utf-8")));
			mutiEntity.addPart("programTypeId",new StringBody(submission.getLanguage(), Charset.forName("utf-8")));
			strtemp="\n";
			Random random = new Random();
            for(int i = 0; i < 50;i++) {
                switch(Math.abs(random.nextInt())%4)
                {
                	case 0:strtemp+=" ";break;
                	case 1:strtemp+="\n";break;
                	case 2:strtemp+="\t";break;
                	case 3:strtemp+="\r";break;
                }
            }
			mutiEntity.addPart("source",new StringBody(submission.getSource()+strtemp, Charset.forName("utf-8")));
			 
			post.setEntity(mutiEntity);
			response = client.execute(post);
			entity =  response.getEntity();
			System.out.println("status code="+response.getStatusLine().getStatusCode());
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY){
				throw new RuntimeException();
			}
			
			submission.setStatus("Submitted");
			mysql.addOrModify(submission);
			System.out.println("Submitted");
		}finally{
			EntityUtils.consume(entity);
		}
	}
	
	/*
	private void getMaxRunId(int idx) throws Exception {
		Pattern p = Pattern.compile("submissionId=\"(\\d*)\">");
		System.out.println("getting maxrunid");
		try{
			get = new HttpGet("http://codeforces.com/problemset/status");
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
		}else{
			throw new RuntimeException();
		}
	}
	*/
	
	public void work() throws SQLException {
		// TODO Auto-generated method stub
		idx = getIdleClient();
		int errorCode = 1;
		System.out.println("CodeForces "+idx+" suozhu");
		try {
			submission.setStatus("Submitting");
			mysql.addOrModify(submission);
			
			try {
				
				if(first[idx]){
					login(idx);
					first[idx]=false;
				}
				submit(idx);
			} catch (Exception e1) {
				e1.printStackTrace();
				login(idx);
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
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		synchronized (using) {
			using[idx] = false;
			System.out.println(submission.getId()+" tijiaowanbi use "+idx);
		}
	}
}
