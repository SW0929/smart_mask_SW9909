package com.bliss.csc.smart_mask_sw9909.fragment


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bliss.csc.smart_mask_sw9909.Database.DatabaseHelper
import com.bliss.csc.smart_mask_sw9909.SensorData
import com.bliss.csc.smart_mask_sw9909.SensorData.temp_sensorVal
import com.bliss.csc.smart_mask_sw9909.SensorData.nosecount_sensorVal
import com.bliss.csc.smart_mask_sw9909.databinding.SettingFragmentBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class SettingFragment: Fragment() {
    var databaseHelper: DatabaseHelper? = null
    var binding: SettingFragmentBinding? = null
    lateinit var device: BluetoothDevice
    var socket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null

    private val TAG = "BluetoothExample"
    private val DEVICE_ADDRESS = "98:DA:60:05:04:8D" // 아두이노 블루투스 모듈의 MAC 주소
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // 시리얼 통신을 위한 UUID



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        binding = SettingFragmentBinding.inflate(inflater, container, false)
        val context = requireContext()
        databaseHelper = DatabaseHelper(context)

        binding!!.bluetoothOnButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    1
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH
                    ),
                    1
                )
            }
            binding!!.bluetoothOnButton.setBackgroundColor(Color.rgb(153,255,204))
            binding!!.bluetoothOffButton.setBackgroundColor(Color.WHITE)
        }
        return binding!!.root
    }

    //bluetoothStart
    //TODO bluetooth 연결 코드 작성
    fun bluetoothStart(){
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS)
            ConnectThread().start()
        }
    }

    fun bluetoothEnd(){
        binding!!.bluetoothOffButton.setOnClickListener {
            //TODO bluetooth 해제 코드 작성
            onDestroyView()
            binding!!.bluetoothOffButton.setBackgroundColor(Color.rgb(153,255,204))
            binding!!.bluetoothOnButton.setBackgroundColor(Color.WHITE)
            Log.d("off", "off is selected")
        }
    }


    //bluetooth Thread
    inner class ConnectThread : Thread() {
        @SuppressLint("MissingPermission")
        override fun run() {
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                socket?.connect()
                Log.d(TAG, "Bluetooth connection established.")

                outputStream = socket?.outputStream


                // 여기서부터 아두이노와의 통신 코드를 작성합니다.
                // 예를 들어, 아두이노로 데이터를 전송하려면 다음과 같이 사용합니다:
                val dataToSend = "s\n".toByteArray()
                outputStream?.write(dataToSend)


                // Get the input and output streams, using temp objects because
                // member streams are final

                // 아두이노로부터 데이터를 수신하려면 다음과 같이 사용합니다:
                val buffer = ByteArray(1024)

                while (true) {
                    inputStream = socket?.inputStream
                    val bytes = inputStream!!.read(buffer)
                    val data = String(buffer, 0, bytes)
                    SystemClock.sleep(3000)
                    Log.d(TAG, "Received data: $data")

                    val currentDate = getCurrentDate()
                    val length = data.length // 3:코골이1자리, 4:코골이2자리, 5:코골이3자리

                    val str_temp = data.substring(0, 2)
                    println("측정된온도값 = $str_temp")
                    val str_nosecount :String
                    val averagetemp_cal :Int

                    if (length == 3) {
                        str_nosecount = data.substring(2,3)
                        averagetemp_cal = (SensorData.temp_sensorVal + str_temp.toInt())/2
                        SensorData.temp_sensorVal = averagetemp_cal
                        SensorData.nosecount_sensorVal = str_nosecount.toInt()
                        println("코골이횟수 = $str_nosecount")
                    } else if (length == 4) {
                        str_nosecount = data.substring(2,4)
                        averagetemp_cal = (SensorData.temp_sensorVal + str_temp.toInt())/2
                        SensorData.nosecount_sensorVal = str_nosecount.toInt()
                        println("코골이횟수 = $str_nosecount")
                    } else if (length == 5) {
                        str_nosecount = data.substring(2,5)
                        averagetemp_cal = (SensorData.temp_sensorVal + str_temp.toInt())/2
                        SensorData.temp_sensorVal = str_nosecount.toInt()
                        println("코골이횟수 = $str_nosecount")
                    } else {
                        println("error data input")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Bluetooth connection failed.", e)
            }
        }
    }


    //bluetooth 해제
    override fun onDestroyView() {
        super.onDestroyView()
        try {
            val dataToSend = "e\n".toByteArray()
            outputStream?.write(dataToSend)

            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close the Bluetooth socket.", e)
        }
    }
}

//현재 날짜 구하는 Function
fun getCurrentDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDate = Date()
    return dateFormat.format(currentDate)
}