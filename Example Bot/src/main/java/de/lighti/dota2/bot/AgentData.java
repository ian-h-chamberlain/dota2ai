package de.lighti.dota2.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import se.lu.lucs.dota2.framework.game.BaseEntity;
import se.lu.lucs.dota2.framework.game.BaseNPC;
import se.lu.lucs.dota2.framework.game.Hero;
import se.lu.lucs.dota2.framework.game.Tower;
import se.lu.lucs.dota2.framework.game.World;

public class AgentData {
	float hp, mp, range, gold, level;
	int team;
	float[] pos;
	public float reward;
	
	public float[] lastData;
	
	int tookDamage = 0;
	int damageCounter, framesToCheckDamage = 5;
	public float[][] shopLocations;
	public float nextReward = 0;
	BaseEntity owner;
	public ArrayList<String> inventory;
	public float rewardMult = 1;
	//public static int stateSize = 43; // MAKE SURE TO UPDATE ACCORDING TO parseGameState!
	public int inventoryValue = 0;
	public static int stateSize = 47; // MAKE SURE TO UPDATE ACCORDING TO parseGameState!
	
	BaseNPC enemyHero;
	
	public Set<BaseEntity> enemyHeroes;
	public Set<BaseEntity> friendlyHeroes;
	public Set<BaseEntity> enemyTurrets;
	public Set<BaseEntity> friendlyTurrets;
	public Set<BaseEntity> enemyCreeps;
	public Set<BaseEntity> friendlyCreeps;
	
	public final String heroName = "hero";
	public final String creepName = "creep";
	public final String turretName = "tower";
/*	
 * 
 * 
	public Set<BaseEntity> getAllEnemies(){
		
		return new HashSet<BaseEntity>(enemyHeroes);//.addAll(enemyHeroes);
	}
	*/
	/*
	public Set<BaseEntity> getUnitsByPredicate(World world, BaseEntity center, float range, Predicate<Object> predicate){
		Predicate<BaseEntity> p1 = (p -> p instanceof BaseNPC);
		Set<BaseEntity> entities = findEntitiesInRange( world, center, range).stream().filter(p1).filter(predicate).collect( Collectors.toSet() );
		return entities;
	}*/
	
	
	
	public static BaseEntity getNearest(Set<BaseEntity> set, float[] point){
		float dist = Float.MAX_VALUE;
		BaseEntity obj = null;
		for(BaseEntity e : set){
			float tdist = Vec3.distance(e.getOrigin(),point);
			if(tdist < dist){
				dist = tdist;
				obj = e;
			}
		}
		return obj;
	}
	
	public Set<BaseEntity> getUnitsByKeyword(World world, BaseEntity center, float range, int team, String keyWord){
		Predicate<BaseEntity> p1 = (p -> p instanceof BaseNPC && ((BaseNPC)p).getTeam() == team && ((BaseNPC)p).getName().contains(keyWord) && ((BaseNPC)p).getName() != owner.getName());
		Set<BaseEntity> entities = findEntitiesInRange( world, center, range * 10).stream().filter(p1).collect( Collectors.toSet() );
		return entities;
	}
	
	public void populate(Hero agent, World world){
		enemyHeroes = getUnitsByKeyword(world,agent,range,3,heroName);
		friendlyHeroes = getUnitsByKeyword(world,agent,range,2,heroName);
		enemyTurrets = getUnitsByKeyword(world,agent,range,3,turretName);
		friendlyTurrets = getUnitsByKeyword(world,agent,range,2,turretName);
		enemyCreeps = getUnitsByKeyword(world,agent,range,3,creepName);
		friendlyCreeps = getUnitsByKeyword(world,agent,range,2,creepName);
//		System.out.println("found: " + enemyHeroes.size() + " enemyheroes " + "found: " + friendlyHeroes.size() + " enemyheroes " +
//		"found: " + enemyTurrets.size() + " enemyTurrets " + "found: " + friendlyTurrets.size() + " friendlyTurrets " + 
//		"found: " + enemyCreeps.size() + " enemyCreeps " + "found: " + friendlyCreeps.size() + " friendlyCreeps ");
	}
	
