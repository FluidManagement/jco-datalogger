// 
// Decompiled by Procyon v0.5.30
// 

package com.gcdc.canopen;

public class DefaultOD
{
    public static ObjectDictionary createStandardDict(final int n, final int n2, final int n3, final int n4, final int n5, final int n6, final int n7) {
        final ObjectDictionary objectDictionary = new ObjectDictionary();
        final OdEntry odEntry = new OdEntry(4096, "Device Type");
        objectDictionary.insert(odEntry);
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.RO, "", n2));
        final OdEntry odEntry2 = new OdEntry(4097, "Error Register");
        objectDictionary.insert(odEntry2);
        odEntry2.appendSub(new SubEntry(SubEntry.AccessType.RO, "", (byte)0));
        final OdEntry odEntry3 = new OdEntry(4101, "SYNC COB ID");
        objectDictionary.insert(odEntry3);
        odEntry3.appendSub(new SubEntry(SubEntry.AccessType.RW, "", 128));
        final OdEntry odEntry4 = new OdEntry(4116, "Emergency COB ID");
        objectDictionary.insert(odEntry4);
        odEntry4.appendSub(new SubEntry(SubEntry.AccessType.RW, "", 0x80 | n));
        final OdEntry odEntry5 = new OdEntry(4119, "Producer Heartbeat Time");
        odEntry5.appendSub(new SubEntry(SubEntry.AccessType.RW, "", (short)n3));
        objectDictionary.insert(odEntry5);
        final OdEntry odEntry6 = new OdEntry(4120, "Identity Object");
        objectDictionary.insert(odEntry6);
        odEntry6.appendSub(new SubEntry(SubEntry.AccessType.CONST, "number of Entries", (byte)4));
        odEntry6.appendSub(new SubEntry(SubEntry.AccessType.RO, "Vendor-ID", n4));
        odEntry6.appendSub(new SubEntry(SubEntry.AccessType.RO, "Product code", n5));
        odEntry6.appendSub(new SubEntry(SubEntry.AccessType.RO, "Revision number", n6));
        odEntry6.appendSub(new SubEntry(SubEntry.AccessType.RO, "Serial number", n7));
        final OdEntry odEntry7 = new OdEntry(4608, "SDO server parameters");
        objectDictionary.insert(odEntry7);
        odEntry7.appendSub(new SubEntry(SubEntry.AccessType.CONST, "number of Entries", (byte)2));
        odEntry7.appendSub(new SubEntry(SubEntry.AccessType.RO, "COB-ID client to server", 0x600 | n));
        odEntry7.appendSub(new SubEntry(SubEntry.AccessType.RO, "COB-ID server to client", 0x580 | n));
        return objectDictionary;
    }
    
    public static ObjectDictionary create(final int n) {
        final ObjectDictionary standardDict = createStandardDict(n, 131474, 5000, 668, 25, 278, 1);
        System.out.println(";WARNING: hardcoding default rpdo 1&2 node ids to 0x66");
        standardDict.insert(OdEntry.pdoFactory(5120, "RPDO 1", 486, 255, 0, 0));
        standardDict.insert(OdEntry.pdoMappingFactory(5632, "RPDO 1 mapping parameter", 1644167176));
        standardDict.insert(OdEntry.pdoFactory(5121, "RPDO 2", 742, 255, 0, 0));
        standardDict.insert(OdEntry.pdoMappingFactory(5633, "RPDO 2 mapping parameter", 1678835984, 1678836240, 1678836496));
        System.out.println(";WARNING: hardcoding default rpdo 3&4 node ids to 0x33");
        standardDict.insert(OdEntry.pdoFactory(5122, "RPDO 3", 435, 255, 0, 0));
        standardDict.insert(OdEntry.pdoMappingFactory(5634, "RPDO 3 mapping parameter", 1644167176));
        standardDict.insert(OdEntry.pdoFactory(5123, "RPDO 4", 691, 255, 0, 0));
        standardDict.insert(OdEntry.pdoMappingFactory(5635, "RPDO 4 mapping parameter", 1678835984, 1678836240, 1678836496));
        for (int i = 0; i < 4; ++i) {
            standardDict.insert(OdEntry.pdoFactory(6144 + i, "TPDO " + (i + 1), (i + 2) * 256 | n, 255, 0, 0));
            standardDict.insert(OdEntry.pdoMappingFactory(6656 + i, "TPDO " + (i + 1) + "mapping parameter", 1677787152 + 256 * i));
        }
        final OdEntry odEntry = new OdEntry(24576, "Read Inputs");
        standardDict.insert(odEntry);
        odEntry.appendSub(new SubEntry(SubEntry.AccessType.RO, "Inputs 0x1 to 0x8", (byte)(-1)));
        final OdEntry odEntry2 = new OdEntry(24578, "Polarity Input");
        standardDict.insert(odEntry2);
        odEntry2.appendSub(new SubEntry(SubEntry.AccessType.RW, "Inputs 0x1 to 0x8", (byte)0));
        final OdEntry odEntry3 = new OdEntry(25088, "Write Outputs");
        standardDict.insert(odEntry3);
        odEntry3.appendSub(new SubEntry(SubEntry.AccessType.RW, "Outputs 0x1 to 0x8", (byte)(-1)));
        final OdEntry odEntry4 = new OdEntry(25090, "Change Polarity Outputs");
        standardDict.insert(odEntry4);
        odEntry4.appendSub(new SubEntry(SubEntry.AccessType.RW, "Outputs 0x1 to 0x8", (byte)0));
        final OdEntry odEntry5 = new OdEntry(25094, "Error Mode Outputs");
        standardDict.insert(odEntry5);
        odEntry5.appendSub(new SubEntry(SubEntry.AccessType.RW, "Outputs 0x1 to 0x8", (byte)0));
        final OdEntry odEntry6 = new OdEntry(25095, "Error Value Outputs");
        standardDict.insert(odEntry6);
        odEntry6.appendSub(new SubEntry(SubEntry.AccessType.RW, "Outputs 0x1 to 0x8", (byte)0));
        final OdEntry odEntry7 = new OdEntry(25601, "Analog Input");
        standardDict.insert(odEntry7);
        odEntry7.appendSub(new SubEntry(SubEntry.AccessType.RO, "Analog In 1", (short)4369));
        odEntry7.appendSub(new SubEntry(SubEntry.AccessType.RO, "Analog In 2", (short)8738));
        odEntry7.appendSub(new SubEntry(SubEntry.AccessType.RO, "Analog In 3", (short)13107));
        odEntry7.appendSub(new SubEntry(SubEntry.AccessType.RO, "Analog In 4", (short)17476));
        final OdEntry odEntry8 = new OdEntry(25617, "Analog Output");
        standardDict.insert(odEntry8);
        odEntry8.appendSub(new SubEntry(SubEntry.AccessType.RW, "Out 1", (short)4369));
        odEntry8.appendSub(new SubEntry(SubEntry.AccessType.RW, "Out 2", (short)8738));
        odEntry8.appendSub(new SubEntry(SubEntry.AccessType.RW, "Out 3", (short)13107));
        odEntry8.appendSub(new SubEntry(SubEntry.AccessType.RW, "Out 4", (short)17476));
        final OdEntry odEntry9 = new OdEntry(25670, "Analog Output Offset");
        standardDict.insert(odEntry9);
        odEntry9.appendSub(new SubEntry(SubEntry.AccessType.RW, "Offset 1", 0));
        odEntry9.appendSub(new SubEntry(SubEntry.AccessType.RW, "Offset 2", 0));
        odEntry9.appendSub(new SubEntry(SubEntry.AccessType.RW, "Offset 3", 0));
        odEntry9.appendSub(new SubEntry(SubEntry.AccessType.RW, "Offset 4", 0));
        final OdEntry odEntry10 = new OdEntry(25671, "Analog Output Scale");
        standardDict.insert(odEntry10);
        odEntry10.appendSub(new SubEntry(SubEntry.AccessType.RW, "Scale 1", 65535));
        odEntry10.appendSub(new SubEntry(SubEntry.AccessType.RW, "Scale 2", 65535));
        odEntry10.appendSub(new SubEntry(SubEntry.AccessType.RW, "Scale 3", 65535));
        odEntry10.appendSub(new SubEntry(SubEntry.AccessType.RW, "Scale 4", 65535));
        return standardDict;
    }
}
