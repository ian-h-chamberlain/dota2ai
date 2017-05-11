package de.lighti.dota2.bot;

import java.util.Random;
import java.util.Set;
import se.lu.lucs.dota2.framework.bot.Bot.Command;

import se.lu.lucs.dota2.framework.bot.BotCommands.Attack;
import se.lu.lucs.dota2.framework.bot.BotCommands.Cast;
import se.lu.lucs.dota2.framework.bot.BotCommands.Noop;
import se.lu.lucs.dota2.framework.bot.BotCommands.Move;
import se.lu.lucs.dota2.framework.bot.BotCommands.Buy;
import se.lu.lucs.dota2.framework.bot.BotCommands.Sell;

import se.lu.lucs.dota2.framework.game.Ability;
import se.lu.lucs.dota2.framework.game.BaseEntity;
import se.lu.lucs.dota2.framework.game.BaseNPC;
import se.lu.lucs.dota2.framework.game.Hero;
import se.lu.lucs.dota2.framework.game.Tower;
import se.lu.lucs.dota2.framework.game.World;

public class Action 
{
	Shop shop;
	Attack ATTACK;
	Buy BUY;
	Sell SELL;
	Cast CAST;
	Move MOVE;
	Noop NOOP;
	AgentData data;
	Set<BaseEntity> targets;
	
    private static final long attackAnimDelay = 200;
    private static long attackDelay = 1300;
    boolean BUYING = false;
    private long animCooldown = 0;
    private long attackSpeedCooldown = 0;
    float[][] thresholdTable;
    String[] buildOrder;
    int buildIndex = 0;
    int[] itemCosts;
    int boIndex = 0;
    //private long lastTime = 0;
    Random rand = new Random();
    public selectionType mode = selectionType.Farm;
    public enum selectionType
    {
    	enemyCreeps, enemyHeroes, enemyTurrets, allyCreeps, allEnemies, allAllies, Farm, Brawl, Push
    }
	public enum filterType
	{
		HEALTH, DISTANCE, LEVEL
	}
	
	public enum ability{
		Q,W,E,R,NONE
	}
	
	public static final int[] manaCosts = new int[]{40,0,0,60,0};
	public static final int[] ranges = new int[]{1800,0,0,2000,0};
	public ability currentCast = ability.NONE;
	
	public Action(Attack a, Cast c, Move m, Noop n, Buy b, Sell s, AgentData d)
	{
		ATTACK = a;
		CAST = c;
		MOVE = m;
		NOOP = n;
		BUY = b;
		SELL = s;
		data = d;
		init();
	}
	
