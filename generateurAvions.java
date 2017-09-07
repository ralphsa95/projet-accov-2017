import static java.lang.Thread.sleep;


public class generateurAvions {

  public static void main(final String[] args) {
    int numberOfSimultaneousExecutions = 2;
    java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(numberOfSimultaneousExecutions);
    for (int i = 0; i < numberOfSimultaneousExecutions; i++)
    {
      try { sleep(500);}
      catch (InterruptedException e) {e.printStackTrace();}  
      executor.execute(new Runnable()
      {
        @Override
        public void run()
        {
          Avion.main("");
  
        }
      }
      );
    }
  
  }
}