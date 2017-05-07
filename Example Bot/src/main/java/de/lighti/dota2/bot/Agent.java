package de.lighti.dota2.bot;

import java.util.Random;
import java.lang.System;
import java.util.Set;
import java.util.stream.Collectors;

import se.lu.lucs.dota2.framework.bot.BaseBot;
import se.lu.lucs.dota2.framework.bot.BotCommands.LevelUp;
import se.lu.lucs.dota2.framework.bot.BotCommands.Select;
import se.lu.lucs.dota2.framework.game.Ability;
import se.lu.lucs.dota2.framework.game.BaseEntity;
import se.lu.lucs.dota2.framework.game.BaseNPC;
import se.lu.lucs.dota2.framework.game.ChatEvent;
import se.lu.lucs.dota2.framework.game.Hero;
import se.lu.lucs.dota2.framework.game.Tower;
import se.lu.lucs.dota2.framework.game.World;

public class Agent extends BaseBot {
    private enum Mode {
        ENABLED, DISABLED
    }

    private static final String MY_HERO_NAME = "npc_dota_hero_sniper";

    private static float distance( BaseEntity a, BaseEntity b ) {
        final float[] posA = a.getOrigin();
        final float[] posB = b.getOrigin();
        return distance( posA, posB );
    }

    private static float distance( float[] posA, float[] posB ) {
        return (float) Math.hypot( posB[0] - posA[0], posB[1] - posA[1] );
    }

    private static Set<BaseEntity> findEntitiesInRange( World world, BaseEntity center, float range ) {
        final Set<BaseEntity> result = world.getEntities().values().stream().filter( e -> distance( center, e ) < range ).collect( Collectors.toSet() );
        result.remove( center );
        return result;
    }

    private int[] myLevels;

    private Mode mode = Mode.ENABLED;
    private boolean shouldRetreat;
    private boolean shouldBuyTango;
    private boolean shouldSellTango;
    private NeuralNetwork nn;
    private AgentData gameData;
    private UtilityScorer scorer;
    private static final long attackAnimDelay = 170;
    private static long attackDelay = 1300;
    private long lastTime = 0;
    
    public Agent() {
        System.out.println( "Creating Agent" );
        myLevels = new int[5];
//<<<<<<< HEAD
       // nn = new NeuralNetwork(5 /*TODO number of actions */);
       // nn.testQ(1);
        
//=======

       // gameData = new AgentData();

        nn = new NeuralNetwork(gameData.stateSize, 3);
//>>>>>>> 6de233d2f3e865c593fa0e1b22cc5b0d73d21d17
    }
    public void train(Hero agent, World world)
    {
    	
    	float[] data = gameData.parseGameState(agent, world);
    	//set inputs of neural network
    	nn.setInputs(data);
        
        
    }
    
    @Override
    public LevelUp levelUp() {
        LEVELUP.setAbilityIndex( -1 );
        if(null != LEVELUP){
        	System.out.println("hi");
        }
        if (myLevels[0] < 4) {
            LEVELUP.setAbilityIndex( 0 );
        }
        else if (myLevels[1] < 4) {
            LEVELUP.setAbilityIndex( 1 );
        }
        else if (myLevels[2] < 4) {
            LEVELUP.setAbilityIndex( 2 );
        }
        else if (myLevels[3] < 3) {
            LEVELUP.setAbilityIndex( 3 );
        }
        else if (myLevels[4] < 10) {
            LEVELUP.setAbilityIndex( 4 );
        }
        //System.out.println( "LevelUp " + LEVELUP.getAbilityIndex() );
        return LEVELUP;
    }

    @Override
    public void onChat( ChatEvent e ) {
        switch (e.getText()) {
            case "lina go":
                mode = Mode.ENABLED;
                break;
            case "lina stop":
                shouldRetreat = true;
                mode = Mode.DISABLED;
                break;
            case "lina sell tango":
                shouldSellTango = true;
                break;
            case "lina buy tango":
                shouldBuyTango = true;
                break;
        }
    }

