package com.roogle.simple.stetho

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.roogle.stetho.mqtt.StethoMqttClient

class MainActivity : AppCompatActivity() {

    val serverUri = "tcp://broker.emqx.io:1883"
    val clientId = "AS-VX0EB09D4737351"
    lateinit var stethoMqttClient: StethoMqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        println("clientId: ${clientId}")
        val simpleStetho = SimpleStetho(this)
//        simpleStetho.logcatProvider.startGatherLogcatInfo()
        simpleStetho.sysLogcatProvider.startGatherLogcatInfo()

        simpleStetho.databaseName = "youzi.db"

        stethoMqttClient = StethoMqttClient(simpleStetho, serverUri, clientId, "topic/client/${clientId}", "topic/server/${clientId}")
        stethoMqttClient.setUsingZip(true)
        stethoMqttClient.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        stethoMqttClient.disconnect()
    }
}
