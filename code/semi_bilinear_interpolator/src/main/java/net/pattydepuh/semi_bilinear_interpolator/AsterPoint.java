/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pattydepuh.semi_bilinear_interpolator;

import java.util.Comparator;

/**
 *
 * @author pattydepuh
 */
class AsterPoint {
    
    public AsterPoint(int _gid, double _dist, float _x, float _y, float _z){
        gid = _gid;
        dist = _dist;
        x = _x;
        y = _y;
        z = _z;
    }
    
    public int gid;
    public double dist;
    public float x;
    public float y;
    public float z;
    
    static Comparator<AsterPoint> AsterComparator = new Comparator<AsterPoint>(){
        @Override
        public int compare(AsterPoint a, AsterPoint b){
           return (int) Math.round(a.dist - b.dist);
        }
    };
}
