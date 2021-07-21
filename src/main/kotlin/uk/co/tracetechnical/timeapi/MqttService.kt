package uk.co.tracetechnical.timeapi

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.stereotype.Service

@Service
class MqttService {
    private val broker = "tcp://192.168.10.229:1883"
    private val clientId = "TimeService"
    private val connOpts = MqttConnectOptions()
    private val txPersistence = MemoryPersistence()
    private var rxClient: MqttAsyncClient? = null
    private var txClient: MqttClient? = null

    init {
        connOpts.isAutomaticReconnect = true
        connOpts.isCleanSession = true
        connOpts.connectionTimeout = 0
        try {
            txClient = MqttClient(broker, clientId + "tx", txPersistence)
            println("Connecting to broker (Tx): $broker")
            connectTx()
        } catch (me: MqttException) {
            handleException(me)
        }
    }

    private fun connectTx() {
        txClient!!.connect(connOpts)
        println("Connected")
    }

    fun publish(topic: String, content: String) {
        try {
            val message = MqttMessage(content.toByteArray())
            message.qos = 0
            txClient!!.publish(topic, message)
        } catch (me: MqttException) {
            handleException(me)
        }
    }

    fun disconnect() {
        try {
            rxClient!!.disconnect()
        } catch (me: MqttException) {
            handleException(me)
        }
    }

    private fun handleException(me: MqttException) {
        println("reason " + me.reasonCode)
        println("msg " + me.message)
        println("loc " + me.localizedMessage)
        println("cause " + me.cause)
        println("excep $me")
        me.printStackTrace()
    }
}