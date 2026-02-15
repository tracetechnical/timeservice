package uk.co.tracetechnical.timeapi

import org.eclipse.paho.mqttv5.client.MqttClient
import org.eclipse.paho.mqttv5.client.MqttClientException.REASON_CODE_CLIENT_NOT_CONNECTED
import org.eclipse.paho.mqttv5.client.MqttClientException.REASON_CODE_CONNECTION_LOST
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.springframework.stereotype.Service
import java.util.*

@Service
class MqttService(private val shutdownService: ShutdownService) {

    private val broker = "tcp://192.168.10.229:1883"
    private val clientId = "TimeService-" + UUID.randomUUID()
    private val connOpts = MqttConnectionOptions()
    private val txPersistence = MemoryPersistence()
    private var txClient: MqttClient? = null

    private val maxRetries = 5
    private val retryDelayMs = 2000L

    init {
        connOpts.isAutomaticReconnect = true
        connOpts.isCleanStart = true
        connOpts.connectionTimeout = 30
        connOpts.keepAliveInterval = 20

        try {
            txClient = MqttClient(broker, "$clientId-tx", txPersistence)
            connectTxWithRetry()
        } catch (me: MqttException) {
            log("FATAL: Failed to create MQTT client", me)
            shutdownService.shutdown(1)
            throw IllegalStateException("Failed to create MQTT client", me)
        }
    }

    /**
     * Initial connection with retry and clear logging
     */
    private fun connectTxWithRetry() {
        for (attempt in 1..maxRetries) {
            try {
                log("MQTT Tx connect attempt $attempt of $maxRetries to $broker")
                txClient!!.connect(connOpts)
                log("MQTT Tx CONNECTED")
                return
            } catch (me: MqttException) {
                log("MQTT Tx CONNECT FAILED (attempt $attempt)", me)

                if (attempt == maxRetries) {
                    log("MQTT Tx FAILED after $maxRetries attempts — shutting down")
                    shutdownService.shutdown(2)
                    throw IllegalStateException("Unable to connect to MQTT broker", me)
                }

                log("MQTT Tx retrying connect in ${retryDelayMs / 1000}s")
                Thread.sleep(retryDelayMs)
            }
        }
    }

    /**
     * Publish with reconnect + retry if disconnected
     */
    fun publish(topic: String, content: String, retain: Boolean) {
        val message = MqttMessage(content.toByteArray()).apply {
            qos = 0
            isRetained = retain
        }

        for (attempt in 1..maxRetries) {
            try {
                if (txClient == null) {
                    log("MQTT Tx client is NULL — cannot publish")
                    shutdownService.shutdown(3)
                    return
                }

                if (!txClient!!.isConnected) {
                    log("MQTT Tx DISCONNECTED before publish (attempt $attempt)")
                    connectTxWithRetry()
                }

                txClient!!.publish(topic, message)
                log("MQTT Tx PUBLISHED to [$topic]")
                return
            } catch (me: MqttException) {
                log("MQTT Tx PUBLISH FAILED (attempt $attempt)", me)

                if (!isRecoverable(me) || attempt == maxRetries) {
                    log("MQTT Tx UNRECOVERABLE publish failure — shutting down")
                    shutdownService.shutdown(3)
                    return
                }

                log("MQTT Tx retrying publish in ${retryDelayMs / 1000}s")
                Thread.sleep(retryDelayMs)
            }
        }
    }

    /**
     * Explicit disconnect with logging
     */
    fun disconnect() {
        try {
            if (txClient?.isConnected == true) {
                log("MQTT Tx DISCONNECT requested")
                txClient!!.disconnect()
                log("MQTT Tx DISCONNECTED cleanly")
            } else {
                log("MQTT Tx disconnect requested but client already disconnected")
            }
        } catch (me: MqttException) {
            log("MQTT Tx DISCONNECT FAILED", me)
        }
    }

    /**
     * Determines if exception is recoverable via reconnect
     */
    private fun isRecoverable(me: MqttException): Boolean {
        val reason = me.reasonCode.toShort()
        return reason == REASON_CODE_CLIENT_NOT_CONNECTED ||
               reason == REASON_CODE_CONNECTION_LOST
    }

    /**
     * Centralised, consistent logging
     */
    private fun log(message: String, me: MqttException? = null) {
        val prefix = "[MQTT]"
        if (me == null) {
            println("$prefix $message")
        } else {
            println("$prefix $message")
            println("$prefix Reason code : ${me.reasonCode}")
            println("$prefix Message     : ${me.message}")
            println("$prefix Cause       : ${me.cause}")
            me.printStackTrace()
        }
    }
}
