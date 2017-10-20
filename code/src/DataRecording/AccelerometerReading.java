package DataRecording;

/**
 * Stores a full description of an accelerometer reading
 * Created by gcdc on 6/7/17.
 */
public class AccelerometerReading {
    long elapsedTime;
    int x,y,z;
    int index, nodeId;

    /**
     *
     * @param elapsedTime Time from initial sync
     * @param x x accelerometer reading
     * @param y y accelerometer reading
     * @param z z accelerometer reading
     * @param index index in object dictionary where reading was stored
     * @param nodeId Id of node that created this reading
     */
    public AccelerometerReading(long elapsedTime, int x, int y, int z, int index, int nodeId){
        this.elapsedTime = elapsedTime;
        this.x = x;
        this.y = y;
        this.z = z;
        this.index = index;
        this.nodeId = nodeId;
    }

    public AccelerometerReading(long elapsedTime, int[] data, int index, int nodeId){
        this.elapsedTime = elapsedTime;
        this.x = data[0];
        this.y = data[1];
        this.z = data[2];
        //can add functionality for more data coming off the nodes
        this.index = index;
        this.nodeId = nodeId;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getIndex() {
        return index;
    }
    
    public int getNodeId(){
        return nodeId;
    }

    public String toString(){
        return String.format("Elapsed Time: %d \nX: %d, Y: %d, Z: %d\nIndex: 0x%04x\n",elapsedTime,x,y,z,index);
    }
}
