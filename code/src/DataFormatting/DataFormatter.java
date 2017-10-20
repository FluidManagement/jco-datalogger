package DataFormatting;

import DataRecording.AccelerometerReading;
import DataRecording.SubIndexValue;
import DataRecording.NodeTracker;
import GlobalVars.GlobalVars;

import java.util.ArrayList;
import java.util.Date;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.text.SimpleDateFormat;
import java.io.File;
/**
 * Created by gcdc on 6/7/17.
 */
public class DataFormatter {
    private final String REVISION_KEY = "Canopen-Revision";
    private final String DATE_KEY = "Build-Date"; 
    private final String VERSION_KEY = "Version";
    public DataFormatter(){}

    /**
     *
     * @param readings A list of readings from the accelerometers
     * @return Example output:  Elapsed Time: 208441739
                                X: 2731, Y: 3343, Z: 2192
                                Index: 0x6210
                                Elapsed Time: 208477156
                                X: 2502, Y: 1504, Z: 2305
                                Index: 0x6211
                                Elapsed Time: 208501947
                                X: 2787, Y: 1825, Z: 1820
                                Index: 0x6212
                                Elapsed Time: 208527208
                                X: 2455, Y: 3095, Z: 3201
                                Index: 0x6213
     */
    public String producePrettyOutputString(AccelerometerReading... readings){
        String retVal = new String();
        for(AccelerometerReading reading : readings){
            retVal = retVal.concat(reading.toString());
        }
        return retVal;

    }
    
    public String getManifestAttribute(String key, Attributes attrs){
        if(attrs == null){
            return "";
        } 
        String val = attrs.getValue(key);
        if(val == null)
            val = "";
        return val;
    }
    
    /**
     *
     * @param indexes A list of indexes that we recieve readings from
     * @return A header comment
     */
    public String produceHeader(int[] rxpdoIndexes){
        JarFile jar;
        Manifest manifest;
        Attributes attrs = null;
        try{
            jar = new JarFile(new File(DataFormatter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
            manifest = jar.getManifest();
            attrs = manifest.getMainAttributes();
        }catch(Exception e){
            System.out.println("Manifest could not be read");
        }        
        String header = new String("");
        
        String headerTitle = ";Title";
        headerTitle = headerTitle.concat(",[url]");
        headerTitle = headerTitle.concat(",[product]");
        headerTitle = headerTitle.concat(",[sensor_type]");
        //headerTitle = headerTitle.concat(",[sensor_typeN]");
        header = header.concat(headerTitle+"\n");
        
        String headerVersion = ";Version";
        headerVersion = headerVersion.concat(","+getManifestAttribute(VERSION_KEY,attrs));
        headerVersion = headerVersion.concat(",Build date:");
        headerVersion = headerVersion.concat(","+getManifestAttribute(DATE_KEY,attrs));
        headerVersion = headerVersion.concat(",[serial_number]");
        header = header.concat(headerVersion + "\n");
        
        String headerCanopen = ";Canopen";
        headerCanopen = headerCanopen.concat(",Revision:");
        headerCanopen = headerCanopen.concat(","+ getManifestAttribute(REVISION_KEY,attrs));
        header = header.concat(headerCanopen + "\n");
        
        String headerStartTime = ";StartTime";
        headerStartTime = headerStartTime.concat(","+new SimpleDateFormat("yyyy-MM-d").format(new Date()));
        headerStartTime = headerStartTime.concat(","+new SimpleDateFormat("HH:mm:ss.SS").format(new Date()));
        header = header.concat(headerStartTime+"\n");
        
        String headerSample = ";SampleRate";
        headerSample = headerSample.concat(",[rate]");
        headerSample = headerSample.concat(",[unit]");
        header = header.concat(headerSample+"\n");
            
    
        String headerLabels = ";Time "; //new String(";").concat(new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss:SS z").format(new Date()));
        for(int id : rxpdoIndexes){
            headerLabels = headerLabels.concat(","+Integer.toHexString(id)+"(X)");
            headerLabels = headerLabels.concat(","+Integer.toHexString(id)+"(Y)");
            headerLabels = headerLabels.concat(","+Integer.toHexString(id)+"(Z)");
        }
        header = header.concat(headerLabels); //no newline intentionally
        return header;
    }
    
    public String produceFileName(){
        String filename = new SimpleDateFormat("EEEE d MMM yyyy HH:mm:ss:SS z").format(new Date());
        return filename;
    }

    public String produceOutputLine(AccelerometerReading[] readings){
        String output = new String("");
        //NOTE:
        //  The elapsed time value will be different for each reading
        //  even though they were all gathered at the same Sync message.
        //  The timestamp of the first reading will be used on the line
        //  because that is assumed to be the most accurate. Luckily
        //  the margin of error is only a few ten thousandths of a second
        //  so it shouldn't cause any problems.
        long nanoElapsed = readings[0].getElapsedTime();
        double secondElapsed = (double)nanoElapsed/(double)1000000000; //10^9
        
        output = output.concat(String.format("%3f",secondElapsed));
        for(AccelerometerReading reading : readings){
            output = output.concat(","+Integer.toString(reading.getX()));
            output = output.concat(","+Integer.toString(reading.getY()));
            output = output.concat(","+Integer.toString(reading.getZ()));
        }
        return output;
    }
}
