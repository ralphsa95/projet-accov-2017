/* Copyright (c) 2017 Ralph Saade CNAM-LIBAN */

import java.io.*;
import java.net.*;

import static java.lang.System.exit;
import static java.lang.Math.*;
import static java.lang.Thread.*;

public class Avion 
{
	int maxVit = 1000;
	int maxAlt = 20000;
	int minVit = 200;
	int minAlt = 1;
	int pause = 2000;
	//
	private Coordonnees coord;
	private Deplacement dep;
	private String numVol;
	//
	String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	//
	Socket socket;
	PrintStream out;
	BufferedReader in;
	//
	String controlleur = "x";
	//
	public Avion()
	{
		numVol = buildNumVol();
		int x = (int) (1000 + Math.random() * 10 % 1000);
		int y = (int) (1000 + Math.random() * 10 % 1000);
		int a = (int) (1000 + Math.random() * 10 % 1000);
		//
		int v = (int) (600  + Math.random() * 10 % 200);
		int c = (int)  Math.random() * 10 % 360;
		//
		dep = new Deplacement(v,c);
		coord = new Coordonnees(x,y,a);
		ouvrir_communication();
	}
	//
	public synchronized void setControleur(String c)
	{
		controlleur = c;
	}
	//
	public void free()
	{
		controlleur = "x";
	}
	//
	public boolean isBusy()
	{
		return !controlleur.equals("x");
	}
	//
	public String getControlleur()
	{
		return controlleur;
	}
	//
	public String buildNumVol() //ex:ME352
	{
		StringBuilder sb = new StringBuilder(5);
		for(int i=0; i< 2; i++)
			sb.append(letters.charAt((int) Math.floor(Math.random() * letters.length())));
		
		for(int i=0; i< 3; i++)
			sb.append((int) Math.floor(Math.random() * 10));
		
		return sb.toString();		
	}
	//
	public String getNumVol()
	{
		return numVol;
	}
	//
	public Coordonnees getCoord()
	{
		return this.coord;
	}
	//
	public Deplacement getDep()
	{
		return this.dep;
	}
	//
	public void setDep(Deplacement d)
	{
		this.dep = d;
	}
	//
	public void setCoord(Coordonnees c)
	{
		this.coord = c;
	}
	//
	public boolean ouvrir_communication()
	{
		try {
			socket = new Socket ("localhost", 2400);
			out = new PrintStream(socket.getOutputStream());
			in =  new BufferedReader(new InputStreamReader(socket.getInputStream()));
			return true;
		} catch (Exception e) {
			 System.err.println("Exception: " + e.getMessage());
			return false;
		}
	}
	//
	public void fermer_communication()
	{
		try {
			socket.close();
		} catch(Exception e) {
			 System.err.println("Exception: " + e.getMessage());
		}
		
	}
	//
	public void envoyer_caracteristiques()
	{
		out.println(afficher_donnees());
	}
	//
	public void changer_vitesse(int v)
	{
		if(v < minVit)
			dep.setVitesse(minVit);
		else if(v > maxVit)
			dep.setVitesse(maxVit);
		else 
			dep.setVitesse(v);
	}
	//
	public void changer_cap(int c)
	{
		if ((c >= 0) && (c< 360))
			dep.setCap(c);
	}
	//
	public void  changer_altitude(int a)
	{
		if (a < 0)
			coord.setAlt(minAlt);
		else if (a > maxAlt)
			coord.setAlt(maxAlt);
		else 
			coord.setAlt(a);
	}
	//
	public String afficher_donnees()
	{
		StringBuilder s = new StringBuilder();
		s.append("Avion: " ).append(numVol).append(" | Localisation: (").append(coord.getX()).append(",").append(coord.getY());
		s.append(") | Altitude: ").append(coord.getAlt()).append(" | Vitesse: ").append(dep.getVitesse()).append(" | Cap: ").append(dep.getCap());
		return s.toString();
	}
	//
	public void calcul_deplacement()
	{
		float cosinus, sinus;
		float dep_x, dep_y;
		//
		if (dep.getVitesse() < minVit)
		{
			System.out.println("Vitesse trop faible : crash de l'avion \n");
			fermer_communication();
			exit(2);
		}
		if (coord.getAlt() == minAlt)
		{
			System.out.println("L'avion s'est ecrase au sol\n");
		    fermer_communication();
		    exit(3);
		}
	   
		//cos et sin ont un paramétre en radian, dep.cap en degré nos habitudes francophone
	    /* Angle en radian = pi * (angle en degré) / 180 
	       Angle en radian = pi * (angle en grade) / 200 
	       Angle en grade = 200 * (angle en degré) / 180 
	       Angle en grade = 200 * (angle en radian) / pi 
	       Angle en degré = 180 * (angle en radian) / pi 
	       Angle en degré = 180 * (angle en grade) / 200 
	     */
		 cosinus = (float) cos(dep.getCap() * 2 * Math.PI / 360);
		 sinus   = (float) sin(dep.getCap() * 2 * Math.PI / 360);
		 
		 //newPOS = oldPOS + Vt
	     dep_x = cosinus * dep.getVitesse() * 10 / minVit;
	     dep_y = sinus   * dep.getVitesse() * 10 / minVit;
	     
	     // on se deplace d'au moins une case quels que soient le cap et la vitesse
	     // sauf si cap est un des angles droit
	     
	     if ((dep_x > 0) && (dep_x < 1))  dep_x = 1;
	     if ((dep_x < 0) && (dep_x > -1)) dep_x = -1;

	     if ((dep_y > 0) && (dep_y < 1))  dep_y = 1;
	     if ((dep_y < 0) && (dep_y > -1)) dep_y = -1;

	     coord.setX(coord.getX() + (int) dep_x);
	     coord.setY(coord.getY() + (int) dep_y);

	    // afficher_donnees();	
	}
	//
	public void se_deplacer() throws InterruptedException
	{
		
		sleep(pause);
		calcul_deplacement();
		envoyer_caracteristiques();
	}
	//
/*	public static void main (String[]args)
	{
		try 
		{
			PrintStream out1;
			PrintStream out2;
			PrintStream out3;
			BufferedReader in;
			
			
			Avion a = new Avion();
			Avion b = new Avion();
			//Avion c = new Avion();
			
			a.ouvrir_communication();
			b.ouvrir_communication();
		    //c.ouvrir_communication();
			out1 = new PrintStream(a.socket.getOutputStream());
			out2 = new PrintStream(b.socket.getOutputStream());
			//out3 = new PrintStream(c.socket.getOutputStream());
			//in = new BufferedReader(new InputStreamReader(a.socket.getInputStream()));
			
			while(true){a.se_deplacer(out1);
			b.se_deplacer(out2);}
			//c.se_deplacer();
			

			

	    } catch (Exception e) {
	    	e.printStackTrace();
	    }

	}*/
	
}
