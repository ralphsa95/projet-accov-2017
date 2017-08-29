
public class Coordonnees 
{
	private int x;
	private int y;
	private int altitude;
	
	public Coordonnees(int x, int y, int a)
	{
		this.x = x;
		this.y = y;
		this.altitude = a;
	}
	//
	public int getX()
	{
		return this.x;
	}
	//
	public int getY()
	{
		return this.y;
	}
	//
	public int getAlt()
	{
		return this.altitude;
	}
	//
	public void setX(int x)
	{
		this.x = x;
	}
	//
	public void setY(int y)
	{
		this.y = y;
	}
	//
	public void setAlt(int a)
	{
		this.altitude = a;
	}
}
