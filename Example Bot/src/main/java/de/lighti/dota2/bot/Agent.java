package de.lighti.dota2.bot;

import java.util.Random;
import java.lang.System;
import java.util.Set;
import java.util.stream.Collectors;

import se.lu.lucs.dota2.framework.bot.BaseBot;
import se.lu.lucs.dota2.framework.bot.BotCommands.Attack;
import se.lu.lucs.dota2.framework.bot.BotCommands.Buy;
import se.lu.lucs.dota2.framework.bot.BotCommands.Cast;
import se.lu.lucs.dota2.framework.bot.BotCommands.LevelUp;
import se.lu.lucs.dota2.framework.bot.BotCommands.Move;
import se.lu.lucs.dota2.framework.bot.BotCommands.Noop;
import se.lu.lucs.dota2.framework.bot.BotCommands.Select;
import se.lu.lucs.dota2.framework.bot.BotCommands.Sell;
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
    private static final long attackAnimDelay = 200;
    private static long attackDelay = 1300;
    private long lastTime = 0;
    private static final boolean useTensor = false;
    Action actionController;
    public Agent() {
        System.out.println( "Creating Agent" );
        myLevels = new int[5];
        actionController = new Action(ATTACK, CAST, MOVE, NOOP, BUY, SELL, gameData);
        
        ///////NOTE: SOME INITIALIZATION IS DONE IN UPDATE BECAUSE OF DEPENDENCIES ON THE INGAME WORLD.
    }
    public void train(Hero agent, World world)
    {
    	if(nn == null){
    		return;
    	}
    	float[] data = gameData.parseGameState(agent, world);

        System.out.println("nn: " + nn);
    	//set inputs of neural network
    	nn.setInputs(data);
        
        
    }
    
    @Override
    public LevelUp levelUp() {
        LEVELUP.setAbilityIndex( -1 );
        if(null != LEVELUP){
        	//System.out.println("hi");
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
    public void reset()
    {
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
    	/*
        if (shouldBuyTango) {
            shouldBuyTango = false;
            return buy( "item_tango" );
        }
        if (shouldSellTango) {
            shouldSellTango = false;
            return sell(0 );
        }
        
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
        if(gameData == null)
        {
        	//put the gamedata and score initializer here so it can access the agent. 
        	gameData = new AgentData(agent);
        	scorer = new UtilityScorer(gameData);
        	if(useTensor){
        		nn = new NeuralNetwork(gameData.stateSize, 3);
        	}
            System.out.println("nn: " + nn);
        }
        
        gameData.populate(agent, world);
        if(!useTensor){
        	gameData.parseGameState(agent, world);
        }

        //Train agent on update
        if(useTensor){
        	train(agent, world);
        }
        return actionController.update(agent, world, scorer, lastTime, attackAnimDelay, attackDelay, gameData);
    }

}
