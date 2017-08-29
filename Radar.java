/* Copyright (c) 2017 Ralph Saade CNAM-LIBAN */

import java.util.*;

public class Radar 
{

  public static void main(String[] args) 
  {
		
    ArrayList<Avion> avionsList = new ArrayList<Avion>();
    HashMap<String,Avion> avions = new HashMap<String,Avion>();
    //start saca
    Saca saca = new Saca(avions, avionsList);
    saca.start();
    // init planes
    for(int i = 0; i<2; i++)
    {
      avionsList.add(new Avion());
      avions.put(avionsList.get(i).getNumVol(), avionsList.get(i));
    }
    // move planes and send data to SACA	
    try 
    {			
      while(true)
      {
	for(int i = 0; i<avionsList.size(); i++)
	  avionsList.get(i).se_deplacer();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
