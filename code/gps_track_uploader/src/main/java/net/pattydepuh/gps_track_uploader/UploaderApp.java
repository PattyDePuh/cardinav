/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pattydepuh.gps_track_uploader;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import com.sun.org.apache.xerces.internal.parsers.XMLParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author pattydepuh
 */
public class UploaderApp {
    
    static String DB_Address = "";
	
    static Connection conn;
    static final String USER = "postgres";
    static final String PASS = "cardinav";
    static final String DB_URL = "jdbc:postgresql://vm009.rz.uos.de:5433/cardinav_db";
    static final String TABLENAME = "jogger_points";
    static final String INSERT_QUERY = "Insert into jogger_points(track_id, the_geom) values ";
    static final String TUPEL_TEMPLATE = "(%1$d, ST_Transform(ST_GeomFromEWKT('SRID=4326;POINT(%2$.7f %3$.7f)'), 32632) )";
    static Statement st;


    public static void main(String[] argv) throws Exception{
        //Baue Verbindung zur Datenbank auf.
        makeDBConnection();

        //Wähle GPS-Trajektorien
        JFileChooser chooser = new JFileChooser();
        int code = chooser.showOpenDialog(null);
        if(code != JFileChooser.APPROVE_OPTION){
            System.err.println("Fehlercode " + code + " im FileChooser. -> Exit"); 
        }
        
        //Öffne und Parse die Datei
        File file = chooser.getSelectedFile();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();
        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
        
        //Starte Schleife
        NodeList nList = doc.getElementsByTagName("trkseg");
        System.out.println("Anzahl Tracks: "+ nList.getLength());
        for(int i = 1; i < nList.getLength(); i++){
            Element nNode = (Element) nList.item(i);
            System.out.println(nNode);
            NodeList pointList = nNode.getElementsByTagName("trkpt");
            System.out.println("Anzahl Points: "+ pointList.getLength());
            String query = INSERT_QUERY;
            boolean first = true;
            for(int j = 0; j < pointList.getLength(); j++){
                if(!first){
                    query += ",";
                }else{
                    first = false;
                }
                Element trackpoint = (Element) pointList.item(j);
                String lat = trackpoint.getAttribute("lat");
                String lon = trackpoint.getAttribute("lon");
                System.out.println("lat:" + lat + " lon:" + lon);
                query += String.format(Locale.US, TUPEL_TEMPLATE, i, Float.valueOf(lon), Float.valueOf(lat));
            }
            query += ";";
            st.execute(query);
        }
        //Fertig, schließe die Verbindung und die Datei.
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
                conn = DriverManager.getConnection(DB_URL,USER,PASS);
                // System.out.println("Connection to postgres established.");
        }
        catch (SQLException sqle) {
                System.out.println("Could not connect to postgres server!");
                System.exit(1);
        }
        /*try {
                ((org.postgresql.PGConnection) conn)
                .addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
        }
        catch (SQLException | ClassNotFoundException sqle) {
                System.out.println("Could not add postgis data types!");
                System.exit(1);
        }*/
        try {
                st = conn.createStatement();
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
