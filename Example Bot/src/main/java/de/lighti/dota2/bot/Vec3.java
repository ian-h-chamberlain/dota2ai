package de.lighti.dota2.bot;

import java.util.Random;

public class Vec3 {
	static Random rand;
	public static float distance( float[] posA, float[] posB ) {
        return (float) Math.hypot( posB[0] - posA[0], posB[1] - posA[1] );
    }
	
	public static float magnitude(float[] vec){
		return (float) Math.hypot(vec[0], vec[1]);
	}
	
	public static float[] normalized(float[] vec){
		float mag = magnitude(vec);
		return new float[]{vec[0]/mag,vec[1]/mag};
	}
	
	public static float[] add(float[] vec1, float[] vec2){
		return new float[]{vec1[0] + vec2[0],vec1[1] + vec2[1] };
	}
	
	public static float[] mult(float[] vec, float coef){
		return new float[]{vec[0] * coef, vec[1] * coef};
	}
	
	public static float[] randomPoint(float[] origin, float range){
		if(rand == null){
			rand = new Random();
		}		
		float[] point = new float[]{rand.nextFloat(),rand.nextFloat()};
		return mult(normalized(point),range);
	}
}
