
public class Deplacement 
{
	private int vitesse;
	private int cap;
	
	public Deplacement(int v, int c)
	{
		this.vitesse = v;
		this.cap = c;
	}
	//
	public int getCap()
	{
		return this.cap;
	}
	//
	public int getVitesse()
	{
		return this.vitesse;
	}
	//
	public void setCap(int c)
	{
		this.cap = c;
	}
	//
	public void setVitesse(int v)
	{
		this.vitesse = v;
	}
}