    @Override
    public void reset() {
        System.out.println( "Resetting" );
        myLevels = new int[5];
    }

    @Override
    public Select select() {
        SELECT.setHero( MY_HERO_NAME );
        return SELECT;
    }

    @Override
    public Command update( World world ) {
//        System.out.println( "I see " + world.searchIndexByClass( Tree.class ).size() + " trees" );

        if (shouldBuyTango) {
            shouldBuyTango = false;
            return buy( "item_tango" );
        }
        if (shouldSellTango) {
            shouldSellTango = false;
            return sell(0 );
        }
        /*
        if (mode == Mode.DISABLED) {
            if (shouldRetreat) {
                shouldRetreat = false;
                return retreat( world );
            }

            return NOOP;
        }*/

//        System.out.println( world.getEntities().size() + " present" );
//        world.getEntities().values().stream().filter( e -> e.getClass() == Building.class ).forEach( e -> System.out.println( e ) );
//        world.getEntities().values().stream().filter( e -> e.getClass() == Tower.class ).forEach( e -> System.out.println( e ) );
        final int myIndex = world.searchIndexByName( MY_HERO_NAME );
        if (myIndex < 0) {
            //I'm probably dead
            //System.out.println( "I'm dead?" );
            reset();
            return NOOP;
        }

        final Hero agent = (Hero) world.getEntities().get( myIndex );
        if(gameData == null){
        	//put the gamedata and score initializer here so it can access the agent. 
        	gameData = new AgentData(agent);
        	scorer = new UtilityScorer(gameData);
        }
//        for (final Ability a : lina.getAbilities().values()) {
//            myLevels[a.getAbilityIndex()] = a.getLevel();
//            System.out.println( a );
//        }
        gameData.populate(agent, world);
        
        /*
        if (agent.getHealth() <= agent.getMaxHealth() * 0.4) {
            return retreat( world );
        }*/

        final float range = agent.getAttackRange();/*
        final Set<BaseEntity> e = findEntitiesInRange( world, agent, range ).stream().filter( p -> p instanceof BaseNPC )
                        .filter( p -> ((BaseNPC) p).getTeam() == 3 ).collect( Collectors.toSet() );
        */
        //Train agent on update
        train(agent, world);
        
        long t = System.currentTimeMillis();
        if(t - lastTime < attackAnimDelay){
        	System.out.println("anim. " + t + ", " + lastTime + ", " + attackAnimDelay);
        	BaseEntity e = AgentData.getNearest(gameData.enemyCreeps, gameData.pos);
            if (e != null) {
            	System.out.println("attack");
                return attack( agent, e, world );
            }else {
            	e = AgentData.getNearest(gameData.enemyHeroes, gameData.pos);
            	if(e != null){
            		System.out.println("attack");
            		return attack(agent, e, world);
            	}else{
            		e = AgentData.getNearest(gameData.enemyTurrets, gameData.pos);
            		if(e != null){
            			System.out.println("attack");
            			return attack(agent,e,world);
            		}
            		
            	}
            }
        }
        if(t - lastTime > attackDelay){
        	System.out.println("attacking " + (t-lastTime));
        	lastTime = t;
        	BaseEntity e = AgentData.getNearest(gameData.enemyCreeps, gameData.pos);
            if (e != null) {
            	System.out.println("attack");
                return attack( agent, e, world );
            }else {
            	e =  AgentData.getNearest(gameData.enemyCreeps, gameData.pos);
            	if(e != null){
            		System.out.println("attack");
            		return attack(agent, e, world);
            	}else{
            		e = AgentData.getNearest(gameData.enemyCreeps, gameData.pos);
            		if(e != null){
            			System.out.println("attack");
            			return attack(agent,e,world);
            		}
            		
            	}
            }
        }
        System.out.println("goto");
        return goTo (scorer.getPoint(gameData.pos));//( agent, world );
        //if()
        /*
        Set<BaseEntity> e = gameData.enemyHeroes;
        if (!e.isEmpty()) {
            return attack( agent, e, world );
        }else {
        	e = gameData.enemyTurrets;
        	if(!e.isEmpty()){
        		return attack(agent, e, world);
        	}else{
        		e = gameData.enemyTurrets;
        		if(!e.isEmpty()){
        			return attack(agent,e,world);
        		}
        		
        	}
        }*/
    }
    
    
    
