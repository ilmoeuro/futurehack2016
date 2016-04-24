/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ilmoeuro.futurehack2016;

import java.util.ArrayList;
import java.util.List;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;

/**
 *
 * @author Ilmo Euro <ilmo.euro@gmail.com>
 */
public class JsscAccerelometerDataSource implements AccelerometerDataSource {

    private static final int BAUD_RATE = SerialPort.BAUDRATE_9600;
    private static final int DATA_BITS = SerialPort.DATABITS_8;
    private static final int STOP_BITS = SerialPort.STOPBITS_1;
    private static final int PARITY = SerialPort.PARITY_NONE;

    private final List<ErrorHandler> errorHandlers = new ArrayList<>();
    private final List<AccelerometerDataHandler> eventHandlers = new ArrayList<>();

    private SerialPort serialPort;

    @Override
    public void init(String param) {
        try {
            if (serialPort != null) {
                serialPort.removeEventListener();
                serialPort.closePort();
            }

            serialPort = new SerialPort(param);
            serialPort.openPort();
            serialPort.setParams(
                    BAUD_RATE,
                    DATA_BITS,
                    STOP_BITS,
                    PARITY);
            serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
            serialPort.addEventListener((serEvt) -> {
                    try {
                        if (serEvt.getEventType() == SerialPortEvent.RXCHAR) {
                            String[] inputs = serialPort
                                    .readString(serEvt.getEventValue())
                                    .split("\n");
                            for (String rawInput : inputs) {
                                String[] fields = rawInput.split(",");
                                try {
                                    long t = Long.parseLong(fields[0]);
                                    int a = Integer.parseInt(fields[1]);
                                    int j = Integer.parseInt(fields[2]);
                                    for (AccelerometerDataHandler handler : eventHandlers) {
                                        handler.onDataReceived(t, a, j);
                                    }
                                } catch (NumberFormatException|ArrayIndexOutOfBoundsException ex) {
                                } 
                            }
                        }
                    } catch (SerialPortException ex) {
                        for (ErrorHandler handler : errorHandlers) {
                            handler.handleError(ex.getLocalizedMessage());
                        }
                    }
            });
        } catch (SerialPortException ex) {
            for (ErrorHandler handler : errorHandlers) {
                handler.handleError(ex.getLocalizedMessage());
            }
        }
    }

    @Override
    public void close() {
        try {
            if (serialPort != null) {
                serialPort.removeEventListener();
                serialPort.closePort();
            }

            errorHandlers.clear();
            eventHandlers.clear();
        } catch (SerialPortException ex) {
            for (ErrorHandler handler : errorHandlers) {
                handler.handleError(ex.getLocalizedMessage());
            }
        }
    }

    @Override
    public void addEventHandler(AccelerometerDataHandler handler) {
        eventHandlers.add(handler);
    }

    @Override
    public void addErrorHandler(ErrorHandler handler) {
        errorHandlers.add(handler);
    }
}
