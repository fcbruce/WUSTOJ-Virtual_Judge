import java.sql.ResultSet;
import java.sql.SQLException;

import Connect_to_Mysql.Connect_to_mysql;
import Submitter.ACdream_Submitter;
import Submitter.CF_Submitter;
import Submitter.HDU_Submitter;
import Submitter.HYSBZ_Submitter;
import Submitter.POJ_Submitter;
import Submitter.SPOJ_Submitter;
import Submitter.Submission;
import Submitter.URAL_Submitter;
import Submitter.UVALive_Submitter;
import Submitter.UVA_Submitter;
import Submitter.ZOJ_Submitter;

public class Main {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws SQLException, InterruptedException {
		// TODO Auto-generated method stub
		String sql = "SELECT * FROM solution where result<2 and source_oj>0 limit 0,3";
		Connect_to_mysql mysql = new Connect_to_mysql();
		
		mysql.update("UPDATE solution SET result=1 where result=3 and source_oj>0");

		ResultSet result;
		
		//new POJ_Submitter().start();
		
		while(true)
		{
			result = mysql.query(sql);
			while(result.next())
			{
				Submission submission = new Submission();
				
				String pid = result.getString("problem_id");
				String originoj = result.getString("source_oj");
				int id = result.getInt("solution_id");
				String language = result.getString("virtual_language");
				submission.setId(id);
				
				submission.setUser(result.getString("user_id"));
				submission.setSoj(originoj);
				
				String sql2 = "select * from source_code where solution_id = '"+id+"'";
				System.out.println(sql2);
				ResultSet result2 = mysql.query(sql2);
				String source = null;
				if(result2.next()) source = result2.getString("source");
				
				submission.setSource(source);
				submission.setLanguage(language);
				submission.setPid(pid);
				System.out.println("source_oj is "+originoj +"   pid is "+pid);
				submission.setStatus("Waitting");
				mysql.addOrModify(submission);

				System.out.println("now we need to judge "+id);
				if(originoj.equals("1"))
				{
					HDU_Submitter sub = new HDU_Submitter();
					sub.setSubmission(submission);
					//System.out.println("HDU Start");
					sub.start();
				}
				else if(originoj.equals("2"))
				{
					POJ_Submitter sub = new POJ_Submitter();
					sub.setSubmission(submission);
					System.out.println("POJ Start");
					sub.start();
				}
				else if(originoj.equals("3"))
				{
					ZOJ_Submitter sub = new ZOJ_Submitter();
					sub.setSubmission(submission);
					System.out.println("ZOJ Start");
					sub.start();
				}
				else if(originoj.equals("4"))
				{
					CF_Submitter sub = new CF_Submitter();
					sub.setSubmission(submission);
					System.out.println("CF Start");
					sub.start();
				}
				else if(originoj.equals("5"))
				{
					UVA_Submitter sub = new UVA_Submitter();
					sub.setSubmission(submission);
					System.out.println("UVA Start");
					sub.start();
				}
				else if(originoj.equals("6"))
				{
					UVALive_Submitter sub = new UVALive_Submitter();
					sub.setSubmission(submission);
					System.out.println("UVALive Start");
					sub.start();
				}
				else if(originoj.equals("7"))
				{
					ACdream_Submitter sub = new ACdream_Submitter();
					sub.setSubmission(submission);
					System.out.println("ACdream Start");
					sub.start();
				}
				else if(originoj.equals("8"))
				{
					HYSBZ_Submitter sub = new HYSBZ_Submitter();
					sub.setSubmission(submission);
					System.out.println("HYSBZ Start");
					sub.start();
				}
				else if(originoj.equals("9"))
				{
					SPOJ_Submitter sub = new SPOJ_Submitter();
					sub.setSubmission(submission);
					System.out.println("SPOJ Start");
					sub.start();
				}
				else if(originoj.equals("10"))
				{
					URAL_Submitter sub = new URAL_Submitter();
					sub.setSubmission(submission);
					System.out.println("URAL Start");
					sub.start();
				}
				
				result2.close();
				//Thread.sleep(5000);
			}
			result.close();
			Thread.sleep(5000);
		}
	}
		
		
}

