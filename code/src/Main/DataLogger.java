package Main;
 
import DataFormatting.DataFormatter;
import DataRecording.AccelerometerReading;
import DataRecording.NodeTracker;
import GlobalVars.GlobalVars;

import com.gcdc.can.CanMessage;
import com.gcdc.can.Driver;
import com.gcdc.can.DriverManager;
import com.gcdc.canopen.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryNotEmptyException;

import java.util.Set;	// for Set
import java.util.Timer;
import java.util.TimerTask;
 
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
/**
 * Created by gcdc on 6/7/17.
 */
public class DataLogger {
    private CanOpen co;
    private DriverManager dm;
    private Driver drvr;
    private ObjectDictionary od;
    private NodeTracker[] nodes;
    private int odIndexes[] = new int[]{0x6210, 0x6211, 0x6212, 0x6213};
    private int rxpdoIndexes[] = new int[]{0x10, 0x11, 0x12, 0x13};
    private DataFormatter dfmt;
    private FileHandler fileHandler;
    private SyncListener sl;
    private Timer shutdownTimer;
    private Thread coThread;
    private SocketListener socketListener;
    private Thread socketThread;
    private String[] args;
    private boolean recordingStatus;
    private ReadableInstant startInstant;
    
    //Weird args4j stuff here
    @Option(name="-l",aliases={"--length"},usage="Maximum number of sample lines per data file",forbids={"-i"})
    private Integer fileLength = 1000; 
    
    @Option(name="-i",aliases={"--infinite"}, usage = "Enable infinite data files", forbids={"-l"})
    private Boolean infiniteDataFile = false;
    
    @Option(name="-d",aliases={"--debug"}, usage = "Enable debugging statements")
    private Boolean debug = false;

    @Option(name="-a",aliases={"--address"}, usage = "Set IF socat address",required=true)
    private String  address;   
    
    @Option(name="-b",aliases={"--base-name"}, usage = "Set base file name")
    private String baseName = "DATA_";
    
    @Option(name="-dir",aliases={"--directory"}, usage = "Directory to place data files")
    private String directory = ".";
    
    @Option(name="-t",aliases={"--timed","--runtime"}, usage = "Specify period of time in milliseconds for java app to run")
    private Long runTime = null;
    
    @Option(name="-s",aliases={"--start","--start-immediately"}, usage = "Will start recording data immediately instead of waiting for signal from socket")
    private Boolean startImmediately = false;
    
    @Option(name="-f",aliases={"--file-limit"},usage = "Set the maximum number of data files to be created")
    private Integer fileLimit = null;
    
    @Option(name="-help",help=true,usage="Bring up the extended help screen")
    private Boolean help = false;
    
    
    private class SyncListener implements CanOpenListener{
        boolean objectDictReady = false;
        
        //Adds Self to the Canopen instance's list of sync listeners
        public void startSyncListener(){
            if(!recordingStatus){
                co.addSyncListener(this);
                recordingStatus = true;
                startInstant = DateTime.now();
                if(runTime != null){
                    shutdownTimer.schedule(new ShutdownTimer(), runTime.longValue());    
                }
            }
        }
        
        //Removes self from the Canopen instance's list of sync listeners
        public void stopSyncListener(){
            co.removeSyncListener(this);
            fileHandler.close();
            GlobalVars.START_TIME = null;
            recordingStatus = false;
        }
        
        //Acts on recieved sync messages
        //Creates new file if needed
        //Gets the latest sample from the NodeListeners
        //Closes file and clears the per-file start time if file size is reached
        /*
         * @param canMessage - The Sync message, supplied by CanOpen. Currently unused
         */
        @Override
        public void onMessage(CanMessage canMessage) {
//	        debugPrint("SYNC message received");
                if(objectDictReady){
                    if (GlobalVars.START_TIME == null) {
                        GlobalVars.START_TIME = System.nanoTime();
                        fileHandler.createFile();
                        fileHandler.printLine(dfmt.produceHeader(rxpdoIndexes));
                        debugPrint(dfmt.produceHeader(rxpdoIndexes));
                    } else {
                        AccelerometerReading readings[] = new AccelerometerReading[nodes.length];
                        for(int i = 0; i < nodes.length; i++ ){
                            readings[i] = nodes[i].getLatestReading();
                        }
    //		        debugPrint(dfmt.producePrettyOutputString(readings));
                        if((fileHandler.currentSampleSize <  fileLength)||infiniteDataFile)
                        {
                            fileHandler.printSample(dfmt.produceOutputLine(readings));
                            debugPrint(dfmt.produceOutputLine(readings));
                        }else
                        {
                            GlobalVars.START_TIME = null;
                            fileHandler.close();
                        }
                    }
                }
        }

