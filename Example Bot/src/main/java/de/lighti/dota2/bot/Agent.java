package de.lighti.dota2.bot;

import java.util.Arrays;
import java.util.Random;
import java.lang.System;
import java.util.Set;
import java.util.stream.Collectors;

import se.lu.lucs.dota2.framework.bot.BaseBot;
import se.lu.lucs.dota2.framework.bot.BotCommands.LevelUp;
import se.lu.lucs.dota2.framework.bot.BotCommands.Select;
import se.lu.lucs.dota2.framework.game.BaseEntity;
import se.lu.lucs.dota2.framework.game.ChatEvent;
import se.lu.lucs.dota2.framework.game.Hero;
import se.lu.lucs.dota2.framework.game.World;

public class Agent extends BaseBot {
    private enum Mode {
        ENABLED, DISABLED
    }

    private static final String MY_HERO_NAME = "npc_dota_hero_sniper";


    private int[] myLevels;

    private Mode mode = Mode.ENABLED;
    private NeuralNetwork nn;
    private AgentData gameData;

    private UtilityScorer scorer;
    Action actionController;

    private static final boolean useTensor = true;
    int lastAction = -1;
    float lastReward;
    
    private float[] lastData;
    
    public Agent() {
        System.out.println( "Creating Agent" );
        myLevels = new int[5];
        //if(gameData == null){
        //	System.out.println("Building new game data");
        	//put the gamedata and score initializer here so it can access the agent. 
        gameData = new AgentData();
        actionController = new Action(ATTACK, CAST, MOVE, NOOP, BUY, SELL, gameData);
        scorer = new UtilityScorer(gameData);
        if(useTensor){
        	System.out.println("creating neural network");
        	nn = new NeuralNetwork(gameData.stateSize, 2);
        }
        System.out.println("nn: " + nn);
        //}
        
        ///////NOTE: SOME INITIALIZATION IS DONE IN UPDATE BECAUSE OF DEPENDENCIES ON THE INGAME WORLD.


    }
    public void train(Hero agent, World world)
    {
    	float[] data = gameData.parseGameState(agent, world);
    	
    	System.out.print("data: [");
    	for (int i=0; i<data.length; i++)
    	{
    		System.out.print(data[i] + ",");
    	}
    	System.out.println("]");
    	
    	if (lastAction >= 0)
    	{
			System.out.println("Action, reward: " + lastAction + "," + lastReward );

			// update the network with the previous reward and new state
			nn.propagateReward(lastAction, lastReward, data);
		}
    	
        //set inputs of neural network and get new q-values
        
    	Random r = new Random();
    	if (r.nextFloat() < nn.epsilon) {
    		lastAction = r.nextInt(2);
    	}
    	else {
    		lastAction = nn.getAction(data);
    	}
    	
    	float[] curPos = new float[] {data[5], data[6]};
    	float[] lastPos;
    	if (lastData != null)
    		lastPos = new float[] {lastData[5], lastData[6]};
    	else
    		lastPos = curPos;
    	
    	float[] moved = Vec3.sub(curPos, lastPos);
    	
    	// reward motion in positive direction
    	lastReward = moved[0] + moved[1];
    	System.out.println(moved[0] + "," + moved[1]);
    	
    	// outputs[0] = retreat
    	if (lastAction == 0)
    	{
    		scorer.currentMode = UtilityScorer.backOff;
    	}
    	else {
    		scorer.currentMode = UtilityScorer.brawl;
       	}

    	lastData = data.clone();
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
        System.out.println( "LevelUp " + LEVELUP.getAbilityIndex() );
        return LEVELUP;
    }

    @Override
    public void onChat( ChatEvent e ) {
        switch (e.getText()) {
            case "lina go":
                mode = Mode.ENABLED;
                break;
/*            case "lina stop":
                shouldRetreat = true;
                mode = Mode.DISABLED;
                break;
            case "lina sell tango":
                shouldSellTango = true;
                break;
            case "lina buy tango":
                shouldBuyTango = true;
                break;*/
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
        	gameData = new AgentData();
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
        return actionController.update(agent, world, scorer);
    }

}
