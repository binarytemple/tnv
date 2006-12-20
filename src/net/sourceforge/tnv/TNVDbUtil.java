/**
 * Created on May 5, 2006
 * @author jgood
 * 
 * Database utility and accessor class
 */
package net.sourceforge.tnv;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * TNVDbUtil
 */
public class TNVDbUtil {

	protected static enum DB_TYPE{
		HSQLDB,MYSQL;
	}
	
	/**
	 * The type of the current instance 
	 */
	private static DB_TYPE instance_type;
	
	/**
	 * The current instance to use 
	 */
	private static TNVDbInterface instance;
	
	
	/**
	 * Create a database instance - will create a new instance only once
	 * @param type
	 * @return
	 */
	protected static TNVDbInterface createDBinstance(DB_TYPE type) {
		// if there already is an instance, remove it
		if ( instance != null ) {
			try {
				if ( type.equals(DB_TYPE.HSQLDB) )
					TNVDbEmbedded.getInstance().closeConnection();
				else
					TNVDbMysql.getInstance().closeConnection();
			}
			catch (SQLException ex) {
				TNVErrorDialog.createTNVErrorDialog(instance.getClass(), 
						"Error closing db connection. Error: ", ex);
			}
			instance = null;
		}
		instance_type = type;
		if ( type.equals(DB_TYPE.HSQLDB) )
			instance = TNVDbEmbedded.getInstance();
		else
			instance = TNVDbMysql.getInstance();
		return instance;
	}
	
	/**
	 * Return the current DB instance
	 * @return instance
	 */
	protected static TNVDbInterface getInstance() {
		// default is embedded
		if ( instance == null )
			TNVErrorDialog.createTNVErrorDialog(instance.getClass(), "Database has not been created.");
		return instance;
	}
	
	protected static DB_TYPE getType() {
		return instance_type;
	}
	
	/**
	 * Executes a query and returns a resultset
	 * @param conn
	 * @param expression
	 * @return resultset
	 * @throws SQLException
	 */
	protected static ResultSet query( Connection conn, String expression ) throws SQLException {
		Statement st = null;
		st = conn.createStatement();
		return st.executeQuery( expression );
	}


	/**
	 * Updates this database connection
	 * @param conn
	 * @param expression
	 * @throws SQLException
	 */
	protected static void update( Connection conn, String expression ) throws SQLException {
		Statement st = null;
		st = conn.createStatement(); // statements
		int i = st.executeUpdate( expression ); // run the query
		if ( i == -1 )
			TNVErrorDialog.createTNVErrorDialog(instance.getClass(), 
					"Database update error from expression: " + expression);
		st.close();
	}
	

}