	public void waitForMS(long ms){
		animCooldown = Math.max(System.currentTimeMillis() + ms, animCooldown);
	}
	private void init()
	{
		shop = new Shop();
		//items to buy
		buildOrder = new String[]
				{
					"item_wraith_band",
					"item_power_treads",
					"item_maelstrom",
					"item_broadsword",
					"item_blades_of_attack", //420
					"item_recipe_lesser_crit", // 500
					"item_demon_edge", //2400
					"item_recipe_greater_crit", // 1000
					"item_hyperstone", //2000
					"item_mjollnir_recipe" //900
					
				};
		
		//cost of items in build order
		itemCosts = new int[]
				{
					485,
					1350,
					2800,
					1200,
					420,
					500,
					2400,
					1000,
					2000,
					900
					
				};
		//matches selection type index
		//0.0f means zero priority 
		//1.0 means full priority
		thresholdTable = new float[][]
				{
					{1.0f, 0.0f, 0.0f},
					{0.0f, 1.0f, 0.0f},
					{0.0f, 0.0f, 1.0f},
					{1.0f, 0.0f, 0.0f},
					{1.0f, 1.0f, 1.0f},
					{1.0f, 1.0f, 1.0f},
					{0.7f, 0.5f, 0.3f},
					{0.7f, 1.0f, 0.3f},
					{1.0f, 0.0f, 1.0f}
				};
	}
	public Command update(Hero agent, World world, UtilityScorer scorer)
	{
		
		Command out = NOOP;
		if (agent.getGold() >= 1000 && agent.getGold() >= itemCosts[buildIndex])
		{
			System.out.println("Build order index: " + buildIndex + "," + buildOrder[buildIndex]);
			BaseEntity enemyHero = AgentData.getNearest(data.enemyHeroes, agent.getOrigin());
			if (enemyHero == null)
			{
				BUYING = true;
			}
			else {
				float dist = Vec3.distance(enemyHero.getOrigin(), agent.getOrigin());
				if (dist > 2500)
				{
					BUYING = true;
				}
			}
		}
		if (BUYING)
			return buy(agent, world, buildOrder[buildIndex]);

		//need input from mode or NN to set agent to buy items
		BaseEntity e = null;

        long t = System.currentTimeMillis();
        
        //targetFilter(agent, 0, filterType.HEALTH.ordinal());
        // 					  ^-- determined by utility scorer mode.
        e = targetFilter(agent, mode.ordinal());
        if(t < animCooldown){//if we're animating do nothing so we don't cancel.
        	return NOOP;
        }
        if(t > attackSpeedCooldown)
        {
        	
            if (e != null) 
            {
            	 System.out.println(e.getName());
            	 Command c =attack(agent, world, e);
            	 if(c != NOOP){
            		 return c;
            	 }
            	 //////.
            }
        } 
        if(currentCast.equals(ability.R)){
    		e= targetFilter(agent,selectionType.enemyHeroes.ordinal());
    	}
        if(canCast(agent,e,ranges[currentCast.ordinal()])){
        	//System.out.println("CASTING: " + currentCast);
        	Command c = this.castSpell(agent, e, world, this.currentCast.ordinal());
        	if(currentCast.equals(ability.R)){
        		this.waitForMS(2000);
        	}
        	return c;
        }
       out = goTo (scorer.getPoint(data.pos));
       return out;
	}
	public Command attack( Hero agent, World world, BaseEntity e)
	{
        if (e == null) 
        {
            //Nothing in range
//            System.out.println( "No enemy in range" );
            return NOOP;
        }
        final int targetindex = world.indexOf( e );
        
        if(isTargetable(agent,e,agent.getAttackRange())){
            ATTACK.setTarget( targetindex );
            this.waitForMS(attackAnimDelay);
        	attackSpeedCooldown = System.currentTimeMillis() + attackDelay;
        }else{
//        	System.out.println("can't attack this frame");
        	return NOOP;
        }
        return ATTACK;
	}
	
	public boolean canCast(Hero agent, BaseEntity e, float range){
		//BaseNPC target = (BaseNPC)e;
		if(!isTargetable(agent, e, range)){
//			System.out.println("Not targetable. Range;" + range);
			return false;
		}
		//Ability a = agent.getAbilities().get(currentCast.ordinal());
		/*if(a.getTargetTeam() != target.getTeam()){
			System.out.println("incorrect team");
			return false;
		}*/
		if(agent.getMana() < manaCosts[currentCast.ordinal()]){
//			System.out.println("not enough mana");
			return false;
		}
		
		return true;
	}
	
	public boolean isTargetable(Hero agent, BaseEntity e,float range){
		BaseNPC target = (BaseNPC)e;
		if(e == null){
			//System.out.println("null");
			return false;
		}
		if(agent == null) {
//			System.out.println("hero null");
			return false;
		}
		if(Vec3.distance(e.getOrigin(),agent.getOrigin()) > range){
//			System.out.println("out of range");
			return false;
		}
		if(target.getTeam() == agent.getTeam() && !target.isDeniable()){
//			System.out.println("not deniable");
			return false;
		}
		return true;
	}
	
