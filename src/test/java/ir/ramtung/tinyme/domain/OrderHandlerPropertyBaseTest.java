package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.messaging.request.*;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.messaging.event.*;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderHandlerPropertyBaseTest extends BaseProviders {

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty0(@ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder) {
        this.orderHandler.handleEnterOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        this.orderHandler.handleEnterOrder(buyOrder);
        Event event2 = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty1(
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder) {
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) <= getBrokerCredit(buyOrder.getBrokerId()));
        this.orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderAcceptedEvent || event instanceof OrderExecutedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty1_2(
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder) {
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) > getBrokerCredit(buyOrder.getBrokerId()));
        this.orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 10)
    void testProperty2(
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
        long credit = getBrokerCredit(buyOrder2.getBrokerId());
        Assume.that(buyOrder1.getPrice() <= buyOrder2.getPrice());
        Assume.that((buyOrder2.getPrice() * buyOrder2.getQuantity()) <= credit);
        this.orderHandler.handleEnterOrder(buyOrder1);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderExecutedEvent);
        revertMatchEngine(buyOrder1, (OrderExecutedEvent) event, credit);
        this.orderHandler.handleEnterOrder(buyOrder2);
        Event event2 = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event2 instanceof OrderExecutedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 10)
    void testProperty3(
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
        Assume.that(buyOrder1.getPrice() >= buyOrder2.getPrice());
        this.orderHandler.handleEnterOrder(buyOrder1);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderAcceptedEvent);
        Assume.that((buyOrder2.getPrice() * buyOrder2.getQuantity()) <= getBrokerCredit(buyOrder2.getBrokerId()));
        this.orderHandler.handleEnterOrder(buyOrder2);
        Event event2 = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event2 instanceof OrderAcceptedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 30)
    void testProperty4(
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
        Assume.that(buyOrder1.getPrice() >= buyOrder2.getPrice());
        Assume.that(
                (buyOrder1.getPrice() * buyOrder1.getQuantity()) <= (buyOrder2.getPrice() * buyOrder2.getQuantity()));
        this.orderHandler.handleEnterOrder(buyOrder1);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        this.orderHandler.handleEnterOrder(buyOrder2);
        Event event2 = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 300)
    void testProperty5(@ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) != null);
        this.orderHandler.handleDeleteOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event instanceof OrderDeletedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty6(@ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) == null);
        this.orderHandler.handleDeleteOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 300)
    void testProperty7(@ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) != null);
        this.orderHandler.handleDeleteOrder(buyOrder);
        this.orderHandler.handleDeleteOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 300)
    void testProperty8(@ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) != null);
        this.orderHandler.handleDeleteOrder(buyOrder);
        this.orderHandler.handleDeleteOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent);
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty9(@ForAll("updateBuyOrderRqProvider") EnterOrderRq buyOrder) {
        Order bo = findOrderById(buyOrder.getSide(), buyOrder.getOrderId());
        Assume.that(bo != null);
        Assume.that(buyOrder.getQuantity() > bo.getQuantity() || buyOrder.getPrice() != bo.getPrice());
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) > getBrokerCredit(buyOrder.getBrokerId()));
        this.orderHandler.handleEnterOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent);
    }

}
