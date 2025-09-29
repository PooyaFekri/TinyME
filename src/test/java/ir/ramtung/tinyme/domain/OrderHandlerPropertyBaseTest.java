package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.messaging.request.*;
import ir.ramtung.tinyme.domain.entity.Order;
import ir.ramtung.tinyme.domain.entity.Side;
import ir.ramtung.tinyme.domain.service.OrderHandler;
import ir.ramtung.tinyme.messaging.event.*;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

class OrderHandlerPropertyBaseTest extends BaseProviders {

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO)
    void testProperty0(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder) {
        orderHandler.handleEnterOrder(buyOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        orderHandler.handleEnterOrder(buyOrder);
        Event event2 = TestSetup.eventPublisher.getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent, event2.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO)
    void testProperty1(
            @ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder) {
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) <= getBrokerCredit(buyOrder.getBrokerId()));
        orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderAcceptedEvent || event instanceof OrderExecutedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 10)
    void testProperty2(
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

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 15)
    void testProperty3(
            @ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
        long credit = getBrokerCredit(buyOrder2.getBrokerId());
        Assume.that(buyOrder1.getPrice() <= buyOrder2.getPrice());
        Assume.that((buyOrder2.getPrice() * buyOrder2.getQuantity()) <= credit);
        orderHandler.handleEnterOrder(buyOrder1);
        Event event = getEventPublisher().getLastEvent().get();
        Assume.that(event instanceof OrderExecutedEvent);
        revertMatchingEngine(buyOrder1, (OrderExecutedEvent) event);
        orderHandler.handleEnterOrder(buyOrder2);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderExecutedEvent, event2.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 30)
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

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 400)
    void testProperty5(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteOrderRqProvider") DeleteOrderRq order) {
        Assume.that(findOrderById(order.getSide(), order.getOrderId()) != null);
        orderHandler.handleDeleteOrder(order);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderDeletedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO)
    void testProperty6(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteOrderRqProvider") DeleteOrderRq order) {
        Assume.that(findOrderById(order.getSide(), order.getOrderId()) == null);
        orderHandler.handleDeleteOrder(order);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 400)
    void testProperty7(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteOrderRqProvider") DeleteOrderRq order) {
        Assume.that(findOrderById(order.getSide(), order.getOrderId()) != null);
        orderHandler.handleDeleteOrder(order);
        orderHandler.handleDeleteOrder(order);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 100)
    void testProperty8(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("deleteOrderRqProvider") DeleteOrderRq order) {
        Assume.that(findOrderById(order.getSide(), order.getOrderId()) != null);
        orderHandler.handleDeleteOrder(order);
        orderHandler.handleDeleteOrder(order);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 100)
    void testProperty9(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("updateOrderRqProvider") EnterOrderRq buyOrder) {
        Assume.that(buyOrder.getSide() == Side.BUY);
        Order bo = findOrderById(buyOrder.getSide(), buyOrder.getOrderId());
        Assume.that(bo != null);
        Assume.that(buyOrder.getPrice() > bo.getPrice());
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) <= getBrokerCredit(buyOrder.getBrokerId())
                + bo.getValue());
        orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderUpdatedEvent || event instanceof OrderExecutedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 500)
    void testProperty10(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq newBuyOrder,
            @ForAll("updateOrderRqProvider") EnterOrderRq updateOrder) {
        Assume.that(newBuyOrder.getQuantity() == updateOrder.getQuantity());
        Assume.that(newBuyOrder.getPrice() == updateOrder.getPrice());
        Order bo = findOrderById(updateOrder.getSide(), updateOrder.getOrderId());
        Assume.that(bo != null);
        orderHandler.handleEnterOrder(newBuyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        orderHandler.handleEnterOrder(newBuyOrder);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent, event2.toString());
    }

    @Property(seed = "123456", tries = 5000, generation = GenerationMode.AUTO, maxDiscardRatio = 5000)
    void testProperty13(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq newBuyOrder,
            @ForAll("updateOrderRqProvider") EnterOrderRq updateOrder) {
        Assume.that(newBuyOrder.getSide() == updateOrder.getSide());
        Order bo = findOrderById(updateOrder.getSide(),
                updateOrder.getOrderId());
        Assume.that(bo != null);
        Assume.that(bo.getPrice() != newBuyOrder.getPrice() || bo.getQuantity() < newBuyOrder.getQuantity());
        Assume.that(getBrokerCredit(bo.getBroker().getBrokerId()) < (newBuyOrder.getPrice() * newBuyOrder.getQuantity())
                - bo.getValue());
        Assume.that(newBuyOrder.getQuantity() == updateOrder.getQuantity());
        Assume.that(newBuyOrder.getPrice() == updateOrder.getPrice());
        orderHandler.handleEnterOrder(newBuyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        orderHandler.handleEnterOrder(updateOrder);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent, event2.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 100)
    void testProperty14(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("updateOrderRqProvider") EnterOrderRq updateOrder) {
        Order bo = findOrderById(updateOrder.getSide(),
                updateOrder.getOrderId());
        Assume.that(bo != null);
        Assume.that(bo.getQuantity() > updateOrder.getQuantity() && bo.getPrice() == updateOrder.getPrice());
        orderHandler.handleEnterOrder(updateOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderUpdatedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 5)
    void testProperty11(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterSellOrderRqProvider") EnterOrderRq sellOrder) {
        orderHandler.handleEnterOrder(sellOrder);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        orderHandler.handleEnterOrder(sellOrder);
        Event event2 = TestSetup.eventPublisher.getLastEvent().get();

        assertTrue(event2 instanceof OrderRejectedEvent, event2.toString());
    }

    @Property(seed = "123456", tries = 1000, generation = GenerationMode.AUTO)
    void testProperty12(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("updateOrderRqProvider") EnterOrderRq buyOrder) {
        Order bo = findOrderById(buyOrder.getSide(), buyOrder.getOrderId());
        Assume.that(bo == null);
        orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderRejectedEvent, event.toString());
    }

    @Property(seed = "123456", tries = 10000, generation = GenerationMode.AUTO, maxDiscardRatio = 10000)
    void testProperty15(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterSellOrderRqProvider") EnterOrderRq newSellOrderRq,
            @ForAll("updateOrderRqProvider") EnterOrderRq updateSellOrderRq) {
        Assume.that(updateSellOrderRq.getSide() == Side.SELL);
        orderHandler.handleEnterOrder(newSellOrderRq);
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderRejectedEvent);
        Order or = findOrderById(updateSellOrderRq.getSide(), updateSellOrderRq.getOrderId());
        Assume.that(or != null);
        Assume.that(newSellOrderRq.getPrice() == updateSellOrderRq.getPrice()
                && newSellOrderRq.getQuantity() == updateSellOrderRq.getQuantity() - or.getQuantity());
        orderHandler.handleEnterOrder(updateSellOrderRq);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderRejectedEvent, event2.toString());
    }

    @Property(seed = "123456", tries = 2000, generation = GenerationMode.AUTO, maxDiscardRatio = 2000)
    void testProperty16(@ForAll("orderHandlerProvider") OrderHandler orderHandler,
            @ForAll("enterSellOrderRqProvider") EnterOrderRq newSellOrderRq,
            @ForAll("updateOrderRqProvider") EnterOrderRq updateSellOrderRq) {
        orderHandler.handleEnterOrder(newSellOrderRq);
        Assume.that(newSellOrderRq.getOrderId() != updateSellOrderRq.getOrderId());
        Event event = TestSetup.eventPublisher.getLastEvent().get();
        Assume.that(event instanceof OrderAcceptedEvent);
        Order or = findOrderById(updateSellOrderRq.getSide(), updateSellOrderRq.getOrderId());
        Assume.that(or != null);
        Assume.that(newSellOrderRq.getPrice() == updateSellOrderRq.getPrice()
                && newSellOrderRq.getQuantity() == updateSellOrderRq.getQuantity() - or.getQuantity());
        Assume.that(updateSellOrderRq.getSide() == Side.SELL);
        revertMatchingEngine(newSellOrderRq, (OrderAcceptedEvent) event);
        orderHandler.handleEnterOrder(updateSellOrderRq);
        Event event2 = getEventPublisher().getLastEvent().get();
        assertTrue(event2 instanceof OrderUpdatedEvent, event2.toString());
    }

}
