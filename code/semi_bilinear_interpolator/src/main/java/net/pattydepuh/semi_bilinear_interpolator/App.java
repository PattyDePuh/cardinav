/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pattydepuh.semi_bilinear_interpolator;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import static net.pattydepuh.semi_bilinear_interpolator.App.conn;


/**
 * Programm zum Upload von XYZ-Höhenpunkten auf eine PostgreSQL/PostGIS-Datenbank
 * @author pattydepuh
 */
public class App {
    
    //Fehler-Zaehler - wir tollerieren 500 Fehler
    static long error_counter = 0;
    static long error_max = 500;
    
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
    static final String DOWNLOAD_QUERY = "Select id, ST_X(the_geom), ST_Y(the_geom), ST_Z(the_geom) FROM ways_vertices_pgr WHERE ST_Z(the_geom) = -1 LIMIT %1$d OFFSET %2$d ;";
    
    //Nachbarschaftspunkte
    static final String NEIGHBOUR_QUERY = "Select gid, ST_X(the_geom), ST_Y(the_geom), ST_Z(the_geom) FROM aster_points WHERE ST_Within(the_geom, ST_MakeEnvelope( %1$.7f, %2$.7f, %3$.7f, %4$.7f, 32632));"; 
    static final int BUFFER_WIDTH = 1024;
    
    //Update Query
    static final String UPDATE_QUERY = "UPDATE ways_vertices_pgr SET the_geom = ST_SetSRID( ST_MakePoint( ST_X(the_geom), ST_Y(the_geom), %2$.7f),32632) WHERE id = %1$d;";
    
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
        rs.next();
        
        int sum_count = rs.getInt(1);
        int current_gid;
        
        //Große Schleife, um alle Punkt abzuarbeiten:
        //current_gid ist der Offset für die DownloadQuery - wird bei jedem fehlerhaften Punkt inkrementiert.
        //sum_count wird mit jeder erfolgreichen Berechnung heruntergezählt.
        for(current_gid = 1; current_gid < sum_count; /*current_gid += BUFFER_WIDTH*/){
            System.out.println("Fortschritt: "+ current_gid);
            //Besorge die erste Charge an Punkten.
            st = conn.createStatement();
            ResultSet point_buffer = st.executeQuery(String.format(Locale.US, DOWNLOAD_QUERY, BUFFER_WIDTH, current_gid));
            point_buffer.next();
            while(!point_buffer.isAfterLast()){
                //gid und Punkt-Koordinaten erfassen.
                int gid = point_buffer.getInt(1);
                float e_x = point_buffer.getFloat(2);
                float e_y = point_buffer.getFloat(3);
                
                System.out.println(gid);
                
                //besorge die Nachbarpunkte von der aster_points Tabelle
                st = conn.createStatement();
                //System.out.println(String.format(Locale.US, NEIGHBOUR_QUERY, e_x-100.0f, e_y-180f, e_x+100.0f, e_y+180.0f));
                ResultSet neighbours = st.executeQuery(String.format(Locale.US, NEIGHBOUR_QUERY, e_x-100.0f, e_y-180f, e_x+100.0f, e_y+180.0f));
                
                //Wenn es keine Nachbarn gibt, kann auch keine Höhe ermittlelt werden:
                if(!neighbours.next()){
                    point_buffer.next();
                    current_gid++;
                    continue;
                }
                
                //Bereite die QuartalContainer vor.
                List<List<AsterPoint>> quarters = new ArrayList<>();
                for(int i = 0; i < 4; i++){
                    quarters.add(i, new ArrayList<AsterPoint>());
                }
                //Ordne nun die Aster-Punkte den Quartalen zu;
                while(!neighbours.isAfterLast()){                 
                    double dist = Math.hypot(e_x - neighbours.getDouble(2), e_y - neighbours.getDouble(3));
                    AsterPoint focus = new AsterPoint(neighbours.getInt(1), 
                                dist, 
                                neighbours.getFloat(2), 
                                neighbours.getFloat(3), 
                                neighbours.getFloat(4));
                    if(focus.x < e_x){
                        //links
                        if(focus.y < e_y){
                            //unten
                            quarters.get(3).add(focus);
                        }else{
                            //oben
                            quarters.get(0).add(focus);
                        }
                    }else{
                        //rechts
                        if(focus.y < e_y){
                            //unten
                            quarters.get(2).add(focus);
                        }else{
                            //oben
                            quarters.get(1).add(focus);
                        }
                    }
                    neighbours.next();
                } //Ende Neighbour-Schleife
                
                //Sortiere die Quartale nach ihrer Distance
                for(int i = 0; i < 4; i++){
                    Collections.sort(quarters.get(i), 
                            AsterPoint.AsterComparator
                    );
                }    
                            
                //Erhalte jeweils den ersten Punkt von jedem Quartal.
                AsterPoint a, b, c, d;
                try{
                    a = quarters.get(0).get(0);
                    b = quarters.get(1).get(0);
                    c = quarters.get(2).get(0);
                    d = quarters.get(3).get(0);
                }catch(Exception e){
                    //Wenn bei einem Quartal keine Punkte drin sind, haben wir ein Problem:
                    System.out.println("Housten: " + e.getMessage());
                    //error_counter ++;
                    if(error_counter > error_max){
                        System.out.println("Schmerzgrenze geknackt, breche ab!");
                        System.exit(1);
                    }
                    
                    //TODO - Z-Wert für den Fehler ermitteln
                    point_buffer.next();
                    continue;
                }
                
                //Baue hier dir den e_z zusammen.
                //Zuerst Stützpunkt F (A<->B)
                float f_ratio = getRatio(a.x, b.x, e_x);
                AsterPoint f = new AsterPoint( -1, -1.0f, e_x, getTarget(a.y,b.y,f_ratio), getTarget(a.z,b.z,f_ratio));
                //Dann Stützpunkt G (C<->D)
                float g_ratio = getRatio(c.x, d.x, e_x);
                AsterPoint g = new AsterPoint( -1, -1.0f, e_x, getTarget(c.y,d.y,g_ratio), getTarget(c.z,d.z,g_ratio));
                //Abschließend Gesuchter Punkt E (F<->G)
                float e_ratio = getRatio(f.y, g.y, e_y);
                float e_z = getTarget(f.z, g.z, e_ratio);
                
                //TODO - Z-Wert in der Datenbank aktualisieren.
                st = conn.createStatement();
                int effect = st.executeUpdate(String.format(Locale.US, UPDATE_QUERY, gid, e_z)); //TODO
                if(effect == 0){
                    System.out.println("Punkt Nr:" + gid + "konnte nicht geupdated werden!");
                    error_counter++;
                }else{
                    sum_count--;
                }
                
                //in die nächste Zeile springen.
                point_buffer.next();
                
            }//Ende des Punkte Buffers
            
            
        }//Ende for-Schleife
        
        //Datenbankverbindung schliessen
        conn.close();
        System.out.println("done");
        
    } // Ende der main()
        
    //Linieare Interpolation - berechne die Ratio zu einem bestimmten Wert.
    public static float getRatio(float origin, float destin, float target){
        if(origin == destin) return 0;
        float target_diff = target - origin;
        float destination_diff = destin - origin;
        return target_diff / destination_diff;
    }
    
    //Lineare Interpolation - berechne den Wert, basierend auf den Ratio.
    public static float getTarget(float origin, float destin, float ratio){
        float destin_diff = destin - origin;
        float addition = destin_diff * ratio;
        return origin + addition;
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
        
        
}