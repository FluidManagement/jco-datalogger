// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.can;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Properties;

public class ConfigFile
{
    private Properties configFile;
    private String confFile;
    private static final String confHeader = "CAN Trace Configuration File";
    
    public ConfigFile(final String confFile) {
        this.confFile = confFile;
        this.configFile = new Properties();
        try {
            this.configFile.load(new FileInputStream(confFile));
        }
        catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
    
    public void reloadConfigFile() {
        try {
            this.configFile.load(new FileInputStream(this.confFile));
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    public String getEntry(final String s) {
        return this.configFile.getProperty(s);
    }
    
    public void setEntry(final String s, final String s2) {
        this.configFile.setProperty(s, s2);
    }
    
    public void saveConfig() {
        try {
            this.configFile.store(new FileOutputStream(this.confFile), "CAN Trace Configuration File");
        }
        catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
