package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.messaging.request.*;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.service.OrderHandler;
import ir.ramtung.tinyme.messaging.event.*;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

class OrderHandlerPropertyBaseTest extends BaseProviders {

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty0(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder) {
        orderHandler.handleEnterOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        orderHandler.handleEnterOrder(buyOrder);
        Event event2 = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent, event2.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty1(
            @ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder) {
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) <= getBrokerCredit(buyOrder.getBrokerId()));
        orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderAcceptedEvent || event instanceof OrderExecutedEvent, event.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 10)
    void testProperty2(
            @ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
        long credit = getBrokerCredit(buyOrder2.getBrokerId());
        Assume.that(buyOrder1.getPrice() <= buyOrder2.getPrice());
        Assume.that((buyOrder2.getPrice() * buyOrder2.getQuantity()) <= credit);
        orderHandler.handleEnterOrder(buyOrder1);
        Event event = getEventPublisher().getLastEvent().get();
        Assume.that(event instanceof OrderExecutedEvent);
        revertMatchEngine(buyOrder1, (OrderExecutedEvent) event);
        orderHandler.handleEnterOrder(buyOrder2);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderExecutedEvent, event2.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 10)
    void testProperty3(
            @ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
        Assume.that(buyOrder1.getPrice() >= buyOrder2.getPrice());
        orderHandler.handleEnterOrder(buyOrder1);
        Event event = getEventPublisher().getLastEvent().get();
        Assume.that(event instanceof OrderAcceptedEvent);
        Assume.that((buyOrder2.getPrice() * buyOrder2.getQuantity()) <= getBrokerCredit(buyOrder2.getBrokerId()));
        orderHandler.handleEnterOrder(buyOrder2);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderAcceptedEvent, event2.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 30)
    void testProperty4(
            @ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
        Assume.that(buyOrder1.getPrice() >= buyOrder2.getPrice());
        Assume.that(
                (buyOrder1.getPrice() * buyOrder1.getQuantity()) <= (buyOrder2.getPrice() * buyOrder2.getQuantity()));
        orderHandler.handleEnterOrder(buyOrder1);
        Event event = getEventPublisher().getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        orderHandler.handleEnterOrder(buyOrder2);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent, event2.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 400)
    void testProperty5(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) != null);
        orderHandler.handleDeleteOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderDeletedEvent, event.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO)
    void testProperty6(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) == null);
        orderHandler.handleDeleteOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent, event.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 400)
    void testProperty7(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) != null);
        orderHandler.handleDeleteOrder(buyOrder);
        orderHandler.handleDeleteOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent, event.toString());
    }

    @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 100)
    void testProperty8(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteBuyOrderRqProvider") DeleteOrderRq buyOrder) {
        Assume.that(findOrderById(buyOrder.getSide(), buyOrder.getOrderId()) != null);
        orderHandler.handleDeleteOrder(buyOrder);
        orderHandler.handleDeleteOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent, event.toString());
    }

    @Property(tries = 10000, generation = GenerationMode.AUTO, maxDiscardRatio = 30)
    void testProperty9(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("updateBuyOrderRqProvider") EnterOrderRq buyOrder) {
        Order bo = findOrderById(buyOrder.getSide(), buyOrder.getOrderId());
        Assume.that(bo != null);
        Assume.that(buyOrder.getPrice() > bo.getPrice());
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) <= getBrokerCredit(buyOrder.getBrokerId())
                + bo.getValue());
        orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderUpdatedEvent || event instanceof OrderExecutedEvent, event.toString());
    }

    @Property(tries = 10000, generation = GenerationMode.AUTO, maxDiscardRatio = 1000)
    void testProperty10(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq newBuyOrder,
            @ForAll("updateBuyOrderRqProvider") EnterOrderRq updateBuyOrder) {
        Assume.that(newBuyOrder.getOrderId() != updateBuyOrder.getOrderId());
        Assume.that(newBuyOrder.getQuantity() == updateBuyOrder.getQuantity());
        Assume.that(newBuyOrder.getPrice() == updateBuyOrder.getPrice());
        Order bo = findOrderById(updateBuyOrder.getSide(), updateBuyOrder.getOrderId());
        Assume.that(bo != null);
        orderHandler.handleEnterOrder(newBuyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        orderHandler.handleEnterOrder(newBuyOrder);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent, event.toString());
    }

}
