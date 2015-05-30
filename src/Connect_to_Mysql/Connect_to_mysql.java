package Connect_to_Mysql;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


import Submitter.Submission;

public class Connect_to_mysql {
	private String driver = "com.mysql.jdbc.Driver";
	private String url = "jdbc:mysql://127.0.0.1:3306/wustacm";
	private String user = "root";
	private String password = "dengran1387";
	private Connection conn;
	public Statement statement;
	
	public Connect_to_mysql(){
		try{
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);
			if(!conn.isClosed())
				System.out.println("Successfully connect the mysql");
		}catch(Exception e){
			System.out.println("There are some errors in the connecting");
			e.printStackTrace();
		}
	}
	
	public void ReConnect_to_mysql(){
		try{
			Class.forName(driver);
			conn = DriverManager.getConnection(url, user, password);
			if(!conn.isClosed())
				System.out.println("Successfully reconnect the mysql");
		}catch(Exception e){
			System.out.println("There are some errors in the reconnecting");
			e.printStackTrace();
		}
	}
	
	public void close() throws SQLException{
		if(!conn.isClosed())
			conn.close();
	}
	
	public Statement getStatement() throws SQLException
	{
		if(conn.isClosed())
		{
			System.out.println("conn is closed, can not get the statement");
			
			ReConnect_to_mysql();
			
			if(conn.isClosed()) throw new SQLException();
		}
		statement = conn.createStatement();
		return statement;
	}
	
	public void update(String sql) throws SQLException
	{
		if(conn.isClosed())
		{
			System.out.println("conn is closed, can not do the update");
			
			ReConnect_to_mysql();
			
			if(conn.isClosed()) throw new SQLException();
		}
		
		PreparedStatement tmpstatement  = conn.prepareStatement(sql);
		tmpstatement.executeUpdate(sql);
	}
	
	public ResultSet query(String sql) throws SQLException
	{
		if(conn.isClosed())
		{
			System.out.println("conn is closed, can not do the update");
			
			ReConnect_to_mysql();
			
			if(conn.isClosed()) throw new SQLException();
		}
		
		Statement tmpstatement = getStatement();
		ResultSet ret = tmpstatement.executeQuery(sql);
		return ret;
	}

	public void addOrModify(Submission submission) throws SQLException {
		// TODO Auto-generated method stub
		int solution_id = submission.getId();
		String result_content = submission.getStatus();
		int memory = submission.getMemory();
		int time = submission.getTime();
		String result = change(result_content);
		String sql = "update solution set result='"+result+"',result_content='"+result_content+"', memory='"+memory+"',time='"
		+time+"' where solution_id='"+solution_id+"'";
		//System.out.println("sql is "+sql);
		update(sql);
		if(result.equals("11"))
		{
			String ceinfo = submission.getAdditionalInfo();
			String sql3 = "select * from compileinfo where solution_id = "+solution_id;
			ResultSet r = query(sql3);
			if(r.next())
			{
				String sql2 = "update compileinfo set error = ? where solution_id = ?";
				PreparedStatement tmpstatement  = conn.prepareStatement(sql2);
				tmpstatement.setInt(2, solution_id);
				tmpstatement.setString(1, ceinfo);
				tmpstatement.executeUpdate();
			}
			else
			{
				String sql2 = "insert into compileinfo (solution_id, error) VALUES (?,?)";
				PreparedStatement tmpstatement  = conn.prepareStatement(sql2);
				tmpstatement.setInt(1, solution_id);
				tmpstatement.setString(2, ceinfo);
				tmpstatement.executeUpdate();
			}
			r.close();
		}
	}
	
	public void UpdateUserAndProblem(String user,String soj,String pid) throws SQLException {
		
		String sql="UPDATE users SET solved=(SELECT count(DISTINCT problem_id,source_oj) FROM solution WHERE user_id='"+user+"' AND result=4) WHERE user_id='"+user+"'";
		update(sql);
		
		sql="UPDATE users SET submit=(SELECT count(*) FROM solution WHERE user_id='"+user+"') WHERE user_id='"+user+"'";
		update(sql);
		
		sql="UPDATE problem SET accepted=(SELECT count(1) FROM solution WHERE problem_id='"+pid+"' AND source_oj="+soj+" AND result=4) WHERE problem_id="
				+pid+" AND source_oj="+soj;
		update(sql);
		
		sql="UPDATE problem SET submit=(SELECT count(1) FROM solution WHERE problem_id="+pid+" AND source_oj="+soj+") WHERE problem_id="
				+pid+" AND source_oj="+soj;
		update(sql);
		
		System.out.println("Updated user: "+user+" and problem: "+soj+" "+pid);
	}
	

	private String change(String result) {
		// TODO Auto-generated method stub

		if(result.contains("Compiling") || result.contains("compiling"))
			result = "2";
		else if(result.contains("Running") || result.contains("running"))
			result = "3";
		else if(result.contains("Acc") || result.contains("acc"))
			result = "4";
		else if(result.contains("Presentation") || result.contains("presentation"))
			result = "5";
		else if(result.contains("Wrong") || result.contains("wrong"))
			result = "6";
		else if(result.contains("Time") || result.contains("time"))
			result = "7";
		else if(result.contains("Memory") || result.contains("memory"))
			result = "8";
		else if(result.contains("Out") || result.contains("out"))
			result = "9";
		else if(result.contains("Runtime") || result.contains("runtime") || result.contains("Segmentation") ||
				result.contains("Non-zero Exit Code") || result.contains("Floating Point Error") || result.contains("Dangerous") || result.contains("Language")
				|| result.contains("Restricted") || result.contains("Idleness"))
			result = "10";
		else if(result.contains("Compilation") || result.contains("compilation") || result.contains("Compile Error") || result.contains("Compile_Error"))
			result = "11";
		else if(result.contains("Judge Error") || result.contains("System Error"))
			result = "14";
		else
			result = "3";
		return result;
	}
	
}