        //Listens to the objestDictionary and sets a flag whenever a change occurs to a specific index
        //After a change has occured that means the busmaster has begun recieving pdos
        /*
         * @param se - Subentry that has changed, unused
         */
        @Override
        public void onObjDictChange(SubEntry se){
            objectDictReady = true;
        }
        @Override
        public void onEvent(CanOpen co, int state) {}
    }
    
    private class FileHandler{
        private PrintWriter output;
        private int currentSampleSize;
        private String currentFileName;
        private int currentFileNumber = 0;
        
        //Clears the Data Directory of .csv files
        //This should really only be used through the cgi-bin script
        //If the java app is manually ran instead of executed through 'onBootSetup.sh'
        // it may result in unexpection deletions
        //It is recommended to not clear any directories until the user is positive 
        // they do not need the data
        /*
         * @returns - A String to be sent back to the cgi-bin script, either success or error
         */
        public String clearDirectory(){
            Path dir = Paths.get(directory);
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)){
                for (Path file: stream){
                    if(file.toString().endsWith(".csv")){
                        debugPrint("FileHandler.clearDirectory(): Deleting file "+file.toString());
                        Files.deleteIfExists(file);
                    }else{
                        debugPrint("FileHandler.clearDirectory(): Skipping file "+file.toString());
                    }
                }
            }catch(DirectoryNotEmptyException dnee){
                return "Error clearing data";
            }catch(IOException ioe){
                return "Error clearing data";
            }
            currentFileNumber = 0;
            return "Success";
            
        }
        
        //Creates a new data file and resets or increments relevant variables        
        public void createFile(){
            if((fileLimit != null)&&(currentFileNumber>=fileLimit)){
                gracefulShutdown();
            }else{
                if(output != null)
                    close();
                try{
                    currentFileName = baseName+String.format("%03d", currentFileNumber)+".csv";
                    output = new PrintWriter(new BufferedWriter(new FileWriter(directory+"/"+currentFileName,false)));            
                }catch(Exception e){
                    debugPrint("createFile() couldn't create file");
                }
                currentSampleSize = 0;
                currentFileNumber++;
            }
        }
        
        //Closes file and makes comment at end of file
        public void close(){
            makeEOFComment();
            if(output!=null){
                output.close();
            }
            output = null;
        }
        
        //Prints a line of text to the current file
        /*
         * @param line - Line to be printed
         */ 
        public void printLine(String line){
            if(output == null)
                createFile();
                
            output.println(line);
            output.flush();
        }
        
        //Wrapper for println that increments the currentSampleSize
        /*
         * @param sample - Line to be printed;
         */ 
        public void printSample(String sample){
            printLine(sample);
            currentSampleSize++;
        }
        
        //Generates the last line of the data file
        //Simply contains info on the number of samples in the file
        // and the average sample rate across the file
        //Does not rely on currentSampleSize to be correct
        //This could probably be changed to show more faith in the instance
        // variables but its works fine like it is.
        public void makeEOFComment(){
            Double firstSampleTime = new Double(0);
            Double lastSampleTime = new Double(0);
            int sampleNumber = 0;
            if(currentFileName != null){
                try{
                    LineNumberReader lnr = new LineNumberReader(new FileReader(new File(directory+"/"+currentFileName)));
                    boolean flushedHeader = false;
                    int headerCount = 0;
                    while(!flushedHeader){
                        String firstSampleTimeString = lnr.readLine();
                        if(firstSampleTimeString.charAt(0) != (char)';'){
                            firstSampleTime = Double.parseDouble(firstSampleTimeString.split(",")[0]);
                            flushedHeader = true;
                        }
                        else{
                            headerCount++;
                        }
                    }

                    String lastLine = new String();
                    do{
                        String newLine = lnr.readLine();
                        if(newLine == null)
                            break;
                        else
                            lastLine = newLine;
                    }while(lastLine != null);
                    
                    if(lastLine.startsWith(";"))
                        return;	//This means the comment was already generated

                    lastSampleTime = Double.parseDouble(lastLine.split(",")[0]);
                    lnr.skip(Long.MAX_VALUE);//go to the end of the file
                    sampleNumber = lnr.getLineNumber()-headerCount;
                    lnr.close();
                }catch(IOException ioe){
                    ioe.printStackTrace();
                    return;            
                }
                
                double deltaTimeSeconds = (lastSampleTime - firstSampleTime); 
                double sampleRate = ((double)sampleNumber/deltaTimeSeconds);
                printLine(String.format(";%d samples at %.3fHz", sampleNumber, sampleRate));    
            }        
        }
        
        
    }
    
    //This class listens for various commands generated by
    // the cgi-bin scripts
    private class SocketListener implements Runnable{
        ServerSocket listenerSocket;
        Socket connection = null;
        BufferedReader in;
        PrintStream out;
        
        int portnum;
        
        /*
         * @param portnum - port number to listen too
         */
        public SocketListener(int portnum){
            this.portnum = portnum;
        }
        
        //7331 is the port that the cgi-bin scripts use
        public SocketListener(){
            this.portnum = 7331;
        }
        
        //closes the reader, writer, and socket connection  
        public void closeConnection(){
            try{
                if(in != null)
                    in.close();
                if(out != null)
                    out.close();
                if(listenerSocket != null)
                    listenerSocket.close();
                if(connection != null)
                    connection.close();            
            }catch(IOException ioe){
                ioe.printStackTrace();    
            }
        }
        
        //sets the max number of samples in a file
        /*
         * @param message - 	The message from the cgi-bin script
         *			to the socket. Should be in format:
         *			[command]=[argument]. The argument determines
         *			the new sampleSize.
         * @returns - The new sample size as a String 
         */
        private String setSampleSize(String message){
            String lengthStr = message.split("=")[1];
            fileLength = Integer.parseInt(lengthStr);
            debugPrint("SocketListener.setSampleSize(): "+ lengthStr);
            return Integer.toString(fileLength);
                    
        }
        
        //Starts or stops the sync listener 
        /*
         * @param message - 	The message from the cgi-bin script
         *			to the socket. Should be in format:
         *			[command]=[argument]. The argument determines
         *			whether to turn the sync listener on or off
         * @returns - The state of the sync listener or an error message  
         */
        private String setRecording(String message){
            String toState = message.split("=")[1];
            debugPrint("SocketListener.setRecording(): ("+toState+")");
            switch(toState){
                case "start":
                    sl.startSyncListener();
                    return "on";
                case "stop":
                    sl.stopSyncListener();
                    return "off";
                default:
                    return("ERROR: Unknown recording argument");
            }
            
        }
        
        //Asks for the status of the java app
        //Right now this includes the:
        //	Recording status
        //	Max Samples per file
        //	Base filename
        //And if the Recording Status is true:
        //	Last time startSyncListener() was invoked
        //	Uptime
        /*
         * @returns - Status of the java app
         */
        private String getStatus(){
            StringBuilder sb = new StringBuilder();
            if(recordingStatus){
                sb.append( "Recording\n");
                sb.append("Samples per file: ");
                sb.append(fileLength);
                sb.append("\n");
                sb.append("Base filename: " + baseName + "\n");
                DateTimeFormatter dateFormat = new DateTimeFormatterBuilder()
                    .appendYearOfEra(4,4)
                    .appendLiteral("-")
                    .appendMonthOfYear(2)
                    .appendLiteral("-")
                    .appendDayOfMonth(2)
                    .appendLiteral(" ")
                    .appendClockhourOfDay(2)
                    .appendLiteral(":")
                    .appendMinuteOfHour(2)
                    .appendLiteral(":")
                    .appendSecondOfMinute(2)
                    .toFormatter();
                    
                sb.append("StartTime: " + dateFormat.print(startInstant) +"\n");
                ReadableInstant endInstant = DateTime.now();
                Period uptime = new Period(startInstant, endInstant);
                PeriodFormatter daysHoursMinutes = new PeriodFormatterBuilder()
                    .appendDays()
                    .appendSuffix("d")
                    .appendSeparator(" ")
                    .appendHours()
                    .appendSuffix("hr")
                    .appendSeparator(" ")
                    .appendMinutes()
                    .appendSuffix("mn")
                    .appendSeparator(" ")
                    .appendSeconds()
                    .appendSuffix("sec")
                    .appendSeparator(" ")
                    .toFormatter();
                    
                sb.append("Running for: " + daysHoursMinutes.print(uptime.normalizedStandard()));
                return sb.toString();
            }else{
                sb.append("Not Recording\n");
                sb.append("Samples per file: ");
                sb.append(fileLength);
                sb.append("\n");
                sb.append("Base filename: " + baseName + "\n");
                return sb.toString();
            }
                        
        }
        
        //Sets a new basename to be used by the filehandler
        /*
         * @param message - 	The message from the cgi-bin script
         *			to the socket. Should be in format:
         *			[command]=[argument]. The argument
         *			will become the new baseName.
         * @returns - The new basename  
         */
        private String setBaseName(String message){
            String newName = message.split("=")[1];
            debugPrint("SocketListener.setBaseName(): "+newName);
            baseName = newName;
            return baseName;
        }
        
        //A more local wrapper for fileHandler.clearDirectory()
        /*
         * @returns - Result of fileHandler.clearDirectory(), either success or error
         */
        private String clearData(){
            return fileHandler.clearDirectory();    
        }
        
        //Determines which specific method should recieve the message
        //Does so by checking the command portion of the message
        /*
         * @param message - 	The message from the cgi-bin script
         *			to the socket. Should be in format:
         *			[command]=[argument]. The command will
         *			determine which method the message is sent to.
         * @returns -	The result of any of the more specific methods  
         *		or an Error in the result or an unkown command
         */
        private String processMessage(String message){
            String[] splitString = message.split("=");
            switch(splitString[0]){
                case "numSamples":
                    return setSampleSize(message);
                case "recording":
                    return setRecording(message);
                case "status":
                    return getStatus();
                case "fileName":
                    return setBaseName(message);                    
                case "clearData":
                    return clearData();
                default:
                    debugPrint("SocketListener.processMessage(): Unknown message");
                    return ("ERROR: Unknown message");
            }    
        }
        
        //Sets up the socket connection
        public void setupConnection() throws IOException{
            listenerSocket = new ServerSocket(portnum);
            debugPrint("SocketListener.setupConnection(): Waiting for connection");
            connection = listenerSocket.accept();
            debugPrint("SocketListener.setupConnection(): Connection received from " + connection.getInetAddress().getHostName());
            out = new PrintStream(connection.getOutputStream());
            out.flush();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));    
        }
        
        //Main loop
        //Waits for socket messages and sends them to the processMessage method
        @Override
        public void run(){
            try{
                while(true){
                    setupConnection();
                    debugPrint("SocketListener.run(): Connection has been set up");
                    boolean loop = true;
                    String message;
                    while((message = in.readLine()) != null){
                        debugPrint("SocketListener.run(): Message recieved " + message);
                        String retVal = processMessage(message);
                        out.print(retVal);
                    }
                    closeConnection();
                } 
            }catch(SocketException se){
                debugPrint("SocketListener.run(): Socket was probably closed, no need to panic");
            }catch(IOException ioe){
                debugPrint("SocketListener.run(): IO Error");
                ioe.printStackTrace();
            }
        }
    }
    
    
    //Does most of the CanOpen setup stuff
    private class CanOpenThread implements Runnable{
        //Sets up CanOpen stuff
        //Creates the NodeTrackers and SyncListener      
        @Override
        public void run(){
            
            boolean restart = false;
            try{
               do{
                    debugPrint("CANbus driver starting");
                    dm = new DriverManager("datagram", address, 2000, false);
                    drvr = dm.getDriver();
                    debugPrint("CANbus driver configured");
                    od = DefaultOD.create(0x23);
                    co = new CanOpen(drvr, od, 0x23, GlobalVars.DEBUG);
                    
                    nodes = new NodeTracker[4];
                    nodes[0] = new NodeTracker(co, 0x281, rxpdoIndexes[0], odIndexes[0], 0x3, 0x10, 0,1,2);
                    nodes[1] = new NodeTracker(co, 0x282, rxpdoIndexes[1], odIndexes[1], 0x3, 0x10, 0,1,2);
                    nodes[2] = new NodeTracker(co, 0x283, rxpdoIndexes[2], odIndexes[2], 0x3, 0x10, 0,1,2);
                    nodes[3] = new NodeTracker(co, 0x284, rxpdoIndexes[3], odIndexes[3], 0x3, 0x10, 0,1,2);
                    sl = new SyncListener();
                    od.getEntry(odIndexes[0]).getSub(0).addListener(sl);
                    od.getEntry(odIndexes[1]).getSub(0).addListener(sl);
                    od.getEntry(odIndexes[2]).getSub(0).addListener(sl);
                    od.getEntry(odIndexes[3]).getSub(0).addListener(sl);
                    
                    debugPrint("CanOpen configured");
                    debugPrint("CanOpen Starting");
                    
                    co.start();
                    if(startImmediately){
                        sl.startSyncListener();
                    }    
                    co.join();               
                    debugPrint("CanOpenThread.run(): co.start() is finished");
                }while(restart);
            }catch(InterruptedException ie){
                dm.unloadDriver();
                co.toRebootState();        
                coThread.interrupt();
                coThread = null;
                dm.unloadDriver();
                drvr = null; 
                dm = null;
                System.gc();
                co.toRebootState();
                co = null;
                od = null;
                GlobalVars.START_TIME = null;
                for(NodeTracker node : nodes){
                    node = null;
                }
                fileHandler = null;
                System.gc();
            }catch(COException coe){
                coe.printStackTrace();
            }
        }
    }
    
    private class ShutdownTimer extends TimerTask{
        
        @Override
        public void run(){
            gracefulShutdown();                
        }
    } 
    
    //constructor
    //Sets up most of the Threads and important Variables
    //Also handles any special cases for the command line arguments
    /*
     * @param args - List of commandLine Arguments
     */
    public DataLogger(String[] args){
        this.args = args;
        
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(100);//???
        try
        {
            parser.parseArgument(args);        
        }catch(CmdLineException cle)
        {
            parser.printUsage(System.err);
            debugPrint("DataLogger(): " + cle.getMessage());
            System.exit(-1);
        }
        if(help){
            try{
                BufferedReader helpReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("helpscreen.txt")));    
                String helpLine;
                helpLine = helpReader.readLine();
                while(helpLine!=null){
                    System.out.println(helpLine);
                    helpLine = helpReader.readLine();
                }
                helpReader.close();
            }catch(IOException ioe){
                ioe.printStackTrace(); 
            }         
        }else{
            socketListener = new SocketListener();
            socketThread = new Thread(socketListener);
            socketThread.start();
            
            shutdownTimer = new Timer("Shutdown Timer", true);//true means it is a daemon
            
            GlobalVars.DEBUG = debug;
                    
            dfmt = new DataFormatter();
            fileHandler = new FileHandler();
            
            recordingStatus = false;
            
            coThread = new Thread(new CanOpenThread());
            coThread.start();

            try{socketThread.join();}
            catch(InterruptedException ie){
                ie.printStackTrace();
            }
        }
    }
    
    //Utility to print debug info
    //only prints when DEBUG is turned on
    /*
     * param s - String to print
     */
    public static void debugPrint(String s){
        if(GlobalVars.DEBUG)
            System.out.println(s);         
    }   
    
    //Shuts down everything
    public void gracefulShutdown(){
        sl.stopSyncListener();
        //fileHandler.close(); //stopSyncListener makes a call to fileHandler.close, so this is unnecessary
        coThread.interrupt();
        socketListener.closeConnection();
        socketThread.interrupt();
    }
    
    
    //Main method
    //Creates DataLogger Instance
    /*
     * @param args - List of comand-line arguments
     */
    public static void main(String args[]){
        DataLogger dl = new DataLogger(args);
        debugPrint("exit");
        System.exit(0);
    }
}
