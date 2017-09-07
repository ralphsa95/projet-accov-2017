/* Copyright (c) 2017 Ralph Saade CNAM-LIBAN */

import java.io.*;
import java.net.*;

public class Controlleur extends Thread
{
  int id;
  public Controlleur(int i)
  {
    this.id = i;
  }
  
  @Override
  public void run()
  {
    Socket control;
    PrintWriter out;
    BufferedReader in;
    String message;
    BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
    try
    {
      System.out.println("Welcome .. Type your commands here: PlaneName-cap=value    to change cap");
      System.out.println("                                 or PlaneName-speed=value  to change speed");  
      System.out.println("                                 or PlaneName-height=value to change height");  
      System.out.println("                                 or PlaneName-free         to free plane");  
      System.out.println("                                 or PlaneName-close        to close plane communication with SACA");  
      control = new Socket("localhost", 2401);
      out = new PrintWriter(control.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(control.getInputStream()));
      String command;
    
      while(true)
      {
        message = null;
        command = read.readLine();
        out.println(command + "/" + id); // send to server
        message = in.readLine();
        if(message != null)
          System.out.println("Server response: " + message);
        
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
