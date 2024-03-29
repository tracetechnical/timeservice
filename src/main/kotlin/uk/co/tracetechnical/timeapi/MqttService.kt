package uk.co.tracetechnical.timeapi

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.stereotype.Service

@Service
class MqttService {
    private val broker = "tcp://mqtt.io.home:1883"
    private val clientId = "TimeService"
    private val connOpts = MqttConnectOptions()
    private val txPersistence = MemoryPersistence()
    private var rxClient: MqttAsyncClient? = null
    private var txClient: MqttClient? = null

    init {
        connOpts.isAutomaticReconnect = true
        connOpts.isCleanSession = true
        connOpts.connectionTimeout = 0
        connOpts.keepAliveInterval = 0

        try {
            txClient = MqttClient(broker, clientId + "tx", txPersistence)
            println("Connecting to broker (Tx): $broker")
            connectTx()
        } catch (me: MqttException) {
            System.out.println("Did not get a connection, exiting to restart service");
            System.exit(1);
        }
    }

    private fun connectTx() {
        txClient!!.connect(connOpts)
        println("Connected")
    }

    fun publish(topic: String, content: String, retain: Boolean) {
        try {
            val message = MqttMessage(content.toByteArray())
            message.qos = 0
            message.isRetained = retain
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
        if (me.getReasonCode() === 32104) {
            println("Client not connected, restarting service")
            System.exit(2)
        }
    }
}
