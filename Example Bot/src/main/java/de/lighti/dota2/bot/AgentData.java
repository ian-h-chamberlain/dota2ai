package de.lighti.dota2.bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
	float[] pos;
	List<Float> abilityDamages;
	List<Float> coolDowns;
	List<Float> enemyDistances;
	HashMap<String, Float> towerDistances;
	
	BaseEntity owner;
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
	
	
	public AgentData(BaseEntity o)
	{
		owner = o;
		hp = mp = range = gold = level = 0.0f;
		abilityDamages = new ArrayList<Float>();
		coolDowns = new ArrayList<Float>();
		enemyDistances = new ArrayList<Float>();
		towerDistances = new HashMap<String, Float>();
	}
	public float[] parseGameState(Hero agent, World world){
    	//obtain game data from agent
    	//health and mp are float percentages.
    	hp = (float)agent.getHealth() / (float)agent.getMaxHealth();
    	mp = (float)agent.getMana() / (float)agent.getMaxMana();
    	range = agent.getAttackRange();
    	gold = (float)agent.getGold();
    	level = (float)agent.getLevel();
    	pos = agent.getOrigin();
    	
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
    	}*/
    	
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
        
        //Package data and send to NN
        //I can make this more organized if necessary.
		float parsedData[] = { hp, mp, range, level, gold, pos[0], pos[1], pos[2] };
		return parsedData;
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
