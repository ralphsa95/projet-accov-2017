import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Saca extends Thread
{
	static HashMap<String,Avion>     planesMap;     //map of planes : key=> plane name; Value=> plane object
	static ArrayList<Avion> 				 planesList  = new ArrayList<Avion>(); // list of planes
	static ArrayList<BufferedReader> palneBuffers;  //list of planes socket buffers (in)
	static ArrayList<BufferedReader> controlBuffers = new ArrayList<BufferedReader>();//list of controls buffers  (in) 
	static ArrayList<PrintWriter>    controlOuters =  new ArrayList<PrintWriter>(); //list of controls printers (out)
	static HashMap<String, String>   controlPlane =  new HashMap<String, String>(); //map of controls/planes: key => control Name
																																									//											value => plane   Name
	//
	public Saca(HashMap<String, Avion> pm, ArrayList<Avion> pl)
	{
		planesMap  = pm;
		planesList = pl;
	}
	//
	public static String getStr(String s, String a, String b)
	{
		String str;
		if( b != null && a != null) 
			str = s.substring(s.indexOf(a) + 1, s.indexOf(b));
		else if(b == null)
			str = s.substring(0, s.indexOf(a));
		else // a == null
			str = s.substring(s.indexOf(b) + 1, s.length());
		//
		return 	str.trim(); 
	}
	//
	public static class Checker extends Thread
	{
		Avion plane1;
		Avion plane2;
		Avion heighest;
		int  a = 0;
		boolean  x = false;
		boolean  y = false;
		int limit = 2;
		@Override
		public void run()
		{
			while(true)
			{
				for(int i=0; i< planesList.size(); i++)
				{
					plane1 = planesList.get(i);
					for(int j=0; j< planesList.size(); j++)
					{
						if(i != j)
						{
							plane2 = planesList.get(j);
						  a = plane1.getCoord().getAlt() 					 - plane2.getCoord().getAlt();
						  x = (Math.abs(plane1.getCoord().getX()   - plane2.getCoord().getX())   <= limit);
						  y = (Math.abs(plane1.getCoord().getY()   - plane2.getCoord().getY())   <= limit);
						  if(Math.abs(a) <= limit && x && y)
						  {
						  	System.out.println("Planes " + plane1.getNumVol() + " and " + plane2.getNumVol() + " will crach");
						  	if(a < 0)
						  		heighest = plane2;
						  	else
						  	  heighest = plane1;
						  	//
						  	heighest.changer_altitude(heighest.getCoord().getAlt() + 3);
						  	System.out.println("Altitude changed for plane" + heighest.getNumVol() +
						  										 " to " + heighest.getCoord().getAlt()+ " to avoid crach");
						  }
						}
					}
				}
			}
		}
	}
	//
	public static class ServerLogControls extends Thread
	{
		public void checkPlane(String c, Avion p)
		{
			if(!p.isBusy())
			{	
				p.setControleur(c);
				controlPlane.put(c, p.getNumVol());
			}
		}
		//
		@Override
		public void run()
		{
			try
			{
				String request = "";
				String planeName = "";
				int value = 0;
				String toControlClient = "";
				boolean okToSend = true;
				String command = "";
				Avion plane = null;
				BufferedReader in;
				PrintWriter outControl;
				String controlleur = "";
				
				while(true)
				{
					for(int i =0; i< controlBuffers.size(); i++)
					{
						in = controlBuffers.get(i);
						outControl = controlOuters.get(i);
						 
						if(in.ready()) 
						{
						  request = in.readLine();
							
							//ex:MB622-speed=3 
						  //ex:MB622-height=3 
						  //ex:MB622-cap=3 
						  //ex:MB622-free
						  //ex:MB622-close
						  //control commands before execute
							planeName = request.substring(0, 5);
							okToSend = true;
							if(request.contains("-") && request.contains("/"))
							{
								controlleur = getStr(request, null, "/");
								if(!planesMap.containsKey(planeName))// check plane name
								{	
									okToSend = false;
									toControlClient = "Plane " + planeName + " doesn't exist ";
								}
								else
								{	
									plane = planesMap.get(planeName);// get plane object
									if(plane.isBusy() && !(controlleur.equals(plane.getControlleur())))
									{
									  // check if plane busy with its controller
										toControlClient = "This plane is currently busy .. Try again later";
										okToSend = false;
									}
									else if(controlPlane.containsKey(controlleur) && !planeName.equals(controlPlane.get(controlleur))) 
									{
									  // check controller if controlling another plane 
										toControlClient = "Controlleur " + controlleur + " is controlling another plane";
										okToSend = false;
									}
									else
										command = request.contains("=") ? getStr(request, "-", "="): getStr(request, "-", "/");
									
								}
								//
								if(okToSend && request.contains("="))
								{
								// check value if integer
									try{
										value = Integer.parseInt(getStr(request,"=","/"));
									}catch (NumberFormatException e){
										okToSend = false;
										toControlClient += "Value not number: " + value;
									}
								}
								//
								if(okToSend)
								{
									// checking command from control
									switch(command)
									{
										case "speed" : 
											checkPlane(controlleur, plane);
											plane.changer_vitesse(value);
											toControlClient = " Speed changed for " + planeName;
											break;
										case "cap" :
											checkPlane(controlleur, plane);
											plane.changer_cap(value);
											toControlClient = " Cap changed for " + planeName;
											break;
										case "height" :
											checkPlane(controlleur, plane);
											plane.changer_altitude(value);
											toControlClient = " Height changed for " + planeName;
											break;
										case "free" :
											controlPlane.remove(controlleur);
											plane.free();
											toControlClient = " " + planeName + " is free";
											break;
										case "close" :
											controlPlane.remove(controlleur);
											plane.free();
											plane.fermer_communication();
											toControlClient = " " + planeName + " communictaion is closed";
											break;
										default :
											okToSend = false;
											toControlClient += "Command not found for " + planeName + " ";
									}
								}
							}
							else
							{	
								toControlClient = "Request not properly written: " + request;
								okToSend = false;
							}
							//
							if(okToSend)
								outControl.println("Request Executed" + toControlClient);
							else
								outControl.println(toControlClient);
						}

					}
				}
			
		  }catch(Exception e){
			e.printStackTrace();}
		}
	}
	//
	public static class ServerListenControl extends Thread
	{

		int j = 1;
		@Override
		public void run()
		{
			ServerSocket serverControl;
			Socket controlSocket;
			BufferedReader in;
			PrintWriter outControl;
			try 
			{
				serverControl = new ServerSocket(2401);
				ServerLogControls logControls;
				while(true)
				{
					controlSocket = serverControl.accept();
					System.out.println("Control "+ j++ +" connected");
					in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
					outControl = new PrintWriter(controlSocket.getOutputStream(), true);
					controlBuffers.add(in);
					controlOuters.add(outControl);
					if(controlBuffers.size() == 1)
					{
						logControls = new ServerLogControls();
						logControls.start();
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
  //
	public static class ServerLogPlanes extends Thread
	{	
		String line;
		//
		@Override
		public void run()
		{		
			try {
				while(true)
				{
					for(int i=0; i< palneBuffers.size(); i++)
					{		
	     			line = palneBuffers.get(i).readLine();
	     			if(line != null) {System.out.println(line);}
					}	
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
				
		}
	}
	
	public static class ServerListenPlane extends Thread
	{
		Socket socket;
		BufferedReader buffer;
		int j = 1;
		ServerLogPlanes slp = new ServerLogPlanes();
   	Checker       check = new Checker();
		//
		@Override
		public void run()
		{	
			ServerSocket serverPlane;
			try
			{
				serverPlane = new ServerSocket(2400);
				while (true)
				{
					socket = serverPlane.accept();
					System.out.println("Plane " + j++ + " connected");
					buffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					palneBuffers.add(buffer);
					if(palneBuffers.size() == 1)
					{	
						slp.start();
						check.start();
					}
				}		
		           
				} catch (IOException e) {
					e.printStackTrace();
				}	
		}
		
	}
	
	@Override
	public void run()
	{
		//planes = new HashMap<String,Socket>();
		palneBuffers = new ArrayList<BufferedReader>();		

		System.out.println("Server Started");		
		
	  ServerListenPlane ts = new ServerListenPlane();
	  ServerListenControl sc = new ServerListenControl();
		try 
		{
			ts.start();
			sc.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
