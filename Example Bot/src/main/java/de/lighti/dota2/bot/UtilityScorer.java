package de.lighti.dota2.bot;

import java.util.List;

import se.lu.lucs.dota2.framework.game.BaseEntity;

public class UtilityScorer {
	
	AgentData data;
	static final int numPoints = 10;
	static final float longRange = 3000;
	static final float shortRange = 800;
	
	float[] currentDest;
	
	
	public UtilityScorer(AgentData d){
		data = d;
		currentDest = d.pos;
	}
	
	public float[] getPoint(float[] pos){
		List<float[]> positions = new ArrayList<float[]>();
		
	}
	
	
	public float scorePoint(float[] pos){
		return 0;
	}
	
	
	
	public static float[] randomVec(float[] pos, float dist){
		
	}
}
