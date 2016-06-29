package gps_track_uploader;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UploaderApp {

	static String DB_Address = "";
	
	static Connection conn;
	static Statement s;
	
	
	public static void main(){
		makeDBConnection();
		
		//TODO
		
		closeDB();
	}
	
	
	public static void makeDBConnection(){
		// Datenbankverbindung vorbereiten
		try {
			Class.forName("org.postgresql.Driver");
			// System.out.println("Postgresql driver registered.");
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println("Could not find postgresql driver!");
			System.exit(1);
		}
		try {
			conn = DriverManager.getConnection(DB_Address);
			// System.out.println("Connection to postgres established.");
		}
		catch (SQLException sqle) {
			System.out.println("Could not connect to postgres server!");
			System.exit(1);
		}
		try {
			((org.postgresql.PGConnection) conn)
			.addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
		}
		catch (SQLException sqle) {
			System.out.println("Could not add postgis data types!");
			System.exit(1);
		}
		catch (ClassNotFoundException e) {
			System.out.println("Could not add postgis data types!");
			System.exit(1);
		}
		try {
			s = conn.createStatement();
			System.out.println("Statement created.");
		}
		catch (SQLException sqle) {
			System.out.println("Could not create statement!");
			System.exit(1);
		}
	}

	static void closeDB(){
		if (conn != null){
			try {
				conn.close();
				// System.out.println("Connection to postgres closed.");	
			}
			catch (SQLException sqle) {
				System.out.println("Could not close connection!");
				System.exit(1);	
			}
		}
	}
}