	/*
	public Command spellHandler(Hero agent, World world, BaseEntity e)
	{
		Command command = NOOP;
		if (e.getClass() == Hero.class)
		{
			int ult = 3;
			if (agent.getMana() >= agent.getMaxMana() * 0.3)
				command = castSpell(agent, e, world, ult);
			

		}
		
		return command;
	}*/
    public Command castSpell( Hero agent, BaseEntity target, World world, int abilityIndex)
    {
        final int index = abilityIndex;
        final Ability a = agent.getAbilities().get( index );
        if (a.getAbilityDamageType() == Ability.DOTA_ABILITY_BEHAVIOR_POINT) {
            return NOOP;
        }
       // System.out.println( "will cast a spell" );
       // System.out.println( "Will try " + a.getName() );
        if (a.getLevel() < 1) 
        {
//            System.out.println( "Not learned yet" );
            return NOOP;
        }
        if (a.getCooldownTimeRemaining() > 0f) 
        {
//            System.out.println( "On cooldown" );
            return NOOP;
        }
        CAST.setAbility( index );
        if ((a.getBehavior() & Ability.DOTA_ABILITY_BEHAVIOR_UNIT_TARGET) > 0) {
            CAST.setX( -1 );
            CAST.setY( -1 );
            CAST.setZ( -1 );
            CAST.setTarget( world.indexOf( target ) );
        }
        else {
            CAST.setTarget( -1 );
            final float[] pos = target.getOrigin();
            CAST.setX( pos[0] );
            CAST.setY( pos[1] );
            CAST.setZ( pos[2] );
        }

        return CAST;
    }


	public BaseEntity targetFilter(Hero agent, int mode)
	{
		BaseEntity target = null;
		if (thresholdTable == null)
		{
			init();
		}
		filterType[] filters;
		//Threshold values based on percentage health values now
		//[Creep Health, Hero Health, Tower Health] <-- ex. [0.5f, 0.5f, 0.4f]
		//System.out.println(mode);
		if (mode == 6)
		{
			//"Farm Mode"
			Set<BaseEntity> enemyTargets = targetType(4);
			
			filters = new filterType[] { filterType.HEALTH, filterType.DISTANCE};
			target = filterSet(enemyTargets, thresholdTable[mode], filters);

			if (target == null)
			{
				Set<BaseEntity> allyTargets = targetType(5);
				target = filterSet(allyTargets, thresholdTable[mode], filters);
			}
			else
			{
				return target;
			}
		}
		else
		{
			Set<BaseEntity> targets = targetType(mode);
			filters = new filterType[] { filterType.HEALTH };
			target = filterSet(targets, thresholdTable[mode], filters);
		}

		return target;
	}
    private int threshold(BaseEntity e){
    	int thresholdIndex = 0;
		if (e.getClass() == BaseNPC.class)
		{
			thresholdIndex = 0;
		}
		else if (e.getClass() == Hero.class)
		{
			thresholdIndex = 1;
		}
		else if (e.getClass() == Tower.class)
		{
			thresholdIndex = 2;
		}
		return thresholdIndex;
    }
    

