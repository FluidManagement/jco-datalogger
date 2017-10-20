package DataRecording;
/**
 * Created by gcdc on 6/7/17.
 * Stores all data collected from a node and dumps it on command
 */
import GlobalVars.GlobalVars;
import com.gcdc.canopen.*;

import java.util.ArrayList;

public class NodeTracker {
    private ArrayList<AccelerometerReading> readingBuffer;
    private CanOpen co;
    private int targetIndex, nodeId, subindexes[];
    


    //Assumes that the CanOpen has been initialized and the Object dictionary has been created
    /**
     *
     * @param coInstance An already configured instance of CanOpen
     * @param cobid CobId of node to be monitored
     * @param index RxPdo targetIndex
     * @param targetIndex Index to place data in Object Dictionary
     * @param numSamples Number of samples/subindexes to create
     * @param numBits Length of subindexes
     * @param subindexes List of subindexes to monitor
     */
    public NodeTracker(CanOpen coInstance, int cobid, int index, int targetIndex, int numSamples, int numBits, int ... subindexes){
//        readingBuffer = new ArrayList<>();
        this.nodeId = index;
        this.targetIndex = targetIndex;
        this.subindexes = subindexes;
        co = coInstance;
        DefaultOD.insertRxpdo(co.getObjDict(), cobid, index, targetIndex, numSamples, numBits);
        try {
            co.addRxPdoIndex(index);
        } catch (Exception e) {
            System.out.println("Could not add RxPdo Index");
            e.printStackTrace();
        }
//        SyncListener syncListener = new SyncListener();
//        co.addSyncListener(syncListener);
    }

//    public AccelerometerReading getLatestReading(){
//        return readingBuffer.remove(readingBuffer.size()-1);
//    }

    public AccelerometerReading getLatestReading(){
        long elapsedTime = System.nanoTime()-GlobalVars.START_TIME;
        int data[] = new int[3];
        for(int i = 0; i < subindexes.length;i++){
            try {
                data[i] = co.getObjDict().getSubEntry(targetIndex,subindexes[i]).getInt();
            } catch (Exception e) {
                System.out.println("Could not read subentry 0x"+Integer.toHexString(targetIndex <<8|subindexes[i]));
                e.printStackTrace();
            }
//            readingBuffer.add(new AccelerometerReading(elapsedTime,data,targetIndex));

        }
        return new AccelerometerReading(elapsedTime,data, targetIndex, nodeId);
    }
}
