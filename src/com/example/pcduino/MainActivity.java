package com.example.pcduino;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.ARDrone.ConfigOption;
import com.codeminders.ardrone.NavData;
import com.codeminders.ardrone.NavDataListener;
import com.codeminders.ardrone.data.navdata.vision.VisionTag;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.sip.SipAudioCall.Listener;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	ARDrone drone = null;
	Handler handler;

	TextView roll;
	TextView pitch;
	TextView yaw;
	TextView Battery;
	TextView FlyingState;

	Sensor sensor1;
	Sensor sensor2;
	Sensor sensor3;

	boolean flagData;
	boolean flagSensor;
	boolean check;

	BluetoothAdapter mBluetoothAdapter = null;
	BluetoothDevice mmDevice = null;
	BluetoothSocket mmSocket = null;
	OutputStream mmOutputStream = null;
	InputStream mmInputStream = null;
	boolean stopWorker = false;

	String data;

	Canvas canvas;
	final int meter = 20; // for distance in canvas
	final int center = 150 / 2;
	final int barrier = 10;
	double dist;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBluetooth = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetooth, 0);
		}

		data = "";

		// 20:14:04:14:34:66
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				// Note, you will need to change this to match the name of your
				// device
				// device.getName().equals("HC-06") ||
				String btMac = device.getAddress();
				if (btMac.equals("20:14:04:14:34:66")) {
					mmDevice = device;
					break;
				}
			}
		}

		try {
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Standard
			// SerialPortService
			// ID
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
		} catch (Exception exc) {

		}

		Thread workerThread = new Thread(new Runnable() {
			public void run() {
				String accumulator = "";
				byte separator = (char) '\n';
				while (!Thread.currentThread().isInterrupted() && !stopWorker) {
					try {
						int bytesAvailable = mmInputStream.available();
						if (bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							mmInputStream.read(packetBytes);

							for (int i = 0; i < bytesAvailable; i++) {
								if (packetBytes[i] == separator) {
									ProcessArduinoData(accumulator);
									accumulator = "";
								} else
									accumulator += (char) packetBytes[i];
							}
						}
					} catch (Exception exc) {

					}
				}

				try {
					mmSocket.close();
					mmInputStream.close();
					mmOutputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Log.i("BT", "\n\n\n\nKilled !!!\n\n\n\n");
			}
		});
		workerThread.start();

		Toast.makeText(this, " ~~~~ NINY Project ~~~~ ", Toast.LENGTH_LONG)
		.show();

		sensor1 = new Sensor();
		sensor2 = new Sensor();
		sensor3 = new Sensor();

		try {
			drone = new ARDrone();
			handler = new Handler();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		findView();

		flagData = false;
		flagSensor = false;

		check = false;

		final Paint paint = new Paint();
		paint.setColor(Color.RED);

		final Paint paintBorder = new Paint();
		paintBorder.setColor(Color.GREEN);

		final Paint paintCancel = new Paint();
		paintCancel.setColor(Color.WHITE);

		Bitmap bg = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bg);
		canvas.drawLine(0, 0, 150, 0, paintBorder);
		canvas.drawLine(0, 0, 0, 150, paintBorder);
		canvas.drawLine(0, 149, 149, 149, paintBorder);
		canvas.drawLine(149, 0, 149, 149, paintBorder);

		canvas.drawPoint(center, center, paint);

		LinearLayout ll = (LinearLayout) findViewById(R.id.rect);
		ll.setBackgroundDrawable(new BitmapDrawable(bg));

		sensor1.DisText = (TextView) findViewById(R.id.Distance1);
		final Runnable run = new Runnable() {

			@Override
			public void run() {
				if (dist > 1) {
					canvas.drawLine(center - barrier,
							(float) (center - meter * dist), center
							+ barrier, (float) (center - meter
									* dist), paintBorder);
				} else {
					canvas.drawLine(center - barrier,
							(float) (center - meter * dist), center
							+ barrier, (float) (center - meter
									* dist), paint);
				}
			}
		};
		
		
		sensor1.DisText.addTextChangedListener(new TextWatcher() {

			

			@Override
			public void afterTextChanged(Editable s) {
				dist = val2mlarge((int) Double.parseDouble(sensor1.DisText
						.getText().toString()));
				handler.post(run);

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}
		});

	}

	public double val2mlarge(int val) {
		double tmp = 0;

		if (val >= 460 && val < 500) {
			tmp = (460 + 500) / val;
			return (1.2 + 1) / tmp;
		} else if (val > 420 && val <= 460) {
			tmp = (420 + 460) / val;
			return (1.4 + 1.2) / tmp;
		} else if (val > 395 && val <= 420) {
			tmp = (395 + 420) / val;
			return (1.6 + 1.4) / tmp;
		} else if (val > 375 && val <= 395) {
			tmp = (375 + 395) / val;
			return (1.8 + 1.6) / tmp;
		} else if (val > 355 && val <= 375) {
			tmp = (355 + 375) / val;
			return (2 + 1.8) / tmp;
		} else if (val > 325 && val <= 355) {
			tmp = (325 + 355) / val;
			return (2.5 + 2) / tmp;
		} else if (val <= 325) {
			tmp = (0 + 325) / val;
			return (5 + 2.5) / tmp;
		}
		return 1;
	}

	public void findView() {
		roll = (TextView) findViewById(R.id.roll);
		pitch = (TextView) findViewById(R.id.pitch);
		yaw = (TextView) findViewById(R.id.Yaw);
		Battery = (TextView) findViewById(R.id.Battery);
		FlyingState = (TextView) findViewById(R.id.FlyingState);
		sensor1.DisText = (TextView) findViewById(R.id.Distance1);
	}

	public void setSensor() {
		sensor1.adc = new ADC();
		sensor1.pin = Integer.parseInt("2");

		sensor2.adc = new ADC();
		sensor2.pin = Integer.parseInt("3");

		sensor3.adc = new ADC();
		sensor3.pin = Integer.parseInt("4");

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void Connect(View v) {
		try {
			drone.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void trim(View v) {
		try {
			drone.trim();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void disConnect(View v) {
		try {
			drone.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void playLed(View v) {
		try {
			drone.playLED(1, 10, 3);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void clear(View v) {
		try {
			drone.clearEmergencySignal();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setEmer(View v) {
		try {
			drone.sendEmergencySignal();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getSensorDis(final Sensor sensor) {
		Thread threadSen = new Thread(new Runnable() {
			public void run() {
				while (flagSensor) {
					try {
						sensor.pinValue = sensor.adc.analogRead(sensor.pin);
						sensor.volts = sensor.pinValue * 0.0012207;
						Thread.sleep(200);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		threadSen.start();
	}

	public void takeOff(View v) {
		try {
			drone.takeOff();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void Land(View v) {
		try {
			drone.land();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void OnOffData(View v) {
		if (!flagData) {
			flagData = true;
		} else {
			flagData = false;
		}
		startData();
	}

	public void startData() {
		NavDataListener nd = new NavDataListener() {
			@Override
			public void navDataReceived(final NavData nd) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						roll.setText(nd.getRoll() + "");
						pitch.setText(nd.getPitch() + "");
						Battery.setText(nd.getBattery() + "");
						yaw.setText(nd.getYaw() + "");
						FlyingState.setText(nd.getFlyingState() + "");
						// sensor1.DisText.setText(Double.toString(sensor1.volts).substring(0,5));
						sensor1.DisText.setText(data);
					}
				});
			}
		};
		if (flagData)
			drone.addNavDataListener(nd);
		else
			drone.removeNavDataListener(nd);
	}

	public void up() {
		try {
			drone.move(0, 0, 20, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void down() {
		try {
			drone.move(0, 0, -20, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void left() {
		try {
			drone.move(-20, 0, 0, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void right() {
		try {
			drone.move(20, 0, 0, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void hover(View v) {
		try {
			drone.hover();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void upPit() {
		try {
			drone.move(0, -20, 0, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void downPit() {
		try {
			drone.move(0, 20, 0, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void leftYaw() {
		try {
			drone.move(0, 0, 0, -20);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void rightYaw() {
		try {
			drone.move(0, 0, 0, 20);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void GoStraight(final int numOfPress, final int MissionTime) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < numOfPress; i++) {
					try {
						drone.move(0, 0, MissionTime, 0);
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		thread.start();
	}

	public void WaitForTask(final int seconds) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < seconds; i++) {
					try {
						drone.hover();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		thread.start();
	}

	public void Mis1(View v) {
		try {
			drone.takeOff();
			leftYaw();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void ProcessArduinoData(String data) {
		// Log.i("BT",data);
		this.data = data;
	}

}
