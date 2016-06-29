/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pattydepuh.xyz_uploader;

import net.pattydepuh.semi_bilinear_interpolator.AsterPoint;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import javax.swing.JFileChooser;
import java.sql.*;
import java.util.Locale;
import java.util.Set;

/**
 * Programm zum Upload von XYZ-Höhenpunkten auf eine PostgreSQL/PostGIS-Datenbank
 * @author pattydepuh
 */
public class App {
    
    //ID-Zaehler
    static long id_counter = 0;
    
    //Datenbank-Parameter
    static Connection conn = null;
    static final String USER = "postgres";
    static final String PASS = "cardinav";
    static final String DB_URL = "jdbc:postgresql://vm009.rz.uos.de:5433/cardinav_db";
    static final String TABLENAME = "aster_points";
    
    //Input Query
    static final String INSERT_QUERY = "Insert into aster_points(gid, the_geom) values ";
    static final String TUPEL_TEMPLATE = "(%1$d, ST_GeomFromEWKT('SRID=4326;POINT(%2$.7f %3$.7f %4$.7f)'))";
    
    //Download Query
    static final String DOWNLOAD_QUERY = "Select gid, ST_X(the_geom), ST_Y(the_geom) FROM ways_vertices_pgr LIMIT %1$d OFFSET %2$d ;";
    
    //Nachbarschaftspunkte
    static final String NEIGHBOUR_QUERY = "Select gid, ST_X(the_geom), ST_Y(the_geom), ST_Z(the_geom) FROM aster_points WHERE ST_Within(the_geom, ST_MakeEnvelope( %1$.7f -50, %2$.7f -90, %1$.7f +50, %2$.7f +90, 32632)"; 
    static final int BUFFER_WIDTH = 1024;
    
    public static void main(String[] arg)throws Exception{
        //Datenbankverbindung herstellen.
        try{
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
            //Datenbank-Typen
            //((org.postgresql.PGConnection)conn).addDataType("geometry","org.postgis.PGgeometry");
            //((org.postgresql.PGConnection)conn).addDataType("box3d","org.postgis.PGbox3d");
        }catch(SQLException e){
            System.err.println(e.getMessage());
            System.exit(1);
        }         

        //Erfasse Anzahl Knoten der Datenbank.
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) from ways_vertices_pgr");
        int sum_count = rs.getInt(1);
        int current_gid;
        
        //Große Schleife, um alle Punkt abzuarbeiten:
        for(current_gid = 1; current_gid < sum_count; current_gid += BUFFER_WIDTH){
            //Besorge die erste Charge an Punkten.
            st = conn.createStatement();
            ResultSet point_buffer = st.executeQuery(String.format(DOWNLOAD_QUERY, BUFFER_WIDTH, current_gid));
            while(!point_buffer.isAfterLast()){
                //gid und Punkt-Koordinaten erfassen.
                int gid = point_buffer.getInt(1);
                double e_x = point_buffer.getInt(2);
                double e_y = point_buffer.getInt(3);
                
                //besorge die Nachbarpunkte von der aster_points Tabelle
                st = conn.createStatement();
                ResultSet neighbours = st.executeQuery(String.format(NEIGHBOUR_QUERY, e_x, e_y));
                
                //Bereite die Container vor.
                Set<AsterPoint> quarters[] = new Set<AsterPoint>[];
                
                
                
                //in die nächste Zeile springen.
                point_buffer.next();
            }
        }
        
        
                
        /*
        //Schleife durch jeden Punkt
        String inputLine;
        String query = new String(INSERT_QUERY);
        boolean first = true;
        System.out.println("Beginne einzulesen ...");
        while(br.ready()){
            if(id_counter <= 840000){
                br.readLine();
                id_counter++;
                continue;
            }
            if(!first)
                query += ", ";
            else
                first = false;
            inputLine = br.readLine();
            String coords[] = inputLine.split(",");
            double x_coord = Double.valueOf(coords[0]);
            double y_coord = Double.valueOf(coords[1]);
            double z_coord = Double.valueOf(coords[2]);
            
            //Tupel erstellen und konkatinieren.
            String tupel = String.format(Locale.US, TUPEL_TEMPLATE, id_counter++, x_coord, y_coord, z_coord);
            query += tupel;
            
            //Nach 10000 Einträgen ein Zwischenergebniss abfeuern
            if(id_counter%10000 == 0){
                System.out.println("Zwischenergebnis");
                query += ";";
                //Zwischenergebniss abfeuern
                Statement st = conn.createStatement();
                st.executeUpdate(query);
                System.out.println("Query "+ id_counter + "abgefeuert!");
                query = new String(INSERT_QUERY);
                first = true;
            }
        }
        
        query += ";";
        //Query abfeuern
        Statement st = conn.createStatement();
        st.executeUpdate(query);

        */
        
        //Datenbankverbindung schliessen
        conn.close();
    }
    
    
    
}
