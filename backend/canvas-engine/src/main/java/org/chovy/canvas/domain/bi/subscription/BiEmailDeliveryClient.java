package org.chovy.canvas.domain.bi.subscription;

public interface BiEmailDeliveryClient {

    boolean configured();

    void send(BiEmailDeliveryRequest request);
}