    public boolean contains(filterType[] filters, filterType f){
    	for (int i = 0; i < filters.length; i++){
    		if (filters[i] == f)
    		{
    			return true;
    		}
    	}
    	return false;
    }
    private BaseEntity filterSet(Set<BaseEntity> set, float[] thresholds, filterType[] filters){
    	BaseEntity target = null;
    	if (contains(filters, filterType.HEALTH) && contains(filters, filterType.DISTANCE) )
    	{
    		target = applyHPDistFilter(set, BaseNPC.class, thresholds);
    		if (target == null)
    			target = applyHPDistFilter(set, Hero.class, thresholds);
    		if (target == null)
    			target = applyHPDistFilter(set, Tower.class, thresholds);
    	}
    	else if (contains(filters, filterType.HEALTH) )
		{
    		
    		target = applyHealthFilter(set, BaseNPC.class, thresholds);
			if (target == null)
				target = applyHealthFilter(set, Hero.class, thresholds);
			if (target == null)
				target = applyHealthFilter(set, Tower.class, thresholds);
	    	}
			if (target != null)
				return target;

    	else if (contains(filters, filterType.DISTANCE))
    	{
        	target = set.stream()
        			.sorted( ( e1, e2 ) -> Float.compare( Vec3.distance( data.pos, e1.getOrigin() ), Vec3.distance( data.pos, e2.getOrigin() )) )
        			.filter( f -> Vec3.distance(f.getOrigin(), data.pos) < data.range)
    				.findFirst()
    				.orElse(null);
    	}    
		return target;
    	
    }
    public BaseEntity applyHealthFilter(Set<BaseEntity> set, @SuppressWarnings("rawtypes") Class type, float[] thresholds)
    {
    	BaseEntity target = null;
		target = set.stream()
    			.sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ))
    			.filter( f -> (float)f.getHealth() / (float)f.getMaxHealth() < thresholds[threshold((BaseEntity)f)])
    			.filter( f -> f.getClass() == type)
				.findFirst()
				.orElse(null);
		return target;
    }
    public BaseEntity applyHPDistFilter(Set<BaseEntity> set, @SuppressWarnings("rawtypes") Class type, float[] thresholds)
    {
    	BaseEntity target = null;
		target = set.stream()
    			.sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ))
    			.filter( f -> (float)f.getHealth() / (float)f.getMaxHealth() < thresholds[threshold((BaseEntity)f)])
    			.filter( f -> Vec3.distance(f.getOrigin(), data.pos) < data.range)
    			.filter( f -> f.getClass() == type)
				.findFirst()
				.orElse(null);
		return target;
    }
    public Set<BaseEntity> targetType(int flag)
    {
    	switch(flag)
    	{
    		case 0:
    			targets = data.enemyCreeps;
    			return targets;
    		case 1:
    			targets = data.enemyHeroes;
    			return targets;
    		case 2:
    			targets = data.enemyTurrets;
    			targets.addAll(data.friendlyTurrets);
    			return targets;
    		case 3:
    			targets = data.friendlyCreeps;
    			return targets;    			
    		case 4:
    			//all enemies
    			targets = data.enemyCreeps;
    			targets.addAll(data.enemyTurrets);
    			targets.addAll(data.enemyHeroes);
    			return targets;
    		case 5:
    			//all allies
    			targets = data.friendlyCreeps;
    			targets.addAll(data.friendlyTurrets);
    			targets.addAll(data.friendlyHeroes);
    			return targets;
    		case 6:
    			//Farming targets:
    			targets = data.enemyCreeps;
    			targets.addAll(data.friendlyCreeps);
    			targets.addAll(data.enemyTurrets);
    			targets.addAll(data.friendlyTurrets);
    			return targets;
    		case 7:
    			//brawl targets:
    			targets = data.enemyCreeps;
    			targets.addAll(data.enemyHeroes);
    			targets.addAll(data.friendlyCreeps);
    			targets.addAll(data.enemyTurrets);
    			targets.addAll(data.friendlyTurrets);
    			return targets;
    		case 8:
    			//Push targets:
    			targets = data.enemyCreeps;
    			targets.addAll(data.enemyTurrets);
    			return targets;
    		default:
    			targets = null;
    			return targets;
    	}
    }
    public Command retreat( World world ) 
    {
        //Retreat at 30% health
//        System.out.println( "Lina is retreating" );
        final BaseNPC fountain = (BaseNPC) world.getEntities().entrySet().stream().filter( p -> p.getValue().getName().equals("ent_dota_fountain_good") )
                        .findAny().get().getValue();
        final float[] targetPos = fountain.getOrigin();
        return goTo(targetPos);
        
    }
    
    private Command goTo(float[] pos)
    {
    	//System.out.println(MOVE + " " + pos);
    	MOVE.setX(pos[0]);
    	MOVE.setY(pos[1]);
    	MOVE.setZ(0);
    	return MOVE;
    }

   public Command buy( Hero agent, World world, String item) {
       float[] shopPos = data.shopLocations[shop.getShopIndex(item)];
       float dist = Vec3.distance(shopPos, agent.getOrigin());
       if (dist < 200)
       {
           System.out.println(item);
           buildIndex++;
           BUY.setItem( item );
           data.inventory.add(item);
           if(agent.getGold() < itemCosts[buildIndex])
           {
        	   BUYING = false;
           }
           return BUY;
       }

	  return goTo(shopPos);
   }
   
   public int determineShopType(String item)
   {
	   
	   int shopIndex = 0;
	   
	   
	   
	   return shopIndex;
   }
   public Command sell( int slot ) 
   {
       SELL.setSlot( slot );
       
       return SELL;
   }
/*
    private static float distance( BaseEntity a, BaseEntity b )
    {
        final float[] posA = a.getOrigin();
        final float[] posB = b.getOrigin();
        return Vec3.distance( posA, posB );
    }
*/

}

