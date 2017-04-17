package de.lighti.dota2.bot;

import java.util.Random;
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

    private static final String MY_HERO_NAME = "npc_dota_hero_drow_ranger";

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

    public Agent() {
        System.out.println( "Creating agent" );
        myLevels = new int[5];

    }

    @Override
    public LevelUp levelUp() {
        LEVELUP.setAbilityIndex( -1 );
        Random rand = new Random();
        int level = rand.nextInt(3);
        while (myLevels[level] >= 4){
        	level = rand.nextInt(3);
        }
      /*  if (myLevels[0] < 4) {
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
        }*/
        if (myLevels[level] < 4)
        {
        	LEVELUP.setAbilityIndex(level);
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
            case "agent go":
                mode = Mode.ENABLED;
                break;
            case "agent stop":
                shouldRetreat = true;
                mode = Mode.DISABLED;
                break;
            case "agent sell tango":
                shouldSellTango = true;
                break;
            case "agent buy tango":
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
        if (mode == Mode.DISABLED) {
            if (shouldRetreat) {
                shouldRetreat = false;
                return retreat( world );
            }

            return NOOP;
        }

//        System.out.println( world.getEntities().size() + " present" );
//        world.getEntities().values().stream().filter( e -> e.getClass() == Building.class ).forEach( e -> System.out.println( e ) );
//        world.getEntities().values().stream().filter( e -> e.getClass() == Tower.class ).forEach( e -> System.out.println( e ) );
        final int myIndex = world.searchIndexByName( MY_HERO_NAME );
        if (myIndex < 0) {
            //I'm probably dead
            System.out.println( "I'm dead?" );
            reset();
            return NOOP;
        }

        final Hero agent = (Hero) world.getEntities().get( myIndex );
//        for (final Ability a : agent.getAbilities().values()) {
//            myLevels[a.getAbilityIndex()] = a.getLevel();
//            System.out.println( a );
//        }


        final float range = agent.getAttackRange();
        final Set<BaseEntity> e = findEntitiesInRange( world, agent, range ).stream().filter( p -> p instanceof BaseNPC )
                        .filter( p -> ((BaseNPC) p).getTeam() == 3 ).collect( Collectors.toSet() );
        if (agent.getHealth() <= agent.getMaxHealth() * 0.4) 
        {
        	return retreat( world );
        }
        if (!e.isEmpty()) {
            return attack( agent, e, world );
        }
        else {
            return move( agent, world );
        }
    }

    private Command attack( Hero agent, Set<BaseEntity> e, World world ) 
    {
       /* final BaseEntity target = e.stream().sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ) )
                        .filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() ).findFirst().orElse( null );
        */
    	 final BaseEntity target = e.stream().sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ) )
                 .filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() || ((BaseNPC) f).getHealth() <  agent.getHealth() ).findFirst().orElse( null );
         
    	 if (target == null) 
    	 {
            //Nothing in range
            System.out.println( "No enemy in range" );
            return NOOP;
        }
		double percentHealth = (double)target.getHealth() / (double)target.getMaxHealth();
    	if (distance(agent, target) + 100 < agent.getAttackRange()) 
        {
             return retreat( world );
        }
        //If agent has enough mana, there's a 30 % chance that she'll cast a spell
        if (target != null && agent.getMana() > agent.getMaxMana() * 0.5 && Math.random() > 0.3) {
        
	       

            return castSpell( agent, target, world );
        }
        else {
            //Otherwise she just attacks
            final int targetindex = world.indexOf( target );
            ATTACK.setTarget( targetindex );
         //   System.out.println( "Attacking" );

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

    private Command castSpell( Hero agent, BaseEntity target, World world ) {
        final Random r = new Random();
        final int index = r.nextInt( 4 );
        final Ability a = agent.getAbilities().get( index );
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

    private Command move( Hero agent, World world ) {
        //Walk up to the nearest enemy
        final Set<BaseEntity> en = findEntitiesInRange( world, agent, Float.POSITIVE_INFINITY ).stream().filter( p -> p instanceof BaseNPC )
                        .filter( p -> ((BaseNPC) p).getTeam() == 3 ).collect( Collectors.toSet() );
        final BaseEntity target = en.stream().sorted( ( e1, e2 ) -> Float.compare( distance( agent, e1 ), distance( agent, e2 ) ) )
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

    private Command retreat( World world ) {
        //Retreat at 30% health
       // System.out.println( "agent is retreating" );
        final BaseNPC fountain = (BaseNPC) world.getEntities().entrySet().stream().filter( p -> p.getValue().getName().equals( "ent_dota_fountain_good" ) )
                        .findAny().get().getValue();
        final float[] targetPos = fountain.getOrigin();
        MOVE.setX( targetPos[0] );
        MOVE.setY( targetPos[1] );
        MOVE.setZ( targetPos[2] );

        return MOVE;
    }
}
