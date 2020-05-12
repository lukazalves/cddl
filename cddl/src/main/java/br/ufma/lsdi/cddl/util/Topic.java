package br.ufma.lsdi.cddl.util;

/**
 * Created by bertodetacio on 26/01/17.
 */

public final class Topic {

    private static final String DOMAIN_PARTICIPANT_TOPIC = "mhub";
    private static final String QUERY_TOPIC = "query_topic";
    private static final String CANCEL_QUERY_TOPIC = "cancel_query_topic";
    private static final String QUERY_RESPONSE_TOPIC = "query_response_topic";
    private static final String COMMAND_TOPIC = "command_topic";
    private static final String EVENT_QUERY_TOPIC = "event_query_topic";
    private static final String EVENT_QUERY_RESPONSE_TOPIC = "event_query_response_topic";
    private static final String SERVICE_TOPIC = "service_topic";
    private static final String SERVICE_INFORMATION_TOPIC = "service_information_topic";
    private static final String LIVELINESS_TOPIC = "liveliness_topic";
    private static final String CONNECTION_CHANGED_STATUS_TOPIC = "connection_changed_status_topic";

    private static final String OBJECT_FOUND_TOPIC = "object_found_topic";
    private static final String OBJECT_CONNECTED_TOPIC = "object_connected_topic";
    private static final String OBJECT_DISCONNECTED_TOPIC = "object_disconnected_topic";
    private static final String OBJECT_DISCOVERED_TOPIC = "object_discovered_topic";

    public static String serviceTopic(String clientId, String serviceName) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + SERVICE_TOPIC + "/" + serviceName;
    }

    public static String serviceTopic(String serviceName) {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + SERVICE_TOPIC + "/" + serviceName;
    }

    public static String serviceTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + SERVICE_TOPIC + "/#";
    }

    public static String serviceTopic2() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + SERVICE_TOPIC + "/+";
    }

    public static String allClientsTopic(String serviceName) {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + serviceName;
    }

    public static String allServicesOf(String publisherId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + publisherId + "/" + SERVICE_TOPIC + "/#";
    }

    public static String allServicesOf2(String publisherId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + publisherId + "/" + SERVICE_TOPIC + "/+";
    }

    public static String livenessTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + LIVELINESS_TOPIC;
    }
    public static String objectFoundTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + OBJECT_FOUND_TOPIC;
    }
    public static String objectFoundTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + OBJECT_FOUND_TOPIC;
    }

    public static String objectConnectedTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + OBJECT_CONNECTED_TOPIC;
    }

    public static String objectConnectedTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + OBJECT_CONNECTED_TOPIC;
    }

    public static String objectDisconnectedTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + OBJECT_DISCONNECTED_TOPIC;
    }

    public static String objectDisconnectedTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + OBJECT_DISCONNECTED_TOPIC;
    }

    public static String objectDiscoveredTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + OBJECT_DISCOVERED_TOPIC;
    }

    public static String objectDiscoveredTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + OBJECT_DISCOVERED_TOPIC;
    }

    public static String livenessTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + LIVELINESS_TOPIC;
    }

    public static String queryTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + QUERY_TOPIC;
    }

    public static String queryTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + QUERY_TOPIC;
    }

    public static String cancelQueryTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + CANCEL_QUERY_TOPIC;
    }

    public static String cancelQueryTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + CANCEL_QUERY_TOPIC;
    }

    public static String commandTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + COMMAND_TOPIC;
    }

    public static String commandTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + COMMAND_TOPIC;
    }

    public static String queryResponseTopic(String subscriberId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + subscriberId + "/" + QUERY_RESPONSE_TOPIC;
    }

    public static String eventQueryTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + EVENT_QUERY_TOPIC;
    }

    public static String eventQueryTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + EVENT_QUERY_TOPIC;
    }
    public static String eventQueryResponseTopic(String subscriberId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + subscriberId + "/" + EVENT_QUERY_RESPONSE_TOPIC;
    }

    public static String serviceInformationTopic(String clientId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + clientId + "/" + SERVICE_INFORMATION_TOPIC;
    }
    public static String serviceInformationTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/+/" + SERVICE_INFORMATION_TOPIC;
    }

    public static String connectionChangedStatusTopic(String publisherId) {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + publisherId + "/" + CONNECTION_CHANGED_STATUS_TOPIC;
    }

    public static String connectionChangedStatusTopic() {
        return DOMAIN_PARTICIPANT_TOPIC + "/" + "+" + "/" + CONNECTION_CHANGED_STATUS_TOPIC;
    }

}
