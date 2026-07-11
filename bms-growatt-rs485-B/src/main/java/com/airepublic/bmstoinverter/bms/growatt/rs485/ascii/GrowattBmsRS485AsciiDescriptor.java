package com.airepublic.bmstoinverter.bms.growatt.rs485.ascii;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.protocol.rs485.FrameDefinition;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.fazecast.jSerialComm.SerialPort;

public class GrowattBmsRS485AsciiDescriptor implements BMSDescriptor {

    @Override
    public String getName() {
        return "GROWATT_RS485_ASCII";
    }

    @Override
    public int getDefaultBaudRate() {
        return 9600;
    }

    @Override
    public Class<? extends BMS> getBMSClass() {
        return GrowattBmsRS485AsciiProcessor.class;
    }

    @Override
    public Port createPort(final BMSConfig config) {
        // default start flag 0x7E and a generic frame definition similar to other ASCII modules
        final Port port = new JSerialCommPort(config.getPortLocator(), config.getBaudRate(), 8, 1, SerialPort.NO_PARITY, new byte[] { (byte) 0x7E }, FrameDefinition.create("SOOAAOCCCCOLLLDVVVVO"));
        return port;
    }
}
