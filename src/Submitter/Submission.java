package Submitter;

import java.util.Date;

public class Submission {
	
	private int id;			//Hibernate缁熺紪ID
	private String status;		//鐘舵��
	private String additionalInfo;	//棰濆淇℃伅锛岃褰曠紪璇戦敊璇瓑淇℃伅
	private String realRunId;	//鍦ㄥ師OJ鐨凴unId

	private int time;			//杩愯鏃堕棿(鏈狝C鎻愪氦涓虹┖	鍗曚綅:ms)
	private int memory;			//杩愯鍐呭瓨(鏈狝C鎻愪氦涓虹┖	鍗曚綅:KB)
	private Date subTime;		//鎻愪氦鏃堕棿
	
	private String language;	//璇█
	private String source;		//婧愪唬鐮�
	private String originPid;
	
	private String user;
	private String soj;
	
	public String getUser() {
		return user;
	}
	
	public String getSoj() {
		return soj;
	}
	
	public String getRealRunId() {
		return realRunId;
	}
	public void setRealRunId(String realRunId) {
		this.realRunId = realRunId;
	}
	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setAdditionalInfo(String additionalInfo) {
		if (additionalInfo != null && additionalInfo.length() > 10000) {
			additionalInfo = additionalInfo.substring(0, 10000) + "\n\n鈥︹�︹�︹��";
		}
		this.additionalInfo = additionalInfo;
	}
	
	public String getAdditionalInfo() {
		return additionalInfo;
	}
	
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getPid()
	{
		return originPid;
	}
	
	public void setPid(String pid)
	{
		this.originPid = pid;
	}
	
	public void setId(int id)
	{
		this.id = id;
	}
	public int getId() {
		// TODO Auto-generated method stub
		return id;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public int getMemory() {
		return memory;
	}
	public void setMemory(int memory) {
		this.memory = memory;
	}
	
	public void setUser(String user) {
		this.user=user;
	}
	
	public void setSoj(String soj) {
		this.soj=soj;
	}
	
}