    private Command goTo(float[] pos){
    	//System.out.println(MOVE + " " + pos);
    	MOVE.setX(pos[0]);
    	MOVE.setY(pos[1]);
    	MOVE.setZ(0);
    	return MOVE;
    }

    private Command attack( Hero lina, BaseEntity target, World world ) 
    {
        //final BaseEntity target = e.stream().sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ) )
          //              .filter( f -> ((BaseNPC) f).getTeam() != lina.getTeam() ).findFirst().orElse( null );
        if (target == null) {
            //Nothing in range
            System.out.println( "No enemy in range" );
            return NOOP;
        }

        //If lina has enough mana, there's a 30 % chance that she'll cast a spell
        if (lina.getMana() > lina.getMaxMana() * 0.5 && Math.random() > 0.3) {
            return castSpell( lina, target, world );
        }
        else {
            //Otherwise she just attacks
            final int targetindex = world.indexOf( target );
            ATTACK.setTarget( targetindex );
            System.out.println( "Attacking" );

            return ATTACK;
        }
    }

    private Command buy( String item ) {
        BUY.setItem( item );

        return BUY;
    }
    
    private Command sell( int slot ) {
        SELL.setSlot( 0 );

        return SELL;
    }

    private Command castSpell( Hero lina, BaseEntity target, World world ) {
        final Random r = new Random();
        final int index = r.nextInt( 4 );
        final Ability a = lina.getAbilities().get( index );
        if (a.getAbilityDamageType() == Ability.DOTA_ABILITY_BEHAVIOR_POINT) {
            return NOOP;
        }
        System.out.println( "will cast a spell" );
        System.out.println( "Will try " + a.getName() );
        if (a.getLevel() < 1) {
            System.out.println( "Not learned yet" );
            return NOOP;
        }
        if (a.getCooldownTimeRemaining() > 0f) {
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
/*
    private Command move( Hero lina, World world ) {
        //Walk up to the nearest enemy
        final Set<BaseEntity> en = findEntitiesInRange( world, lina, Float.POSITIVE_INFINITY ).stream().filter( p -> p instanceof BaseNPC )
                        .filter( p -> ((BaseNPC) p).getTeam() == 3 ).collect( Collectors.toSet() );
        final BaseEntity target = en.stream().sorted( ( e1, e2 ) -> Float.compare( distance( lina, e1 ), distance( lina, e2 ) ) )
                        .filter( f -> f.getClass() != Tower.class ).findFirst().orElse( null );
        if (target == null)
        {
            //Nothing in range
            System.out.println( "No enemy in sight" );
            return NOOP;
        }
        final BaseNPC targetEntity = (BaseNPC) target;
        final float[] targetPos = targetEntity.getOrigin();
        MOVE.setX( targetPos[0] );
        MOVE.setY( targetPos[1] );
        MOVE.setZ( targetPos[2] );

       // System.out.println( "Moving" );

        return MOVE;
    }
*/
    /*
    private Command retreat( World world ) {
        //Retreat at 30% health
        System.out.println( "Lina is retreating" );
        final BaseNPC fountain = (BaseNPC) world.getEntities().entrySet().stream().filter( p -> p.getValue().getName().equals( "ent_dota_fountain_good" ) )
                        .findAny().get().getValue();
        final float[] targetPos = fountain.getOrigin();
        MOVE.setX( targetPos[0] );
        MOVE.setY( targetPos[1] );
        MOVE.setZ( targetPos[2] );

        return MOVE;
    }*/
}
