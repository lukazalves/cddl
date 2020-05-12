package br.ufma.lsdi.cddl.pubsub;

import br.ufma.lsdi.cddl.listeners.IMessageListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberQoSListener;
import br.ufma.lsdi.cddl.message.ServiceInformationMessage;

public interface Subscriber extends Client {

    ISubscriberListener getSubscriberListener();

    ISubscriberQoSListener getSubscriberQoSListener();

    /**
     * Sets a listener for this subscriber
     * @param subscriberListener the listener to be set
     */
    void setSubscriberListener(ISubscriberListener subscriberListener);

    void setSubscriberQoSListener(ISubscriberQoSListener subscriberQoSListener);

    //void subscribeServiceTopic();

    //void unsubscribeServiceTopic();

    /**
     * Subscribe to a service
     * @param serviceName Name of the service to be subscribed.
     */
    void subscribeServiceByName(String serviceName);

    /**
     * Unsubscribe from a service
     * @param serviceName Name of the service to be unsubscribed.
     */
    void unsubscribeServiceByName(String serviceName);

    void subscribeServiceByPublisherId(String publisherId);

    void unsubscribeServiceByPublisherId(String publisherId);

    void subscribeServiceByPublisherAndName(String publisherId, String serviceName);

    void unsubscribeServiceByPublisherAndName(String publisherId, String serviceName);

    void subscribeServiceByServiceInformationMessage(ServiceInformationMessage sim);

    void unsubscribeServiceByServiceInformationMessage(ServiceInformationMessage sim);

    void subscribeQueryTopic();

    void unsubscribeQueryTopic();

    void subscribeCancelQueryTopic();

    void unsubscribeCancelQueryTopic();

    void subscribeQueryResponseTopic();

    void unsubscribeQueryResponseTopic();

    void subscribeCommandTopic();

    void unsubscribeCommandTopic();

    void subscribeEventQueryTopic();

    void unsubscribeEventQueryTopic();

    void subscribeEventQueryResponseTopic();

    void unsubscribeEventQueryResponseTopic();

    void subscribeEventQueryResponseTopicBySubscriberId(String clientId);

    void unsubscribeEventQueryResponseTopicBySunscriberId(String clientId, IMessageListener listener);

    void subscribeLivelenessTopicByPublisherId(String publisherId);

    void unsubscribeLivelenessTopicByPublisherId(String publisherId);

    void subscribeLivelenessTopic();

    void unsubscribeLivelenessTopic();

    void subscribeConnectionChangedStatusTopic(String publisherID);

    void unsubscribeConnectionChangedStatusTopic(String publisherID);

    void subscribeConnectionChangedStatusTopic();

    void unsubscribeConnectionChangedStatusTopic();

    void subscribeTopic(String topicName);

    void unsubscribeTopic(String topicName);

    void pauseSubscriptions();

    void resumeSubscriptions();

    void unsubscribeAll();

    void subscribeObjectFoundTopic();

    void subscribeObjectConnectedTopic();

    void subscribeObjectDisconnectedTopic();

    void subscribeObjectDiscoveredTopic();


}

