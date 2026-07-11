package com.airepublic.bmstoinverter.bms.growatt.rs485;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.rs485.JSerialCommPort;
import com.airepublic.bmstoinverter.core.protocol.rs485.FrameDefinition;
import com.fazecast.jSerialComm.SerialPort;

/**
 * The {@link BMSDescriptor} for the Growatt {@link BMS} using the RS485 protocol.
 */
public class GrowattBmsRS485Descriptor implements BMSDescriptor {
    @Override
    public String getName() {
        return "GROWATT_RS485";
    }


    @Override
    public int getDefaultBaudRate() {
        return 500000;
    }


    @Override
    public Class<? extends BMS> getBMSClass() {
        return GrowattBmsRS485Processor.class;
    }


    @Override
    public Port createPort(final BMSConfig config) {
        // adjust startFlag and FrameDefinition to match the inverter's request framing
        final byte[] startFlag = new byte[] { (byte) 0x7E };
        final Port port = new JSerialCommPort(config.getPortLocator(), config.getBaudRate(), 8, 1, SerialPort.NO_PARITY, startFlag, FrameDefinition.create("SOOOOAAAAOOOOOOOOOOOOLLLLOOOODOOOOCCCCOO"));
        return port;
    }
}
