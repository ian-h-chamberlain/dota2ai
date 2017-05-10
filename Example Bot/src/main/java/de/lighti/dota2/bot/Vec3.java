package de.lighti.dota2.bot;

import java.util.Random;

public class Vec3 {
	static Random rand;
	public static float distance( float[] posA, float[] posB ) {
        return (float) Math.hypot( posB[0] - posA[0], posB[1] - posA[1] );
    }
	
	public static float angle(float[] vec) {
		return (float) Math.atan2(vec[1], vec[0]);
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
	
	public static float[] sub(float[] vec1, float[] vec2){
		return new float[]{vec1[0] - vec2[0], vec1[1] - vec2[1]};
	}
	
	public static float[] mult(float[] vec, float coef){
		return new float[]{vec[0] * coef, vec[1] * coef};
	}
	
	public static boolean equals(float[] vec1, float[] vec2){
		if(vec1 == vec2){
			return true;
		}
		if(vec1 == null || vec2 == null || vec1.length != vec2.length){
			return false;
		}
		if(vec1[0] == vec2[0] && vec1[1] == vec2[1]){
			return true;
		}
		return false;
	}
	
	public static String str(float[] vec){
		String str = "(";
		for(int i = 0; i < vec.length; i++){
			str += vec[i] + ",";
		}
		str += ")";
		return str;
	}
	
	public static float[] randomPoint(float[] origin, float range){
		if(rand == null){
			rand = new Random();
		}
		float dist = rand.nextFloat() * range;
		float[] point = mult(normalized(new float[]{rand.nextFloat()-.5f,rand.nextFloat() -.5f}),dist);
		//System.out.println(str(point));
		return point;
	}
}
