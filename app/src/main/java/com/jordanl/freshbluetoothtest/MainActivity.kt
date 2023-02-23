package com.jordanl.freshbluetoothtest

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var devices: Set<BluetoothDevice>? = null
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothESP: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket


    override fun onCreate(savedInstanceState: Bundle?) {

        // Initialisation du Bluetooth
        initBLE()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val monButton = findViewById<Button>(R.id.btnPress)
        monButton.setOnClickListener {
            sendMessage("HELLOOO WORRRLD")
        }
    }

    private fun initBLE(){
        bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // On vérifie que l'appareil dispose du bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Appareil pas compatible", Toast.LENGTH_SHORT).show()
        } else {
            // On vérifie que le bluetooth est activé
            if (!bluetoothAdapter.isEnabled) {

                val activeBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                val startActivityForResult = registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result: ActivityResult ->
                    if (result.resultCode == RESULT_OK) {
                        val data = result.data
                        println(data)
                    }
                }

                Toast.makeText(this, "Bluetooth NON activé", Toast.LENGTH_SHORT).show()
                startActivityForResult.launch(activeBluetooth)
                connectToDuino(bluetoothAdapter)
            } else {
                Toast.makeText(this, "uwu!", Toast.LENGTH_SHORT).show()
                // Verification de permission bluetooth
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    connectToDuino(bluetoothAdapter)
                }
            }
        }
    }

    private fun connectToDuino(bluetoothAdapter: BluetoothAdapter){
        //val macAdressArduino = "CC:50:E3:BF:BB:1A"
        val nameArduino = "ESP32test"
        val serviceUUIDArduino = "00001101-0000-1000-8000-00805f9b34fb"
        println("connectToDuino")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("Permission Check")
            //Pour obtenir la liste des périphériques
            devices = bluetoothAdapter.bondedDevices

            // On parcours la liste des appareils
            for (bluetoothDevice in (devices as MutableSet<BluetoothDevice>?)!!) {
                // On un appareil spécifique
                //if (bluetoothDevice.name == nameArduino  && bluetoothDevice.address == macAdressArduino){
                if (bluetoothDevice.name == nameArduino){
                    // On copie les infos du périph
                    bluetoothESP = bluetoothDevice
                    println("Device found !")
                }
            }
            val deviceName = bluetoothESP.name
            val deviceUuid =  getUuid(bluetoothESP.uuids)
            println("Connexion à $deviceName")
            println("Uuid est $deviceUuid")

            /**
             * createRfcommSocketToServiceRecord -> est plus sécure
             * createInsecureRfcommSocketToServiceRecord -> non crypté
             */
            bluetoothSocket = bluetoothESP.createRfcommSocketToServiceRecord(UUID.fromString(serviceUUIDArduino))

            // On se connect
            bluetoothSocket.connect()
        }
    }

    private fun sendMessage(text: String) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    println("Envoie de MSG")
                    val outputStream = bluetoothSocket.outputStream
                    outputStream.write(text.toByteArray())
                    outputStream.flush()
                } catch (e: IOException) {
                    println("Echec envoie de MSG")
                    e.printStackTrace()
                }
                return
            }

        } catch (e: IOException) {
            println("Echec Connexion")
            e.printStackTrace()
        }
    }

    private fun getUuid(data: Array<ParcelUuid>): ArrayList<UUID> {
        val uuidListe = arrayListOf<UUID>()
        for(uuid in data ) {
            uuidListe.add(uuid.uuid)
        }
        return uuidListe
    }
}