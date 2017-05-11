package de.lighti.dota2.bot;

import java.util.ArrayList;

public class Shop {
	public int[] secretCosts;
	public ArrayList<String> secretItems;
	public Shop()
	{
		secretItems = new ArrayList<String>();
		String[] secrets = new String[]
				{
						"item_ultimate_orb", 
						"item_point_booster", 
						"item_vitality_booster", 
						"item_energy_booster", 
						"item_platemail", 
						"item_talisman_of_evasion",
						"item_hyperstone",
						"item_demon_edge",
						"item_mystic_staff",
						"item_reaver",
						"item_eagle",
						"item_relic",
						"item_soul_booster"
				};
		
		for (int i = 0; i < secrets.length; i++)
		{
			secretItems.add(secrets[i]);
		}
		
		secretCosts = new int[]
				{
						2150,
						1200,
						1100,
						900,
						1400,
						1450,
						2000,
						2400,
						2700,
						3000,
						3200,
						3800
				};
		
		 //fusion of the other boosters

		
	}
	
	public int getShopIndex(String item)
	{
		int index = 0;
		if (secretItems.contains(item))
		{
			index = 2;
		}
		
		return index;
	}
	
	
}
