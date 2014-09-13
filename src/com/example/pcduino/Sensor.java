package com.example.pcduino;

import android.widget.EditText;
import android.widget.TextView;

public class Sensor {

	ADC 		adc;
	int 		pin;
	int 		pinValue;
	double 		volts;
	TextView 	DisText;
	
	public Sensor() {
		adc = new ADC();
		pin = 0;
		pinValue = 0;
		volts = 0;
	}

}