	public AgentData()
	{
		inventory = new ArrayList<String>();
		
		shopLocations = new float[][]
				{
					//radiant spawn
					{-7232f,-6644f, 0f },
					//dire spawn
					{7168f, 6097f, 0f},
					//radiant secret
					{-4726.10f, 1100f, 0f},
					//dire secret
					{4792f, -1700f, 0f },
					//bot side shop
					{7168f, -4200f, 0f},
					//top side shop
					{-7150f, 4000f, 0f}
				};
	}
	
	public float[] parseGameState(Hero agent, World world){
		
		// check if we took damage since last frame
		float oldHP = hp;
		
    	//obtain game data from agent
    	//health and mp are float percentages.
		team = agent.getTeam();
		reward = getReward(agent);
		owner = agent;
    	hp = (float)agent.getHealth() / (float)agent.getMaxHealth();
    	mp = (float)agent.getMana() / (float)agent.getMaxMana();
    	range = agent.getAttackRange();
    	gold = (float)agent.getGold();
    	level = (float)agent.getLevel();
    	pos = agent.getOrigin();
    	
    	// update tookDamage
    	damageCounter++;
    	if (Math.abs(oldHP - hp) > 0.01)
    	{
    		tookDamage = 1;
    		damageCounter = 0;
    	}
    	else if (damageCounter > framesToCheckDamage)
    	{
    		tookDamage = 0;
    	}
    	

    	final Set<BaseEntity> ents = findEntitiesInRange(world, agent, range*10);
        
        // determine what enemies are in what areas
        float[] nearbyEnemyCreeps = new float[8];
        Arrays.fill(nearbyEnemyCreeps, 0f);
        float[] nearbyEnemyHeroes = new float[8];
        Arrays.fill(nearbyEnemyHeroes, 0f);

        float[] nearbyFriendlyHeroes = new float[8];
        Arrays.fill(nearbyFriendlyHeroes, 0f);
        float[] nearbyFriendlyCreeps = new float[8];
        Arrays.fill(nearbyFriendlyCreeps, 0f);
        
        int enemyTower = 0;
        int friendlyTower = 0;
        
        Iterator<BaseEntity> itr = ents.iterator();
        
        while (itr.hasNext())
        {
        	BaseEntity ent = itr.next();
        	
        	// calculate which slice the enemy is in
			float angle = Vec3.angle(Vec3.sub(ent.getOrigin(), agent.getOrigin()));
			angle += Math.PI;
			
			int slice = (int) (angle / (Math.PI / 4f));
			if(slice >= 8) {
				slice -= 8;
			}

			float distance = Vec3.distance(ent.getOrigin(), agent.getOrigin());
			float danger = (float) Math.pow((distance / range), -1f);
			
			danger = Math.min(danger, 1000f);
			
			danger *= (float) ent.getHealth();
			
        	if (enemyHeroes.contains(ent)) {
        		nearbyEnemyHeroes[slice] += danger;
        	}
        	else if (enemyCreeps.contains(ent)) {
        		nearbyEnemyCreeps[slice] += danger;
        	}
        	else if (enemyTurrets.contains(ent)) {
        		if (distance < ((Tower) ent).getAttackRange())
        		{
        			enemyTower = 1;
        		}
        	}
        	else if (friendlyHeroes.contains(ent)) {
        		nearbyFriendlyHeroes[slice] += danger;
        	}
        	else if (friendlyCreeps.contains(ent)) {
        		nearbyFriendlyCreeps[slice] += danger;
        	}
        	else if (friendlyTurrets.contains(ent)) {
        		if (distance < ((Tower) ent).getAttackRange())
        		{
        			friendlyTower = 1;
        		}
        	}
        }
        
        // Get info about the current target
        BaseNPC target = (BaseNPC) world.getEntities().get(agent.getAttackTarget());
        float targetHealth = 0, isTargeting = 0, inRange = 0, inTargetRange = 0;
        
        if (target != null) {
			
			targetHealth = (float) target.getHealth() / (float) target.getMaxHealth();
			isTargeting = 0;
			if (agent.equals(world.getEntities().get(target.getAttackTarget()))) {
				isTargeting = 1;
			}
			
			float distanceFromTarget = Vec3.distance(target.getOrigin(), agent.getOrigin());
			
			inRange = 0;
			if (distanceFromTarget < agent.getAttackRange()) {
				inRange = 1;
			}
			
			inTargetRange = 0;
			if (distanceFromTarget < target.getAttackRange()) {
				inRange = 1;
			}
        }
        
        //Package data and send to NN
        //I can make this more organized if necessary.
		float parsedData[] = new float[stateSize];
		
		parsedData[0] = hp;
		parsedData[1] = mp;
		parsedData[2] = range;
		parsedData[3] = level;
		parsedData[4] = gold;
		for (int i=0; i<3; i++)
		{
			parsedData[5+i] = pos[i];
		}
		for (int i=0; i<8; i++)
		{
			parsedData[8+i] = nearbyEnemyHeroes[i];
			parsedData[16+i] = nearbyEnemyCreeps[i];
			parsedData[24+i] = nearbyFriendlyHeroes[i];
			parsedData[32+i] = nearbyFriendlyCreeps[i];
		}
		
		parsedData[40] = enemyTower;
		parsedData[41] = friendlyTower;
		
		parsedData[42] = tookDamage;
		
		parsedData[43] = targetHealth;
		parsedData[44] = isTargeting;
		parsedData[45] = inRange;
		parsedData[46] = inTargetRange;
		
		lastData = parsedData.clone();
		
		System.err.println(Arrays.toString(parsedData));
		
		return parsedData;
	}
	
