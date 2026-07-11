package com.airepublic.bmstoinverter.bms.growatt.rs485;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.BMSConfig;
import com.airepublic.bmstoinverter.core.BMSDescriptor;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.protocol.modbus.J2ModSlavePort;

/**
 * The {@link BMSDescriptor} for a Growatt BMS exposing a Modbus slave interface (used by some inverters).
 */
public class GrowattBmsModbusDescriptor implements BMSDescriptor {
    @Override
    public String getName() {
        return "GROWATT_MODBUS_BMS";
    }

    @Override
    public int getDefaultBaudRate() {
        return 9600;
    }

    @Override
    public Class<? extends BMS> getBMSClass() {
        return GrowattBmsModbusProcessor.class;
    }

    @Override
    public Port createPort(final BMSConfig config) {
        final Port port = new J2ModSlavePort(config.getPortLocator(), config.getBaudRate());
        return port;
    }
}
