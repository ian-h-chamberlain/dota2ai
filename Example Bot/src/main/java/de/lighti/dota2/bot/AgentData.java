package de.lighti.dota2.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import se.lu.lucs.dota2.framework.game.Ability;
import se.lu.lucs.dota2.framework.game.BaseEntity;
import se.lu.lucs.dota2.framework.game.BaseNPC;
import se.lu.lucs.dota2.framework.game.Hero;
import se.lu.lucs.dota2.framework.game.Tower;
import se.lu.lucs.dota2.framework.game.World;

public class AgentData {
	float hp, mp, range, gold, level;
	List<Float> abilityDamages;
	List<Float> coolDowns;
	List<Float> enemyDistances;
	public AgentData()
	{
		hp = mp = range = gold = level = 0.0f;
		abilityDamages = new ArrayList<Float>();
		coolDowns = new ArrayList<Float>();
		enemyDistances = new ArrayList<Float>();
		
	}
	public void parseGameState(Hero agent, World world){
    	//obtain game data from agent
    	//health and mp are float percentages.
    	hp = (float)agent.getHealth() / (float)agent.getMaxHealth();
    	mp = (float)agent.getMana()/ (float)agent.getMaxMana();
    	range = agent.getAttackRange();
    	gold = (float)agent.getGold();
    	level = (float)agent.getLevel();
    	
    	abilityDamages = new ArrayList<Float>();
    	coolDowns = new ArrayList<Float>();
    	enemyDistances = new ArrayList<Float>();
    	//Get a list of the first four abilities, the other indices are probably items.
    	for (int i = 0; i < 4; i++){
    		Ability a = agent.getAbilities().get(i);
    		abilityDamages.add((float)a.getAbilityDamage());
    		coolDowns.add((float)a.getCooldownTimeRemaining());
    		//System.out.println(abilityDamages.get(i));
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
        
        //Tower info
        BaseEntity current = null;
        for (int i = 0; i < towers.size(); i++)
        {
        	current = towers.get(i);
        	//System.out.println("Enemy + " + i);
        	//System.out.println("Class of enemy tower " + current.getName());
        	//System.out.println(current.getMaxHealth());
        	//System.out.println(current.getHealth());
        	//System.out.println(current.getName());
        	//System.out.println(distance(current.getOrigin(), agent.getOrigin()));
        }
		
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
