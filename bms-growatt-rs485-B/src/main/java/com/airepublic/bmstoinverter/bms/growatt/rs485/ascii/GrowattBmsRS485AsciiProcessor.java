package com.airepublic.bmstoinverter.bms.growatt.rs485.ascii;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.bmstoinverter.core.BMS;
import com.airepublic.bmstoinverter.core.NoDataAvailableException;
import com.airepublic.bmstoinverter.core.Port;
import com.airepublic.bmstoinverter.core.TooManyInvalidFramesException;
import com.airepublic.bmstoinverter.core.bms.data.BatteryPack;
import com.airepublic.bmstoinverter.core.util.ByteAsciiConverter;
import com.airepublic.bmstoinverter.core.util.HexUtil;

/**
 * A minimal ASCII/hex RS485 BMS processor for Growatt inverter complement protocol.
 * This implements the common ASCII hex framing used in other modules in the repo.
 *
 * Note: Field/command mappings may require tuning to match the inverter. Please run with
 * DEBUG logging enabled and provide captured request frames if adjustments are needed.
 */
@ApplicationScoped
public class GrowattBmsRS485AsciiProcessor extends BMS {
    private final static Logger LOG = LoggerFactory.getLogger(GrowattBmsRS485AsciiProcessor.class);
    private final static int BATTERY_ID = 0;

    @Override
    protected void collectData(final Port port) throws TooManyInvalidFramesException, NoDataAvailableException, IOException {
        final ByteBuffer sendFrame = prepareSendFrame();

        // send request frame (some inverters actively poll; others just listen)
        if (sendFrame != null) {
            LOG.debug("SENDING: {}", Port.printBuffer(sendFrame));
            port.sendFrame(sendFrame);
        }

        // read frames
        final List<ByteBuffer> frames = readFrames(port);

        if (frames == null || frames.isEmpty()) {
            return;
        }

        final BatteryPack pack = getBatteryPack(BATTERY_ID);

        // parse each frame and populate BatteryPack
        for (final ByteBuffer frame : frames) {
            LOG.debug("RECEIVED: {}", Port.printBuffer(frame));

            // basic validation: frame must have at least 4 bytes (start + cid + len + checksum)
            if (frame == null || frame.capacity() < 6) {
                continue;
            }

            // Example ASCII parsing: skip start flag if present
            int pos = 0;
            final byte start = frame.get(pos);
            if (start == (byte) '~' || start == 0x7E) {
                pos++;
            }

            // get command id (next byte)
            final byte cid = frame.get(pos++);

            // get length (next byte)
            final byte len = frame.get(pos++);

            // get data
            final int dataLen = len & 0xFF;
            final byte[] data = new byte[dataLen];
            for (int i = 0; i < dataLen && pos < frame.capacity(); i++) {
                data[i] = frame.get(pos++);
            }

            // compute a basic checksum if present (last byte)
            // This is a generic placeholder; actual devices may use other checksums
            // Validate if possible

            // parse known CIDs (best-effort mapping)
            switch (cid & 0xFF) {
                case 0x60: // system info / battery info (example)
                {
                    final ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
                    // expect SOC (2 ascii bytes -> short) 0.1% -> packSOC
                    final byte[] shortBuf = new byte[4];
                    if (bb.remaining() >= 4) {
                        bb.get(shortBuf);
                        pack.packSOC = ByteAsciiConverter.convertAsciiBytesToShort(shortBuf) / 10;
                    }
                    if (bb.remaining() >= 4) {
                        bb.get(shortBuf);
                        pack.packVoltage = ByteAsciiConverter.convertAsciiBytesToShort(shortBuf) / 10;
                    }
                    if (bb.remaining() >= 4) {
                        bb.get(shortBuf);
                        pack.packCurrent = ByteAsciiConverter.convertAsciiBytesToShort(shortBuf);
                    }
                }
                break;
                case 0x47: // limits
                {
                    // ignore for now
                }
                break;
                case 0x42: // alarms
                {
                    // parse simple alarm flag
                    if (data.length > 0) {
                        final byte alarm = data[0];
                        if (alarm != 0) {
                            // set a generic alarm in the BatteryPack (map to ALARM enum elsewhere)
                            pack.alarms.put(com.airepublic.bmstoinverter.core.bms.data.Alarm.PACK_VOLTAGE_HIGH, com.airepublic.bmstoinverter.core.AlarmLevel.ALARM);
                        }
                    }
                }
                break;
                default:
                    LOG.debug("Unhandled CID {} ({} bytes)", cid & 0xFF, data.length);
                break;
            }
        }

        LOG.debug("Updated BatteryPack: {}", pack);
    }

    private List<ByteBuffer> readFrames(final Port port) throws IOException, TooManyInvalidFramesException, NoDataAvailableException {
        final List<ByteBuffer> readBuffers = new ArrayList<>();
        int noDataReceived = 0;

        do {
            final ByteBuffer receiveBuffer = port.receiveFrame();
            LOG.debug("RECV: {}", Port.printBuffer(receiveBuffer));

            if (receiveBuffer == null) {
                noDataReceived++;
                if (noDataReceived > 10) {
                    throw new NoDataAvailableException();
                }

                try {
                    Thread.sleep(50);
                } catch (final InterruptedException e) {
                }
                continue;
            }

            readBuffers.add(receiveBuffer);

            // break early if inverter sends one-shot
            break;
        } while (true);

        return readBuffers;
    }

    private ByteBuffer prepareSendFrame() {
        // Minimal placeholder: many inverters actively poll/none poll. Return null to not send.
        return null;
    }
}
