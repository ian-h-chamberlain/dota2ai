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
    
    private static final int[] levels = {0,2,1,0,0,3,0,2,2,4,3,2,1,1,6,3,1,9,11};//The order for 
    int levelIndex = 0;

    private int[] myLevels;
    Hero agent;
    private Mode mode = Mode.ENABLED;
    private NeuralNetwork nn;
    private AgentData gameData;
    Random r = new Random();
    private UtilityScorer scorer;
    Action actionController;

    private static final boolean useTensor = true;
    int lastAction = -1;
    float lastReward;
    OutputProcess networkProcessor;
    
    private float[] lastData;
    
    public Agent() {
       // System.out.println( "Creating Agent" );
        myLevels = new int[5];
        //if(gameData == null){
        //	System.out.println("Building new game data");
        	//put the gamedata and score initializer here so it can access the agent. 
        gameData = new AgentData();
        actionController = new Action(ATTACK, CAST, MOVE, NOOP, BUY, SELL, gameData);
        scorer = new UtilityScorer(gameData);
        networkProcessor = new OutputProcess();
        if(useTensor){
        	System.out.println("creating neural network");
        	nn = new NeuralNetwork(gameData.stateSize, networkProcessor.size());
        }
        //System.out.println("nn: " + nn);
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
			nn.propagateReward(new int[]{lastAction}, lastReward, data);
		}
    	
        //set inputs of neural network and get new q-values
        
    	float[] outputs = nn.getQ(data);
    	
    	System.out.println(outputs[0]);
    	int[] chosenAction;
    	if (r.nextFloat() < nn.epsilon) {
    		chosenAction = this.networkProcessor.pickRandom();
    	}
    	else {
    		chosenAction = this.networkProcessor.runNumbers(outputs);
    		//lastAction = nn.getAction(data);
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
    	///Level = (Hero) world.getEntities().get( myIndex );
    	if(agent == null){
    		return null;
    	}
    	LEVELUP.setAbilityIndex(levels[levelIndex]);
    	if(levelIndex < agent.getLevel()){
    		levelIndex++;
    	}
    	return LEVELUP;
    }

    @Override
    public void onChat( ChatEvent e ) {
        switch (e.getText()) {
            case "lina go":
                mode = Mode.ENABLED;
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

    	final int myIndex = world.searchIndexByName( MY_HERO_NAME );
        if (myIndex < 0) {
            //I'm probably dead
            //System.out.println( "I'm dead?" );
            reset();
            return NOOP;
        }

        agent = (Hero) world.getEntities().get( myIndex );
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
    
    
    
    
    
    public interface Output{
    	void Run();
    }
    
    public class OutputProcess{
    	
    	public int[] runNumbers(float[] inputs){
    		return runNumbers(inputs,false);
    	}
    	
    	public int[] runNumbers(float[] inputs, boolean dummyRun){
    		if(size() != inputs.length){
    			System.err.println("Warning: " + size() + " does not equal " + inputs.length);
    		}
    		int inputIndex = 0;
    		int[] ret = new int[outputs.length];
    		for(int i = 0; i < outputs.length; i++){
    			int maxIndex = -1;
    			float maxVal = Float.MIN_VALUE;
    			for(int j = 0; j < outputs[i].length; i++){
    				if(inputs[inputIndex] > maxVal){
    					maxIndex = j;
    					maxVal = inputs[inputIndex];
    				}
    				
    				inputIndex++;
    			}
    			ret[i] = maxIndex;
    			if(!dummyRun){
        			outputs[i][maxIndex].Run();
    			}
    		}
    		return ret;
    	}
    	
    	public int[] pickRandom(){
    		return pickRandom(false);
    	}
    	
    	public int[] pickRandom(boolean dummyRun){
    		int[] ret = new int[outputs.length];
    		for(int i = 0; i < outputs.length;i++){
    			ret[i] = r.nextInt(outputs[i].length);
    			outputs[i][ret[i]].Run();
    		}
    		return ret;
    	}
    	
    	public int size(){
    		int s = 0; 
    		for(int i = 0; i < outputs.length; i++){
    			s += outputs[i].length;
    		}
    		return s;
    	}
    	
    	public Output[][] outputs = new Output[][]{
    		new Output[]{
    				
    			//This is the UtilityScorerer output array.
    			new Output(){
					@Override
					public void Run() {
						scorer.currentMode = UtilityScorer.backOff;
					}
				},new Output(){
					@Override
					public void Run() {
						scorer.currentMode = UtilityScorer.brawl;
					}
				},new Output(){
					@Override
					public void Run() {
						scorer.currentMode = UtilityScorer.farm;
					}
				},new Output(){
					@Override
					public void Run() {
						scorer.currentMode = UtilityScorer.gank;
					}
				},new Output(){
					@Override
					public void Run() {
						scorer.currentMode = UtilityScorer.guard;
					}
				},new Output(){
					@Override
					public void Run() {
						scorer.currentMode = UtilityScorer.laneSwitch;
					}
				},new Output(){
					@Override
					public void Run() {
						scorer.currentMode = UtilityScorer.siege;
					}
				}
    		},
    		
    		new Output[]{///TARGET SELECTION
    			new Output(){
    				public void Run() {
						actionController.mode = Action.selectionType.enemyCreeps;
					}
    			},new Output(){
    				public void Run() {
						actionController.mode = Action.selectionType.enemyHeroes;
					}
    			},new Output(){
    				public void Run() {
						actionController.mode = Action.selectionType.enemyTurrets;
					}
    			},new Output(){
    				public void Run() {
						actionController.mode = Action.selectionType.allyCreeps;
					}
    			}
    		}
    	};
    }
    
}
