package DataRecording;

/**
* Stores a full description of an accelerometer reading
* Created by gcdc on 6/7/17.
*/
public class AccelerometerReading
{
	private int x,y,z;

	/**
	*
	* @param elapsedTime Time from initial sync
	* @param x x accelerometer reading
	* @param y y accelerometer reading
	* @param z z accelerometer reading
	* @param index index in object dictionary where reading was stored
	* @param nodeId Id of node that created this reading
	*/
	public AccelerometerReading(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public AccelerometerReading( int[] data)
	{
		this.x = data[0];
		this.y = data[1];
		this.z = data[2];
	}


	public int getX()
	{
		return x;
	}


	public int getY()
	{
		return y;
	}

	public int getZ()
	{
		return z;
	}

	public String toString()
	{
		return String.format("X: %d, Y: %d, Z: %d\n",x,y,z);
	}
	
	public StringBuilder toStringBuilder()
	{
		StringBuilder output = new StringBuilder(100);
		output.append(x);
		output.append(",");
		output.append(y);
		output.append(",");
		output.append(z);
		return(output);
	}
}
