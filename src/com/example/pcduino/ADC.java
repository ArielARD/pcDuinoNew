package com.example.pcduino;

import java.io.File;
import java.io.FileInputStream;

public class ADC {
    final static String ANALOG_PIN_DIR = "/proc/";
    final static String[] AVALABLE_ANALOGS = {"adc0", "adc1", "adc2", "adc3", "adc4", "adc5"};

    public int analogRead(int pin) throws Exception {
        int bytesRead = 0;
        int bytesToRead = 12;
        byte[] buffer = new byte[bytesToRead];

        String result;
        try {
            File f = new File(ANALOG_PIN_DIR + AVALABLE_ANALOGS[pin]);
            FileInputStream fis = new FileInputStream(f);

            while (bytesRead < bytesToRead) {
                int read = fis.read(buffer, bytesRead, bytesToRead - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }

            result = new String(buffer);
            if (result.contains(":")) {
                result = result.split(":")[1].trim();
            }

            fis.close();

            return Integer.parseInt(result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("An exception occurred in trying to read from analog pin " + AVALABLE_ANALOGS[pin] + ":\n" + e.getMessage());
        }

    }

//    public static void main(String args[]) throws Exception {
//        if (args.length != 2) {
//            System.out.println("Wrong number of args: Eg. java ADC <read|continuous-read>  <pin number>");
//            return;
//        }
//
//        ADC adc = new ADC();
//        int pin = Integer.parseInt(args[1]);
//
//        switch (args[0]) {
//            case "read":
//                System.out.println(adc.analogRead(pin));
//                break;
//            case "continuous-read":
//                while (true) {
//                    System.out.println(adc.analogRead(pin));
//                    Thread.sleep(1000);
//                }
//            default:
//                System.out.println("invalid cmd: " + args[0]);
//                break;
//
//        }
//    }

}