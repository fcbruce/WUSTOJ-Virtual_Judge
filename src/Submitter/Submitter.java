package Submitter;

import java.sql.SQLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import Connect_to_Mysql.Connect_to_mysql;

public abstract class Submitter extends Thread {
	
	protected int maxRunId = 0;
	protected int idx;
	
	public Submission submission;
	public abstract void work() throws SQLException;
	public abstract void waitForUnfreeze();
	static public Connect_to_mysql mysql = new Connect_to_mysql();
	
	public void run()
	{
		try {
			//System.out.println(Thread.currentThread().getName()+" using to judge "+submission.getId());
			work();
			//System.out.println();
			//System.out.println(Thread.currentThread().getName()+" : "+idx+" tijiao "+submission.getId()+" wanbi");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		waitForUnfreeze();
		//System.out.println("shifangwanbi");
	}
	
	public Submission getSubmission() {
		return submission;
	}
	public void setSubmission(Submission submission) {
		this.submission = submission;
	}
	
}
