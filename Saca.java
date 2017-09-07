/* Copyright (c) 2017 Ralph Saade CNAM-LIBAN */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Saca extends Thread
{
  static HashMap<String,AvionSaca>  planesMap     =  new HashMap<String, AvionSaca>(); //map of planes : key=> plane name; Value=> plane object
  static HashMap<String,Socket>     planesSocket  =  new HashMap<String, Socket>();    //map of planeSockets : key=> plane name; Value=> plane socket
  static ArrayList<AvionSaca>       planesList    = new ArrayList<AvionSaca>();        // list of planes
  static ArrayList<BufferedReader> controlBuffers = new ArrayList<BufferedReader>();   //list of controls buffers  (in) 
  static ArrayList<PrintWriter>    controlOuters  =  new ArrayList<PrintWriter>();     //list of controls printers (out)
  static HashMap<String, String>   controlPlane   =  new HashMap<String, String>();    //map of controls/planes: key => control Name
                                                                                       //                       value => plane   Name
  //
  public static class AvionSaca
  {
    int x;
    int y;
    int a;
    int v;
    int c;
    String name;
    String cont;
    StringBuilder line;
    //
    public AvionSaca(String name, int x, int y, int a, int v, int c)
    {
      this.name = name;
      this.x = x;
      this.y = y;
      this.a = a;
      this.v = v;
      this.c = c;
      cont = "x";
      line = new StringBuilder();
    }
    //
    public String toString()
    {
      line.setLength(0);
      line.append("Avion: " ).append(name).append(" | Localisation: (").append(x).append(",").append(y);
      line.append(") | Altitude: ").append(a).append(" | Vitesse: ").append(v).append(" | Cap: ").append(c);
      return line.toString();
    }
    //
    public synchronized void setControleur(String c)
    {
      cont = c;
    }
    //
    public void free()
    {
      cont = "x";
    }
    //
    public boolean isBusy()
    {
      return !cont.equals("x");
    }
  }
  //
  public static void refreshPalnes(int i, String n, int x, int y, int a, int v, int c)
  {
    planesMap.get(n).x = x; planesList.get(i).x = x;
    planesMap.get(n).y = y; planesList.get(i).y = y;
    planesMap.get(n).a = a; planesList.get(i).a = a;
    planesMap.get(n).v = v; planesList.get(i).v = v;
    planesMap.get(n).c = c; planesList.get(i).c = c;
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
    return   str.trim(); 
  }
  //
  public static class Checker extends Thread
  {
    AvionSaca plane1;
    AvionSaca plane2;
    AvionSaca heighest;
    int  a = 0;
    int newA = 0;
    boolean  x = false;
    boolean  y = false;
    int limit = 2;
    PrintWriter outPlane;
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
              a = plane1.a            - plane2.a;
              x = (Math.abs(plane1.x   - plane2.x)   <= limit);
              y = (Math.abs(plane1.y   - plane2.y)   <= limit);
              if(Math.abs(a) <= limit && x && y)
              {
                System.out.println("Planes " + plane1.name + " and " + plane2.name + " will crach");
                if(a < 0)
                  heighest = plane2;
                else
                  heighest = plane1;                
                //
                newA = heighest.a + 3;
                try {
                  outPlane = new PrintWriter(planesSocket.get(heighest.name).getOutputStream(), true);
                  outPlane.print("a-"+newA);
                  System.out.println("Altitude changed for plane" + heighest.name + " to " + heighest.a + " to avoid crach");
                } catch (IOException e) {
                  e.printStackTrace();
                }

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
    public void checkPlane(String c, String n, AvionSaca p)
    {
      if(!p.isBusy())
      {  
        p.setControleur(c);
        controlPlane.put(c, n);
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
        AvionSaca plane = null;
        BufferedReader in;
        PrintWriter outControl;
        PrintWriter outPlane;
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
                  if(plane.isBusy() && !(controlleur.equals(plane.cont)))
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
                  outPlane = new PrintWriter(planesSocket.get(planeName).getOutputStream(), true);
                  switch(command)
                  {
                    case "speed" : 
                      checkPlane(controlleur, planeName, plane);
                      outPlane.println("v-"+value);
                      toControlClient = " Speed changed for " + planeName;
                      break;
                    case "cap" :
                      checkPlane(controlleur, planeName, plane);
                      outPlane.println("c-"+value);
                      toControlClient = " Cap changed for " + planeName;
                      break;
                    case "height" :
                      checkPlane(controlleur, planeName, plane);
                      outPlane.println("a-"+value);
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
                      outPlane.println("close");
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
    String data[];
    BufferedReader readPlane;
    AvionSaca a;
    BufferedReader p;
    //
    @Override
    public void run()
    {    
      try {
        while(true)
        {  
          for(int i=0; i< planesList.size(); i++)
          {
            a = planesList.get(i);
            p =  new BufferedReader(new InputStreamReader(planesSocket.get(a.name).getInputStream()));
            if(p.ready())
            {
              data = p.readLine().split(",");
              refreshPalnes(i, data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]),
                            Integer.parseInt(data[4]),Integer.parseInt(data[5]));
              System.out.println(planesList.get(i).toString());
            }
             
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
    PrintWriter writer;
    int j = 1;
    ServerLogPlanes slp = new ServerLogPlanes();
     Checker       check = new Checker();
     String data[];
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
          writer = new PrintWriter(socket.getOutputStream(), true);
          
          data = buffer.readLine().split(",");
          writer.println("From server: Hello Plane " + data[0]);
          AvionSaca a = new AvionSaca(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]),
                                      Integer.parseInt(data[3]), Integer.parseInt(data[4]), Integer.parseInt(data[5])); 
          planesMap.put(data[0],a);
          planesSocket.put(data[0], socket);
          planesList.add(a);
          System.out.println(a.toString());
          if(planesMap.size() == 1)
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
