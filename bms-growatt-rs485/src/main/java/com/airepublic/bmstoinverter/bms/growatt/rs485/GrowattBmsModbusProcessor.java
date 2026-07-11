package com.airepublic.bmstoinverter.bms.growatt.rs485;

import java.nio.ByteBuffer;

import javax.enterprise.context.ApplicationScoped;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.PortAllocator;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.protocol.modbus.J2ModSlavePort;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Modbus slave BMS implementation exposing Growatt registers so a Growatt inverter acting as Modbus master
 * can read the values.
 */
@ApplicationScoped
public class GrowattBmsModbusProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattBmsModbusProcessor.class);

    @Override
    protected void collectData(final com.airepublic.bmstoinverter.core.Port port) {
        // nothing to do for Modbus slave here; values are set in createSendFrames
    }

    @Override
    protected java.util.List<ByteBuffer> createSendFrames(final ByteBuffer requestFrame, final BatteryPack aggregatedPack) {
        // allocate the modbus slave processing image for the port
        final J2ModSlavePort port = (J2ModSlavePort) PortAllocator.allocate(getPortLocator());
        final SimpleProcessImage spi = port.getProcessingImage();

        // set battery info (same mapping as inverter-growatt-modbus expects)
        spi.setInputRegister(1083, new SimpleInputRegister(getBMSStatus(aggregatedPack)));
        spi.setInputRegister(1085, new SimpleInputRegister(getAlarms(aggregatedPack)));
        spi.setInputRegister(1086, new SimpleInputRegister(aggregatedPack.packSOC / 10));
        spi.setInputRegister(1087, new SimpleInputRegister(aggregatedPack.packVoltage * 10));
        spi.setInputRegister(1088, new SimpleInputRegister(aggregatedPack.packCurrent * 10));
        spi.setInputRegister(1089, new SimpleInputRegister(aggregatedPack.tempAverage / 10));
        spi.setInputRegister(1090, new SimpleInputRegister(aggregatedPack.maxPackChargeCurrent / 10));
        spi.setInputRegister(1091, new SimpleInputRegister(aggregatedPack.remainingCapacitymAh / 10));
        spi.setInputRegister(1092, new SimpleInputRegister(aggregatedPack.ratedCapacitymAh / 10));
        spi.setInputRegister(1094, new SimpleInputRegister(0)); // delta voltage
        spi.setInputRegister(1095, new SimpleInputRegister(aggregatedPack.bmsCycles));
        spi.setInputRegister(1096, new SimpleInputRegister(aggregatedPack.packSOH));

        return null;
    }

    private int getBMSStatus(final BatteryPack aggregatedPack) {
        int status = 0;

        // Use same bitfield mapping as inverter-growatt-modbus
        // dis-/charging status
        switch (aggregatedPack.chargeDischargeStatus) {
            case 0: // idle/stationary
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 0, true);
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 1, false);
            break;
            case 1: // charging
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 0, false);
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 1, true);
            break;
            case 2: // discharging
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 0, true);
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 1, true);
            break;
            case 3: // sleep
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 0, false);
                status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 1, false);
            break;
        }

        final boolean hasErrors = aggregatedPack.alarms.values().stream().anyMatch(level -> level == com.airepublic.bmstoinverter.core.AlarmLevel.ALARM);
        status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 2, hasErrors);

        // balancing
        status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 3, aggregatedPack.cellBalanceActive);

        // battery terminal connected
        status = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(status, 7, true);

        return status;
    }

    private int getAlarms(final BatteryPack aggregatedPack) {
        // Provide a simplified alarm mapping similar to other inverter processors
        int alarms = 0;
        // Example: set bit0 if any ALARM exists
        final boolean hasErrors = aggregatedPack.alarms.values().stream().anyMatch(level -> level == com.airepublic.bmstoinverter.core.AlarmLevel.ALARM);
        alarms = com.airepublic.bmstoinverter.core.util.BitUtil.setBit(alarms, 0, hasErrors);
        return alarms;
    }
}
