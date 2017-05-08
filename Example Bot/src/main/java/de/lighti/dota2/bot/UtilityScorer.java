package de.lighti.dota2.bot;

import java.util.ArrayList;
import java.util.List;

import se.lu.lucs.dota2.framework.game.BaseEntity;
import se.lu.lucs.dota2.framework.game.BaseNPC;
import se.lu.lucs.dota2.framework.game.Hero;

public class UtilityScorer {
	
	AgentData data;
	static final int numPoints = 50;
	static final float longRange = 8000;
	static final float shortRange = 1500;
	
	static final float towerRange = 1000;
	static final float epsilon = 150;
	float[] currentDest;
	float[] lastPos;
	public float[] currentMode;
	public enum Constants{
		sittingStill,enemyTurretMultiplier,enemyMinionInRange,enemyHeroMultiplier,alliedHeroMultiplier,alliedMinionMulitplier,alliedTurretMultiplier,
		preferredX,preferredY, preferredMult,dist,stickiness
	}
	public static final float[] farm = new float[]{-1000,20,1000,2,-1,-2,0, 0,0 ,-.0001f,-.01f,0};

	public static final float[] laneSwitch = new float[]{-2000,20,1000,2,-1,-2,0, 0,0 ,-.0001f,.05f,1500};
	public static final float[] backOff = new float[]{0,5,-1000,5,-5,-1,-5, -6000,-6000 ,-.0001f,-.01f,0};
	public static final float[] guard = new float[]{0,5,1000,-5,-2,0,-5, -6000,-6000 ,-.0001f,0,0};

	public static final float[] brawl = new float[]{-300,2,1000,-5,-1,-2,0, 0,0 ,-.0001f,-.01f,0};
	
	public static final float[] gank = new float[]{-1000,10,0,-5,-1,0,0, 0,0 ,0,0,-1};

	public static final float[] siege = new float[]{-1000,1,0,2,-5,-5,0, 0,0 ,-.0001f,-.01f,0};
	
	public UtilityScorer(AgentData d){
		data = d;
		currentDest = d.pos;
		currentMode = farm;
		lastPos = new float[]{0,0};
	}
	
	public float[] getPoint(float[] pos){
		List<float[]> positions = new ArrayList<float[]>();
		//System.out.println("STARTING SCORE==================================");
		if(currentDest != null){
			positions.add(currentDest);
		}
		for(int i = 0; i < numPoints; i++){
			if(i < numPoints/2){
				positions.add(Vec3.add(Vec3.randomPoint(data.pos, longRange),data.pos));
			}else{
				positions.add(Vec3.add(Vec3.randomPoint(data.pos, shortRange),data.pos));
			}
		}
		float[] bestPos = positions.get(0);
		float score = scorePoint(bestPos);
		//System.out.println("current: " + score);
		for(int i = 0; i < positions.size(); i++){
			
			float tscore = scorePoint(positions.get(i));
			if(tscore > score){
		//		System.out.println("replaced by: " + tscore);
				score = tscore;
				bestPos = positions.get(i);
			}
			//System.out.println("Trying point " + positions.get(i)[0] + positions.get(i)[1]);
		}
		//System.out.println("Returning " + Vec3.str(bestPos) + " with score: " + score);
		lastPos = data.pos;
		currentDest = bestPos;
		return bestPos;
		
	}
	
	public float distToEntity(BaseEntity e, float[] origin){
		if(e == null){
			return Float.MAX_VALUE;
		}
		float dist = Vec3.distance(e.getOrigin(), origin);
		//System.out.println(dist + " from " + e.getName());
		return dist;
	}
	
	public float scorePoint(float[] pos){
		BaseEntity enemyTurret = AgentData.getNearest(data.enemyTurrets, pos);
		BaseEntity enemyMinion = AgentData.getNearest(data.enemyCreeps, pos);
		BaseEntity enemyHero = AgentData.getNearest(data.enemyHeroes, pos);
		BaseEntity allyTurret = AgentData.getNearest(data.friendlyTurrets, pos);
		BaseEntity allyMinion = AgentData.getNearest(data.friendlyCreeps, pos);
		BaseEntity allyHero = AgentData.getNearest(data.friendlyHeroes, pos);
		/*float etScore = 0;
		float emScore = 0;
		float ehScore = 0; 
		float amScore = 0;
		float ahScore = 0;*/
		float sitScore = Vec3.distance(lastPos, pos) < epsilon ? currentMode[(int)Constants.sittingStill.ordinal()] : 0;
		float etScore = currentMode[Constants.enemyTurretMultiplier.ordinal()] * 
				clamp(distToEntity(enemyTurret,pos),0,towerRange);
		
		
		float emScore = (distToEntity(enemyMinion,pos) < data.range) ? currentMode[Constants.enemyMinionInRange.ordinal()] : 0;	
		
		float ehScore = currentMode[Constants.enemyHeroMultiplier.ordinal()] *
				clamp(distToEntity(enemyHero,pos),0,data.range);
		float amScore = currentMode[Constants.alliedMinionMulitplier.ordinal()] *
				clamp(distToEntity(allyMinion,pos),0,data.range);
		float ahScore = currentMode[Constants.enemyTurretMultiplier.ordinal()] *
				clamp(distToEntity(allyHero,pos),0,data.range);
		float atScore = currentMode[Constants.alliedTurretMultiplier.ordinal()] *
				clamp(distToEntity(allyTurret,pos),0,data.range);	
		float preferredPointScore = currentMode[Constants.preferredMult.ordinal()] * Vec3.distance(pos, 
				new float[]{currentMode[Constants.preferredX.ordinal()],currentMode[Constants.preferredY.ordinal()]});
		float distScore = currentMode[Constants.dist.ordinal()] * Vec3.distance(pos, data.pos);
		//if(allyHero != null){
		float stickinessScore = Vec3.equals(this.currentDest,pos) ? currentMode[Constants.stickiness.ordinal()] : 0;
		//System.out.println("SittingStill: " + sitScore + " et: " + etScore + " em: "  + +emScore + " eh: " + ehScore+ " am: " +amScore+ "ah: " +ahScore + " at: " + atScore + " dist: " + distScore + " pp: " + preferredPointScore + "Trying point: " + Vec3.str(pos) + " dist: " + Vec3.distance(pos,data.pos));
		
		return sitScore+etScore+emScore+ehScore+amScore+ahScore + atScore +distScore + stickinessScore+ preferredPointScore;
	}
	
	public static float clamp(float val,float min,float max){
		return Math.max(min, Math.min(max, val));
	}
	
}
