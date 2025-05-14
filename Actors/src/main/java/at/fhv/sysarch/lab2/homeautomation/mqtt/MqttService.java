
package at.fhv.sysarch.lab2.homeautomation.mqtt;

import org.eclipse.paho.client.mqttv3.*;

public class MqttService {
    private final IMqttClient client;

    public MqttService(String brokerUrl, String clientId) throws MqttException {
        this.client = new MqttClient(brokerUrl, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        client.connect(options);
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        client.subscribe(topic, listener);
    }

    public void publish(String topic, String content) throws MqttException {
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(0);
        message.setRetained(true);
        client.publish(topic, message);
    }
}