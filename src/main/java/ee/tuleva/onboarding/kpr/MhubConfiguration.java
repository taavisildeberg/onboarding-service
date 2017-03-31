package ee.tuleva.onboarding.kpr;

import com.ibm.jms.JMSBytesMessage;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;


@Configuration
public class MhubConfiguration {

    @Value("${mhub.host}")
    private String host;

    @Value("${mhub.port}")
    private int port;

    @Value("${mhub.queueManager}")
    private String queueManager;

    @Value("${mhub.channel}")
    private String channel;

    @Value("${mhub.inboundQueue}")
    private String inboundQueue;

    @Value("${mhub.outboundQueue}")
    private String outboundQueue;

    @Value("${mhub.trustStore}")
    private String trustStore;

    @Value("${mhub.trustStorePassword}")
    private String trustStorePassword;

    @Value("${mhub.keyStore}")
    private String keyStore;

    @Value("${mhub.keyStorePassword}")
    private String keyStorePassword;

    @Bean
    @Scope("singleton")
    public MQQueueConnectionFactory createMQConnectionFactory() {
        // it requires SSLv3 to be enabled
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        SSLContext sslContext = KeyUtils.createSSLContext(
                keyStore,
                keyStorePassword,
                trustStore,
                trustStorePassword);

        try {
            MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
            factory.setSSLSocketFactory(sslContext.getSocketFactory());
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            //factory.setSSLSocketFactory(sslContext.getSocketFactory());
            factory.setHostName(this.host);
            factory.setPort(this.port);
            factory.setQueueManager(this.queueManager);
            factory.setChannel(this.channel);
            factory.setCCSID(WMQConstants.CCSID_UTF8);
            // only cipher that works
            factory.setSSLCipherSuite("SSL_RSA_WITH_3DES_EDE_CBC_SHA");
            factory.setSSLFipsRequired(false);
            return factory;
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }


    @Bean
    public JmsTemplate createJmsTemplate(MQQueueConnectionFactory factory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        SingleConnectionFactory singleConnectionFactory = new SingleConnectionFactory();
        singleConnectionFactory.setTargetConnectionFactory(factory);
        jmsTemplate.setConnectionFactory(singleConnectionFactory);
        jmsTemplate.setDefaultDestinationName(this.outboundQueue);
        return jmsTemplate;
    }

    @Autowired(required = false)
    private MessageListener messageListener;

    @Bean
    @Scope("singleton")
    @ConditionalOnBean(MessageListener.class)
    public DefaultMessageListenerContainer createMessageListenerContainer(
            MQQueueConnectionFactory factory) {
        DefaultMessageListenerContainer defaultMessageListenerContainer = new DefaultMessageListenerContainer();
        defaultMessageListenerContainer.setConnectionFactory(factory);
        defaultMessageListenerContainer.setDestinationName(this.inboundQueue);
        defaultMessageListenerContainer.setRecoveryInterval(4000); // todo
        defaultMessageListenerContainer.setMessageListener(this.messageListener);
        return defaultMessageListenerContainer;
    }


}