package br.ufma.lsdi.cddl.message;


import br.ufma.lsdi.cddl.pubsub.Publisher;

public final class MessageSubscriber {

    private final Publisher publisher;

    public MessageSubscriber(Publisher publisher) {
        this.publisher = publisher;
    }

    public void update(Message[] messages, Message[] removed) {
        for (final Message m : messages) {
                new Thread(new Runnable() {
                @Override
                public void run() {
                    publisher.publish(m);
                }
            }
            ).start();
        }
    }

}

