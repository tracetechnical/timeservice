package uk.co.tracetechnical.timeapi

import org.eclipse.paho.mqttv5.client.MqttClient
import org.eclipse.paho.mqttv5.client.MqttClientException.REASON_CODE_CLIENT_NOT_CONNECTED
import org.eclipse.paho.mqttv5.client.MqttClientException.REASON_CODE_CONNECTION_LOST
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.springframework.stereotype.Service
import kotlin.system.exitProcess

@Service
class MqttService {
    private val broker = "tcp://mqtt.io.home:1883"
    private val clientId = "TimeService"
    private val connOpts = MqttConnectionOptions()
    private val txPersistence = MemoryPersistence()
    private var txClient: MqttClient? = null

    init {
        connOpts.isAutomaticReconnect = true
        connOpts.isCleanStart = true
        connOpts.connectionTimeout = 0
        connOpts.keepAliveInterval = 0

        try {
            txClient = MqttClient(broker, clientId + "tx", txPersistence)
            println("Connecting to broker (Tx): $broker")
            connectTx()
        } catch (me: MqttException) {
            println("Did not get a connection, exiting to restart service");
            exitProcess(1);
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
            txClient!!.disconnect()
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
        val exitCodes = listOf(REASON_CODE_CLIENT_NOT_CONNECTED, REASON_CODE_CONNECTION_LOST)
        if (exitCodes.contains(me.reasonCode.toShort())) {
            println("Client not connected, restarting service")
            exitProcess(2)
        }
    }
}
