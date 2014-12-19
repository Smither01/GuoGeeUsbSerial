package com.guogu.getway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class SerialComm {

	private static SerialComm mInstance;

	private UsbManager mUsbManager;
	private Context context;
	private String TAG = "LZP";

	private List<UsbSerialDriver> usbDrivers;

	Protocol protocol;

	private UsbSerialDriver correctDevices;
	private boolean UsbNotExist = true;
	private List<UsbSerialPort> ports;
	private UsbSerialPort mPort;
	private UsbDeviceConnection connection;
	private SerialInputOutputManager mSerialIoManager;
	private final ExecutorService mExecutor;
	private boolean SearchFlag = false;

	private boolean m_ThreadReadFlag = false;

	private SearchTask searchThread;

	private boolean flag = true;

	private long before = 0;
	private int receivePackage = 0;
	private int sendPackage = 0;

	private volatile List<SmartNode> nodeStatusList;

	// private AdbDevice mAdbDevice;
	private SerialComm(Context context) {
		this.context = context;
		protocol = Protocol.getInstance();
		mUsbManager = (UsbManager) context
				.getSystemService(Context.USB_SERVICE);
		ports = new ArrayList<UsbSerialPort>();
		SearchFlag = true;
		mExecutor = Executors.newSingleThreadExecutor();
		searchThread = new SearchTask();
		searchThread.start();

		before = System.currentTimeMillis();

		nodeStatusList = new ArrayList<SmartNode>();
	}

	public static SerialComm getInstance(Context context) {
		if (mInstance == null) {
			synchronized (SerialComm.class) {
				if (mInstance == null) {
					mInstance = new SerialComm(context);
				}
			}
		}
		return mInstance;
	}

	public static SerialComm getInstance() {
		return mInstance;
	}

	public void stopUSB() {
		stopIoManager();
//		SearchFlag = false;
		if (mPort != null) {
			try {
				mPort.close();
			} catch (IOException e) {
				// Ignore.
			}
			mPort = null;
		}
	}

	public void write(byte[] arr, int len) {
		if (arr == null)
			return;
		if (!ports.isEmpty()) {
			if (connection == null) {
				buildConnect();
				Log.v("ASDFG", "buildConnect");
			}
			// stopIoManager();
			startIoManager(arr);
		}
	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	private void startIoManager(byte[] arrData) {
		if (mPort != null) {
			Log.i(TAG, "Starting io manager ..");
			if (mSerialIoManager == null) {
				mSerialIoManager = new SerialInputOutputManager(mPort,
						mListener);
				Log.v("LZP", "Create mSerialIoManager");
			}
			String StrHex = " ";
			for (int i = 0; i < arrData.length; i++) {
				StrHex += Integer.toHexString(arrData[i] & 0xff);
				StrHex += " ";
			}
			Log.v("SENDRECEIVE", StrHex.toUpperCase());
			receivePackage++;
			if (System.currentTimeMillis() - before > 60000) {
				before = System.currentTimeMillis();
				Log.v("AAAAA", "ReceivePackage:" + receivePackage
						+ " SendingPackage:" + sendPackage);
			}
			mSerialIoManager.writeAsync(arrData);
			mExecutor.submit(mSerialIoManager);

		}
	}
	
	public void clearTaskInThreadPool(){
		mExecutor.shutdownNow();
	}

	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			String aa = "";
			for (int i = 0; i < data.length; i++) {
				aa += Integer.toHexString(data[i] & 0xff) + " ";
			}
			aa = aa.toUpperCase(Locale.getDefault());
			Log.v("SENDRECEIVE", "Coming Back:" + aa);
			sendPackage++;
			int nLen = (data[2] & 0xff) << 8 | data[3] & 0xff;
			if (0x00aa != (data[0] & 0xff) || 0x55 != (data[1] & 0xff)
					|| nLen > 546) {
				System.out.println(0xaa + " " + 0x55);
				System.out.println("No Heda " + (data[0] & 0xff) + " "
						+ (data[1] & 0xff) + " " + nLen);
				// 抛弃该帧
			} else {
				ISmartFrame ReadFrame = null;
				ReadFrame = new ISmartFrame(data);
				if(data[32] != 0 && data[33] != 0)//SEQ = 0 为APP自动查询返回值，只做更新出来，不发送
				{
					Log.v("LZP","手机查询返回");
					Protocol.getInstance().DealSerialFrame(ReadFrame);
				}
				try{
					addQueryRequest(ReadFrame);//查询若是查询结果则~
				}catch(Exception e)
				{
					Log.v("AAAAA", "addQueryRequest:"+e.toString());
				}
				

			}
		}
	};
	
	public List<SmartNode> getNodeStatusList()
	{
		return this.nodeStatusList;
	}
	public void setEmptyNodeList()
	{
		this.nodeStatusList.clear();
	}

	// 查询某个节点状态,若原不存在，则插入List中，若存在则更新
	public void addQueryRequest(ISmartFrame frame) {// 地址2个字节 1个字节的类型 1个字节的状态
													// 1个字节的时间
		boolean hasFlag = true;
		SmartNode node = new SmartNode();
		switch (frame.GetDev()) {
		case SmartNode.PROTOCOL_TYPE_COLORLIGHT:
			SmartNode.GetItemFromColorLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_ONELIGNT:
			SmartNode.GetItemFromOneLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_TWOLIGNT:
			SmartNode.GetItemFromTwoLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_THREELIGNT:
			SmartNode.GetItemFromThreeLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_FOURLIGNT:
			SmartNode.GetItemFromFourLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_POWERSOCKET:
			SmartNode.GetItemFromColorLight(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_CONTROLSOCKET:
			SmartNode.GetItemFromControlSocket(frame, node);
			break;
		case SmartNode.PROTOCOL_TYPE_GATEWAY:/* 网关不提取节点信息 */
			return;
		default:
			SmartNode.GetItemFromAny(frame, node);
			break;
		}
		if(nodeStatusList.size() > 0){
			for (SmartNode nodeTemp : nodeStatusList) {
				if(Arrays.equals(frame.GetSourceMac(), nodeTemp.getMac())){
					//若存在，更新状态与时间
					nodeTemp.setStatus(frame.GetData()[0]);
					nodeTemp.setTime(System.currentTimeMillis());
					hasFlag = false;
					break;
				}
			}
		}		
		if (hasFlag) {
			// Add into nodeStatusList
			nodeStatusList.add(node);
		}

	}

	private void buildConnect() {
		if (mUsbManager.hasPermission(mPort.getDriver().getDevice())) {
			connection = mUsbManager.openDevice(mPort.getDriver().getDevice());
			try {
				// mPort
				mPort.open(connection);
				mPort.setParameters(38400, 8, UsbSerialPort.STOPBITS_1,
						UsbSerialPort.PARITY_NONE);
			} catch (IOException e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				try {
					mPort.close();
				} catch (IOException e2) {
					// Ignore.
				}
				mPort = null;
				return;
			}
		}
	}

	// 定时查询Serial USB 串口,发现有数据就往串口中写入
	public class SearchTask extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			// SystemClock.sleep(1000);
			while (!Thread.currentThread().isInterrupted()) {
				while (SearchFlag) {
			//		Log.d(TAG, "Refreshing device list ...");
					try {
						if (protocol.getDataPackageSize() > 20) {
							Thread.sleep(30);
						} else {
							Thread.sleep(50);
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (usbDrivers != null) {
						usbDrivers = null;
					}
					usbDrivers = UsbSerialProber.getDefaultProber()
							.findAllDrivers(mUsbManager);

					int size = usbDrivers.size();
					int count = 0;
					if (usbDrivers.size() == 0) {
						if (correctDevices != null) {
							Log.v("LZP", "count:" + count + " size:" + size);
							UsbNotExist = true;
							correctDevices = null;
							ports.clear();
							m_ThreadReadFlag = false;
							stopUSB();
							Log.v("LZP", "Devices Removed");
							// stop();
						}
					}
					for (final UsbSerialDriver driver : usbDrivers) {
						count++;
						if (correctDevices == null) {
							Log.v("LZP", "correctDevices is null");
							if (driver.getDevice().getVendorId() == 4292
									&& driver.getDevice().getProductId() == 60000) {
								correctDevices = driver;
								UsbNotExist = false;
								m_ThreadReadFlag = true;
								ports.addAll(driver.getPorts());
								Log.v("LZP", "Devices Connected");
								// 建立连接
								mPort = ports.get(0);
								buildConnect();
								break;
							}
						} else {
							if (driver.getDevice().getDeviceId() == correctDevices
									.getDevice().getDeviceId()
									&& driver.getDevice().getVendorId() == correctDevices
											.getDevice().getVendorId()
									&& driver.getDevice().getProductId() == correctDevices
											.getDevice().getProductId()) {
								UsbNotExist = false;
								m_ThreadReadFlag = true;
								// Log.v("LZP", "Devices did not Removed");
								// 读取一条信息 并删除
								byte[] arr = protocol.getFirstDataByte();
								if (arr.length > 0) {
									write(arr,
											protocol.getFirstDataByte().length);
								}
								/*
								 * Log.v("LZP", "mUsbManager:" + (mUsbManager ==
								 * null ? true : false)); Log.v("LZP",
								 * "mPort.getDriver().getDevice():" +
								 * (mPort.getDriver() .getDevice() == null ?
								 * true : false)); Log.v("LZP", "connection:" +
								 * (connection == null ? true : false));
								 */
								break;
							}
							if (size == count) {
								m_ThreadReadFlag = false;
								UsbNotExist = true;
								correctDevices = null;
								ports.clear();
								stopUSB();
								// stop();
								Log.v("LZP", "Devices Removed");
							}
						}
						/*
						 * Log.v("LZP", "getDeviceId:" +
						 * driver.getDevice().getDeviceId() + " getProductId:" +
						 * driver.getDevice().getProductId() + " getVendorId:" +
						 * driver.getDevice().getVendorId());
						 * 
						 * Log.d(TAG, String.format("+ %s: %s port%s", driver,
						 * Integer.valueOf(ports.size()), ports.size() == 1 ? ""
						 * : "s"));
						 */

					}
				}
			}
		}
	}

	public boolean getUsbNotExist() {
		return UsbNotExist;
	}

}
