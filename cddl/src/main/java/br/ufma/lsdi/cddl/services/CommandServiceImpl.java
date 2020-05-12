package br.ufma.lsdi.cddl.services;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import br.pucrio.inf.lac.mhub.components.MOUUID;
import br.pucrio.inf.lac.mhub.models.base.ActuatorMessage;
import br.pucrio.inf.lac.mhub.util.MHubEventBus;
import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.message.CommandMessage;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.pubsub.Subscriber;
import br.ufma.lsdi.cddl.pubsub.SubscriberFactory;
import br.ufma.lsdi.cddl.util.CDDLEventBus;
import lombok.val;

/**
 * Created by lcmuniz on 05/03/17.
 */
public class CommandServiceImpl {

    private Subscriber subscriber;

    private ISubscriberListener subscriberListener = new ISubscriberListener() {
        @Override
        public void onMessageArrived(Message message) {

            if (message instanceof CommandMessage) {
                val commandMessage = (CommandMessage) message;
                sendActuatorMessageToS2PA(commandMessage);
            }

        }
    };

    public CommandServiceImpl() {

        if (!CDDLEventBus.getDefault().isRegistered(this)) {
            CDDLEventBus.getDefault().register(this);
        }

        subscriber = SubscriberFactory.createSubscriber();
        subscriber.addConnection(CDDL.getInstance().getConnection());
        subscriber.subscribeCommandTopic();
        subscriber.setSubscriberListener(subscriberListener);

    }

    public void close() {
        if (CDDLEventBus.getDefault().isRegistered(this)) {
            CDDLEventBus.getDefault().unregister(this);
        }

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(final CommandMessage commandMessage) {
        sendActuatorMessageToS2PA(commandMessage);
    }

    private void sendActuatorMessageToS2PA(CommandMessage commandMessage) {
         val moouid = MOUUID.fromString(commandMessage.getMouuid());
         MHubEventBus.getDefault().post(new ActuatorMessage(commandMessage.getTo(), moouid, commandMessage.getServiceName(), commandMessage.getServiceValue()));
    }
}
