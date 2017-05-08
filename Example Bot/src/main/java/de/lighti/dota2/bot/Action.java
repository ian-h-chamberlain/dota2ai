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
import se.lu.lucs.dota2.framework.game.World;

public class Action 
{
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
    
    private long animCooldown = 0;
    private long attackSpeedCooldown = 0; 
    //private long lastTime = 0;
    Random rand = new Random();
    public selectionType mode = selectionType.farmTargets;
    public enum selectionType
    {
    	enemyCreeps, enemyHeroes, enemyTurrets, allyCreeps, allEnemies, allAllies, farmTargets, Brawl
    }
	public enum filterType
	{
		HEALTH, DISTANCE, LEVEL
	}
	public Action(Attack a, Cast c, Move m, Noop n, Buy b, Sell s, AgentData d)
	{
		ATTACK = a;
		CAST = c;
		MOVE = m;
		NOOP = n;
		BUY = b;
		SELL = s;
		data = d;		
	}
	
	public void waitForMS(long ms){
		animCooldown = Math.max(System.currentTimeMillis() + ms, animCooldown);
	}
	
	public Command update(Hero agent, World world, UtilityScorer scorer){
		BaseEntity e = null;
        long t = System.currentTimeMillis();
        Command out = NOOP;
        BaseEntity closestHero = targetFilter(agent, selectionType.enemyHeroes.ordinal(), filterType.DISTANCE.ordinal());
        if (closestHero != null && distance(agent, closestHero) < agent.getAttackRange())
        {
        	int abilityIndex = 0;
        	if (agent.getMana() > agent.getMaxMana() * 0.5 &&
        			agent.getAbilities().get(abilityIndex).getCooldownTimeRemaining() <= 0)
        	{
            	System.out.println("dist to closest hero" + distance(agent, closestHero));
        		return castSpell(agent, closestHero, world, 0);
        	}
        	else {
        		return attack(agent, world, closestHero); 
        	}

        }
        //targetFilter(agent, 0, filterType.HEALTH.ordinal());
        // 					  ^-- determined by utility scorer mode.
       
        if(t < animCooldown){//if we're animating do nothing so we don't cancel.
        	return NOOP;
        }
        if(t > attackSpeedCooldown){
        	//System.out.println("attacking.");
        	attackSpeedCooldown = t + attackDelay;
        	e = targetFilter(agent, mode.ordinal(), filterType.HEALTH.ordinal());
        	
            if (e != null) {
                if (e.getClass() == Hero.class ){
                	System.out.println("entity is " + e.getClass());
                }
            	return setAction(agent, world, e, 0);
            }else {
            	e = targetFilter(agent, 1, filterType.HEALTH.ordinal());
            	if(e != null){
            		return attack(agent, world, e);
            	}else{
            		e = targetFilter(agent, 2, filterType.HEALTH.ordinal());;
            		if(e != null){
            			return attack(agent, world, e);
            		}
            		
            	}
            }
        }
        /*
        if(t - lastTime < attackAnimDelay)
        {
        	e = targetFilter(agent, selectionType.farmTargets.ordinal(), filterType.HEALTH.ordinal());
        	
            if (e != null) {
                if (e.getClass() == Hero.class ){
                	System.out.println("entity is " + e.getClass());
                }
            	return setAction(agent, world, e, 0);
            }else {
            	e = targetFilter(agent, 1, filterType.HEALTH.ordinal());
            	if(e != null){
            		return attack(agent, world, e);
            	}else{
            		e = targetFilter(agent, 2, filterType.HEALTH.ordinal());;
            		if(e != null){
            			return attack(agent, world, e);
            		}
            		
            	}
            }
        }
        if(t - lastTime > attackDelay)
        {
        	lastTime = t;
        	e = targetFilter(agent, selectionType.farmTargets.ordinal(), filterType.HEALTH.ordinal());
            if (e != null)
            {
            	return attack(agent, world, e);
            }
            else {
            	e =  targetFilter(agent, 1, filterType.HEALTH.ordinal());
            	if(e != null){
            		return attack(agent, world, e);
            	}else{
            		e = targetFilter(agent, 2, filterType.HEALTH.ordinal());
            		if(e != null){
            			return attack(agent, world, e);
            		}
            	}
            }
        }*/

       if (e == null)
       {
           out = goTo (scorer.getPoint(data.pos));
       }
       return out;
	}
	public Command setAction(Hero agent, World world, BaseEntity e, int mode)
	{
		//Modes -- Farming, Ganking, Team Fight?
		if (e != null)
		{
			return attack(agent, world, e);
		}
		else
		{
			return attack(agent, world, 0, mode);
		}
	}
	public Command attack( Hero agent, World world, BaseEntity e)
	{
        if (e == null) 
        {
            //Nothing in range
            System.out.println( "No enemy in range" );
            return NOOP;
        }
        final int targetindex = world.indexOf( e );
        ATTACK.setTarget( targetindex );

        this.waitForMS(attackAnimDelay);
        return ATTACK;
	}
    public Command attack( Hero agent, World world, int group, int flag) 
    {
    	BaseEntity target = null;
    	target = targetFilter(agent, group, flag);
    	
        if (target == null)
        {
            //Nothing in range
            System.out.println( "No enemy in range" );
            return NOOP;
        }
        final int targetindex = world.indexOf( target );
        ATTACK.setTarget( targetindex );
        this.waitForMS(attackAnimDelay);
        return ATTACK;
    }
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
            System.out.println( "Not learned yet" );
            return NOOP;
        }
        if (a.getCooldownTimeRemaining() > 0f) 
        {
            System.out.println( "On cooldown" );
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
    			//Farming targets:
    			targets = data.enemyCreeps;
    			targets.addAll(data.enemyHeroes);
    			targets.addAll(data.friendlyCreeps);
    			targets.addAll(data.enemyTurrets);
    			targets.addAll(data.friendlyTurrets);
    			return targets;
    		default:
    			targets = null;
    			return targets;
    	}
    }
	public BaseEntity targetFilter(Hero agent, int selectionGroup, int selectionType)
	{
		Set<BaseEntity> e = targetType(selectionGroup);
		//Select an target based on selection algorithm
		/* 0: Health
		 * 1: Distance
		 */
		BaseEntity target = null;
		/*for (BaseEntity h : data.friendlyHeroes) {
			h.
		}*/
		switch (selectionType)
		{
			//Filter set based on lowest health
			case 0:
		        target = e.stream()
        					.sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ))
			                .findFirst()
			                .orElse( null );
		        break;
		    //Based on distance
			case 1:
		        target = e.stream()
		        			.sorted( ( e1, e2 ) -> Float.compare( distance( agent, e1 ), distance( agent, e2 )) )
			                .findFirst()
			                .orElse( null );
		        break;
		    //level based for heroes
			case 2:
				target = e.stream()
							.sorted( (e1, e2) -> Integer.compare( ((Hero) e1).getLevel(), ((Hero) e2).getLevel()))
							.findFirst()
							.orElse(null);
			default:
				break;
		}
		return target;
	}
    public Command retreat( World world ) 
    {
        //Retreat at 30% health
        System.out.println( "Lina is retreating" );
        final BaseNPC fountain = (BaseNPC) world.getEntities().entrySet().stream().filter( p -> p.getValue().getName().equals("ent_dota_fountain_good") )
                        .findAny().get().getValue();
        final float[] targetPos = fountain.getOrigin();
        return goTo(targetPos);
        
    }
    private Command goTo(float[] pos){
    	//System.out.println(MOVE + " " + pos);
    	MOVE.setX(pos[0]);
    	MOVE.setY(pos[1]);
    	MOVE.setZ(0);
    	return MOVE;
    }

   public Command buy( String item ) {
       BUY.setItem( item );

       return BUY;
   }
   
   public Command sell( int slot ) {
       SELL.setSlot( 0 );

       return SELL;
   }

    private static float distance( BaseEntity a, BaseEntity b )
    {
        final float[] posA = a.getOrigin();
        final float[] posB = b.getOrigin();
        return Vec3.distance( posA, posB );
    }


}

