package de.lighti.dota2.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import se.lu.lucs.dota2.framework.game.Ability;
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
	
	int tookDamage = 0;
	int damageCounter, framesToCheckDamage = 5;
	
	List<Float> abilityDamages;
	List<Float> coolDowns;
	List<Float> enemyDistances;
	HashMap<String, Float> towerDistances;
	
	BaseEntity owner;
	public static int stateSize = 43; // MAKE SURE TO UPDATE ACCORDING TO parseGameState!
	
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
		abilityDamages = new ArrayList<Float>();
		coolDowns = new ArrayList<Float>();
		enemyDistances = new ArrayList<Float>();
		towerDistances = new HashMap<String, Float>();
		
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
    	if (Math.abs(oldHP - hp) > 0.001)
    	{
    		tookDamage = 1;
    		damageCounter = 0;
    	}
    	else if (damageCounter > framesToCheckDamage)
    	{
    		tookDamage = 0;
    	}
    	
		abilityDamages.clear();
    	coolDowns.clear();
    	enemyDistances.clear();
    	
    	//Get a list of the first four abilities, the other indices are probably items.
    	for (int i = 0; i < 4; i++)
    	{
    		Ability a = agent.getAbilities().get(i);
    		abilityDamages.add((float)a.getAbilityDamage());
    		coolDowns.add((float)a.getCooldownTimeRemaining());
    	}
    	/*Map<Integer, Ability> map = agent.getAbilities();
    	for (Map.Entry<Integer, Ability> entry : map.entrySet())
    	{
    		System.out.println(entry.getKey() + "/" + entry.getValue());
    	}
    	
    	 //Obtain entities in a wide range (Attack Range * 10)
        final Set<BaseEntity> e = findEntitiesInRange( world, agent, range*10 ).stream().filter( p -> p instanceof BaseNPC )
                .filter( p -> ((BaseNPC) p).getTeam() == 3 ).collect( Collectors.toSet() );
     
        
        
        //Filter enemies in range (
        final ArrayList<BaseEntity> enemies = (ArrayList<BaseEntity>) e.stream().filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() ).collect(Collectors.toList());
        final ArrayList<BaseEntity> towers = (ArrayList<BaseEntity>) e.stream().filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() ).filter( f -> f.getClass() == Tower.class).collect(Collectors.toList());
        
        
        
        //Tower info, add all distances from visible enemy towers to this hash map.
        for (int i = 0; i < towers.size(); i++)
        {
        	towerDistances.put(towers.get(i).getName(), distance(pos, towers.get(i).getOrigin()));
        }
        */

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
			
			
			float distance = Vec3.distance(ent.getOrigin(), agent.getOrigin());
			float danger = (float) Math.exp((double) (-distance / range));
			
			danger *= (float) ent.getHealth() / (float) ent.getMaxHealth();
			
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
		
		return parsedData;
	}
	
	
	private float getReward(Hero agent){ //THIS SHOULD ONLY BE CALLED AT THE START OF PARSEGAMESTATE!
		float reward = -1;
		reward += 10 * (agent.getGold() - gold);
		reward += agent.getHealth() - hp;
		if(agent.getHealth() > agent.getMaxHealth()/2){
			reward += Vec3.distance(agent.getOrigin(), new float[]{-8000,-8000})/ 16000;
		}
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