	public void reward(float reward){
		nextReward += reward;
	}
	
	private float getReward(Hero agent){ //THIS SHOULD ONLY BE CALLED AT THE START OF PARSEGAMESTATE!
		float reward = -1;
		reward += nextReward;
		nextReward = 0;
		
		BaseNPC closestEnemy = (BaseNPC)getNearest(enemyHeroes, agent.getOrigin());
		float damageReward = 0;
		if(enemyHero != null && closestEnemy != null  && enemyHero.getHealth() != 0 && closestEnemy.getHealth() != 0){
			
			if(closestEnemy.getName().equals(enemyHero.getName())){
				damageReward = enemyHero.getHealth() - closestEnemy.getHealth();
			}
		}
		if(pos != null && Vec3.distance(pos, agent.getOrigin()) < 1){
			System.out.println("sitting");
			reward -= 10;
		}
		enemyHero = closestEnemy;
		reward += damageReward;
		reward += (agent.getGold() - gold);
		reward += (((float)agent.getHealth()/(float)agent.getMaxHealth()) - hp) * 300;
		reward *= rewardMult;
		/*if(agent.getHealth() > agent.getMaxHealth()/2){
			reward += Vec3.distance(agent.getOrigin(), new float[]{-8000,-8000})/ 16000;
		}*/
		System.out.println("Reward: " + reward + " from " + damageReward + ", " + (10 * (agent.getGold() - gold)) + ", " + ((((float)agent.getHealth()/(float)agent.getMaxHealth()) - hp) * 300));
		//reward += 
		return reward;
	}
	
    private static Set<BaseEntity> findEntitiesInRange( World world, BaseEntity center, float range ) {
        final Set<BaseEntity> result = world.getEntities().values().stream().filter( e -> distance( center, e ) < range ).collect( Collectors.toSet() );
        result.remove( center );
        return result;
    }
    private static float distance( BaseEntity a, BaseEntity b ) {
        final float[] posA = a.getOrigin();
        final float[] posB = b.getOrigin();
        return distance( posA, posB );
    }

    private static float distance( float[] posA, float[] posB ) {
        return (float) Math.hypot( posB[0] - posA[0], posB[1] - posA[1] );
    }
}
