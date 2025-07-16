package ir.ramtung.tinyme.domain;

import ir.ramtung.tinyme.messaging.request.*;
import ir.ramtung.tinyme.messaging.event.*;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.config.MockedJMSTestConfig;
import ir.ramtung.tinyme.domain.BaseProviders;

import net.jqwik.api.*;
import net.jqwik.spring.*;
import net.jqwik.api.Assume;

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
            @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder
    ) {
        Assume.that((buyOrder.getPrice() * buyOrder.getQuantity()) <= getBrokerCredit(buyOrder.getBrokerId()));
        this.orderHandler.handleEnterOrder(buyOrder);
        Event event = getEventPublisher().getLastEvent().get();
        assertTrue(event instanceof OrderAcceptedEvent || event instanceof OrderExecutedEvent);
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

    // @Property(tries = 1000, generation = GenerationMode.AUTO)
    // void testProperty3(
    //         @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
    //         @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
    //     Assume.that(buyOrder1.getPrice() >= buyOrder2.getPrice());
    //     TestSetup.brokerRepo.addBroker(broker);
    //     this.orderHandler.handleEnterOrder(buyOrder1);
    //     Event event = TestSetup.eventPublisher.getLastEvent().get();
    //     Assume.that(event instanceof OrderAcceptedEvent);
    //     TestSetup.brokerRepo.addBroker(broker);
    //     this.orderHandler.handleEnterOrder(buyOrder2);
    //     Event event2 = TestSetup.eventPublisher.getLastEvent().get();
    //     assertTrue(event2 instanceof OrderAcceptedEvent);
    // }
    // @Property(tries = 1000, generation = GenerationMode.AUTO, maxDiscardRatio = 20)
    // void testProperty4(
    //         @ForAll("brokerProvider") Broker broker,
    //         @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder1,
    //         @ForAll("enterBuyOrderRqProvider") EnterOrderRq buyOrder2) {
    //     long brokerCredit = broker.getCredit();
    //     Assume.that(buyOrder1.getPrice() <= buyOrder2.getPrice());
    //     Assume.that((buyOrder1.getPrice() * buyOrder1.getQuantity()) >= (buyOrder2.getPrice() * buyOrder2.getQuantity()));
    //     TestSetup.brokerRepo.addBroker(broker);
    //     this.orderHandler.handleEnterOrder(buyOrder1);
    //     Event event = TestSetup.eventPublisher.getLastEvent().get();
    //     Assume.that(event instanceof OrderExecutedEvent);
    //     revertMatchEngine(buyOrder1, (OrderExecutedEvent) event, brokerCredit);
    //     this.orderHandler.handleEnterOrder(buyOrder2);
    //     Event event2 = TestSetup.eventPublisher.getLastEvent().get();
    //     assertTrue(event2 instanceof OrderExecutedEvent);
    // }
    // @Provide
    // Arbitrary<Tuple2<Broker, EnterOrderRq>> brokerAndBuyOrderProviderHasEnoughCredit() {
    //     Arbitrary<Integer> orderQuantity = Arbitraries.integers().between(1, 1000);
    //     Arbitrary<Integer> orderPrice = Arbitraries.integers().between(1, 1000);
    //     return Combinators.combine(orderQuantity, orderPrice)
    //             .as((quantity, price) -> {
    //                 long requiredCredit = (long) quantity * price * 2;
    //                 Broker broker = Broker.builder()
    //                         .brokerId(TestSetup.brokerId)
    //                         .credit(requiredCredit)
    //                         .build();
    //                 EnterOrderRq buyOrder = EnterOrderRq.createNewOrderRq(
    //                         Arbitraries.longs().between(1, Long.MAX_VALUE).sample(),
    //                         TestSetup.security.getIsin(),
    //                         Arbitraries.longs().between(1, Long.MAX_VALUE).sample(),
    //                         LocalDateTime.now(),
    //                         Side.BUY,
    //                         quantity,
    //                         price,
    //                         TestSetup.brokerId,
    //                         TestSetup.shareholderId,
    //                         0
    //                 );
    //                 return Tuple.of(broker, buyOrder);
    //             });
    // }
    // @Property(tries = 1000, generation = GenerationMode.AUTO)
    // void testProperty5(@ForAll("brokerAndBuyOrderProviderHasEnoughCredit") Tuple2<Broker, EnterOrderRq> input) {
    //     Broker broker = input.get1();
    //     EnterOrderRq buyOrder = input.get2();
    //     TestSetup.brokerRepo.addBroker(broker);
    //     this.orderHandler.handleEnterOrder(buyOrder);
    //     Event event = TestSetup.eventPublisher.getLastEvent().get();
    //     assertTrue(event instanceof OrderAcceptedEvent | event instanceof OrderExecutedEvent);
    // }
    // @Provide
    // Arbitrary<Tuple2<Broker, Tuple2<EnterOrderRq, EnterOrderRq>>> brokerAndTwoOrdersProvider() {
    //     Arbitrary<Long> brokerCredit = Arbitraries.longs().between(10_000_000, 100_000_000);
    //     Arbitrary<Integer> baseQuantity = Arbitraries.integers().between(1, 3);
    //     Arbitrary<Integer> basePrice = Arbitraries.integers().between(1, 3);
    //     return Combinators.combine(brokerCredit, baseQuantity, basePrice)
    //             .as((credit, quantity, price) -> {
    //                 Broker broker = Broker.builder()
    //                         .brokerId(TestSetup.brokerId)
    //                         .credit(credit)
    //                         .build();
    //                 EnterOrderRq buyOrder1 = EnterOrderRq.createNewOrderRq(
    //                         Arbitraries.longs().between(1, Long.MAX_VALUE).sample(),
    //                         TestSetup.security.getIsin(),
    //                         Arbitraries.longs().between(1, Long.MAX_VALUE).sample(),
    //                         LocalDateTime.now(),
    //                         Side.BUY,
    //                         Arbitraries.integers().between(1, 5).sample(),
    //                         price,
    //                         TestSetup.brokerId,
    //                         TestSetup.shareholderId,
    //                         0
    //                 );
    //                 EnterOrderRq buyOrder2 = EnterOrderRq.createNewOrderRq(
    //                         Arbitraries.longs().between(1, Long.MAX_VALUE).sample(),
    //                         TestSetup.security.getIsin(),
    //                         Arbitraries.longs().between(1, Long.MAX_VALUE).sample(),
    //                         LocalDateTime.now(),
    //                         Side.BUY,
    //                         Arbitraries.integers().between(1, 5).sample(),
    //                         price + Arbitraries.integers().between(1, 5).sample(),
    //                         TestSetup.brokerId,
    //                         TestSetup.shareholderId,
    //                         0
    //                 );
    //                 return Tuple.of(broker, Tuple.of(buyOrder1, buyOrder2));
    //             });
    // }
    // @Property(tries = 1000, generation = GenerationMode.AUTO)
    // void testProperty6(@ForAll("brokerAndTwoOrdersProvider") Tuple2<Broker, Tuple2<EnterOrderRq, EnterOrderRq>> input) {
    //     Broker broker = input.get1();
    //     EnterOrderRq buyOrder1 = input.get2().get1();
    //     EnterOrderRq buyOrder2 = input.get2().get2();
    //     TestSetup.brokerRepo.addBroker(broker);
    //     this.orderHandler.handleEnterOrder(buyOrder1);
    //     Event event = TestSetup.eventPublisher.getLastEvent().get();
    //     Assume.that(event instanceof OrderExecutedEvent);
    //     // revertMatchEngine(buyOrder1, (OrderExecutedEvent) event, brokerCredit);
    //     this.orderHandler.handleEnterOrder(buyOrder2);
    //     Event event2 = TestSetup.eventPublisher.getLastEvent().get();
    //     if (!(event2 instanceof OrderAcceptedEvent)) {
    //         System.out.println(event2);
    //         // return;
    //     }
    //     assertTrue(event2 instanceof OrderAcceptedEvent || event2 instanceof OrderExecutedEvent);
    // }
}
