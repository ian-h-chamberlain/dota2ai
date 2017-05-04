package de.lighti.dota2.bot;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import se.lu.lucs.dota2.framework.game.BaseEntity;
import se.lu.lucs.dota2.framework.game.BaseNPC;
import se.lu.lucs.dota2.framework.game.Hero;
import se.lu.lucs.dota2.framework.game.Tower;

public class Action 
{

	public Action()
	{
		
		
	}
	public BaseEntity getTarget(Hero agent, Set<BaseEntity> e, int selectionType)
	{
		//Select an target based on selection algorithm
		/* 0: Lowest Health Enemy Creep in Range
		 * 1: Closest Enemy Creep in Range
		 * 2: Lowest Health Ally Creep in Range
		 * 3: Closest Ally Creep in Range
		 * 4: Lowest Health Enemy Hero
		 * 5: Closest Enemy Hero 
		 * 6: Nearest Tower
		 * 7: Lowest Health Tower
		 */
		BaseEntity target = null;
		switch (selectionType)
		{
			//Enemy with lowest health
			case 0:
		        target = e.stream()
        					.sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ))
			                .filter( f -> f.getClass() != Tower.class )
			                .filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() )
			                .findFirst()
			                .orElse( null );
		        break;
		    //Nearest enemy
			case 1:
		        target = e.stream()
		        			.sorted( ( e1, e2 ) -> Float.compare( distance( agent, e1 ), distance( agent, e2 )) )
			                .filter( f -> f.getClass() != Tower.class )
			                .filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() )
			                .findFirst()
			                .orElse( null );
		        break;
		    //Ally creep with lowest health
			case 2:
		        target = e.stream()
		        			.sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ) )
			                .filter( f -> f.getClass() != Tower.class )
			                .filter( f -> f.getClass() != Hero.class )
			                .filter( f -> ((BaseNPC) f).getTeam() == agent.getTeam() )
			                .findFirst()
			                .orElse( null );
		        break;
		    //Closest health enemy hero
			case 3:
		        target = e.stream()
					        .sorted( ( e1, e2 ) -> Float.compare( distance( agent, e1 ), distance( agent, e2 ) ))
					        .filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() )
			                .filter( f -> f.getClass() == Hero.class )
			                .findFirst()
			                .orElse( null );
		        break;
		    //Lowest health enemy hero
			case 4:
		        target = e.stream()
		        			.sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ))
		        			.filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() )
		        			.filter( f -> f.getClass() == Hero.class )
		        			.findFirst()
		        			.orElse( null );
		        break;
		    //Nearest enemy hero
			case 5:
		        target = e.stream()
		        			.sorted( ( e1, e2 ) -> Float.compare( distance( agent, e1 ), distance( agent, e2 ) ) )
		        			.filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() )
		        			.filter( f -> f.getClass() == Hero.class )
		        			.findFirst()
		        			.orElse( null );
		        break;
		    //Closest enemy tower
			case 6:
		        target = e.stream()
					        .sorted( ( e1, e2 ) -> Float.compare( distance( agent, e1 ), distance( agent, e2 ) ) )
		        			.filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() )
			                .filter( f -> f.getClass() == Tower.class )
			                .findFirst()
			                .orElse( null );
		        break;
		    //Lowest health tower
			case 7:
		        target = e.stream()
		        			.sorted( ( e1, e2 ) -> Integer.compare( ((BaseNPC) e1).getHealth(), ((BaseNPC) e2).getHealth() ) )
		        			.filter( f -> ((BaseNPC) f).getTeam() != agent.getTeam() )
			                .filter( f -> f.getClass() == Tower.class )
			                .findFirst()
			                .orElse( null );
		        break;
			default:
				break;
		}
		return target;
	}
	
    private static float distance( BaseEntity a, BaseEntity b )
    {
        final float[] posA = a.getOrigin();
        final float[] posB = b.getOrigin();
        return distance( posA, posB );
    }
    private static float distance( float[] posA, float[] posB ) 
    {
        return (float) Math.hypot( posB[0] - posA[0], posB[1] - posA[1] );
    }



}

