package ir.ramtung.tinyme.domain;

import java.util.List;
import java.time.LocalDateTime;

import ir.ramtung.tinyme.messaging.request.*;
import ir.ramtung.tinyme.messaging.event.*;
import ir.ramtung.tinyme.messaging.*;
import ir.ramtung.tinyme.domain.entity.*;
import ir.ramtung.tinyme.domain.service.*;
import ir.ramtung.tinyme.repository.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

public class BaseProviders {

    protected OrderHandler orderHandler = null;

    public static class TestSetup {

        static final long brokerId = 1L;
        static final long shareholderId = 1;
        static final String isin = "US1234567890";

        static final BrokerRepository brokerRepo = new BrokerRepository();
        static final SecurityRepository securityRepo = new SecurityRepository();
        static final ShareholderRepository shareholderRepo = new ShareholderRepository();
        static final EventPublisher eventPublisher = new EventPublisher();
    }

    Broker getBroker(long brokerId) {
        return TestSetup.brokerRepo.findBrokerById(brokerId);
    }

    long getBrokerCredit(long brokerId) {
        Broker broker = TestSetup.brokerRepo.findBrokerById(TestSetup.brokerId);
        return broker.getCredit();
    }

    EventPublisher getEventPublisher() {
        return TestSetup.eventPublisher;
    }

    @Provide
    Arbitrary<Broker> brokerProvider() {
        Arbitrary<Integer> credit = Arbitraries.integers().between(0, 100);
        Arbitrary<Long> brokerId = Arbitraries.longs().between(TestSetup.brokerId, TestSetup.brokerId);
        return Combinators.combine(credit, brokerId)
                .as((cre, bid) -> Broker.builder().brokerId(bid).credit(cre).build());
    }

    @Provide
    Arbitrary<EnterOrderRq> enterBuyOrderRqProvider() {
        // Arbitrary instances for random values
        Arbitrary<Long> requestId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<String> securityIsin = Arbitraries.of(TestSetup.isin); // Wrap in Arbitrary
        Arbitrary<Long> orderId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<LocalDateTime> entryTime = Arbitraries.defaultFor(LocalDateTime.class);
        Arbitrary<Side> side = Arbitraries.of(Side.BUY); // Wrap in Arbitrary
        Arbitrary<Integer> quantity = Arbitraries.integers().between(1, 3);
        Arbitrary<Integer> price = Arbitraries.integers().between(1, 3);
        Arbitrary<Long> brokerId = Arbitraries.of(TestSetup.brokerId); // Wrap in Arbitrary
        Arbitrary<Long> shareholderId = Arbitraries.of(TestSetup.shareholderId); // Wrap in Arbitrary
        Arbitrary<Integer> peakSize = Arbitraries.integers().between(0, 0);
        // Combine first set of Arbitrary instances
        Arbitrary<List<Object>> firstCombine = Combinators.combine(requestId, securityIsin, orderId)
                .as((reqId, isin, ordId) -> List.of(reqId, isin, ordId));

        // Combine second set of Arbitrary instances
        Arbitrary<List<Object>> secondCombine = Combinators
                .combine(entryTime, side, quantity, price, brokerId, shareholderId, peakSize)
                .as((entry, s, qty, pr, brk, share, peak) -> List.of(entry, s, qty, pr, brk, share, peak));

        // Combine the results from both combinations
        return Combinators.combine(firstCombine, secondCombine)
                .as((first, second) -> EnterOrderRq.createNewOrderRq(
                        (Long) first.get(0), (String) first.get(1), (Long) first.get(2),
                        (LocalDateTime) second.get(0), (Side) second.get(1), (Integer) second.get(2),
                        (Integer) second.get(3),
                        (Long) second.get(4), (Long) second.get(5), (Integer) second.get(6)));
    }

    @Provide
    Arbitrary<EnterOrderRq> enterSellOrderRqProvider() {
        // Arbitrary instances for random values
        Arbitrary<Long> requestId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<String> securityIsin = Arbitraries.of(TestSetup.isin); // Wrap in Arbitrary
        Arbitrary<Long> orderId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<LocalDateTime> entryTime = Arbitraries.defaultFor(LocalDateTime.class);
        Arbitrary<Side> side = Arbitraries.of(Side.SELL); // Wrap in Arbitrary
        Arbitrary<Integer> quantity = Arbitraries.integers().between(1, 3);
        Arbitrary<Integer> price = Arbitraries.integers().between(1, 3);
        Arbitrary<Long> brokerId = Arbitraries.of(TestSetup.brokerId); // Wrap in Arbitrary
        Arbitrary<Long> shareholderId = Arbitraries.of(TestSetup.shareholderId); // Wrap in Arbitrary
        Arbitrary<Integer> peakSize = Arbitraries.integers().between(0, 0);

        // Combine first set of Arbitrary instances
        Arbitrary<List<Object>> firstCombine = Combinators.combine(requestId, securityIsin, orderId)
                .as((reqId, isin, ordId) -> List.of(reqId, isin, ordId));

        // Combine second set of Arbitrary instances
        Arbitrary<List<Object>> secondCombine = Combinators
                .combine(entryTime, side, quantity, price, brokerId, shareholderId, peakSize)
                .as((entry, s, qty, pr, brk, share, peak) -> List.of(entry, s, qty, pr, brk, share, peak));

        // Combine the results from both combinations
        return Combinators.combine(firstCombine, secondCombine)
                .as((first, second) -> EnterOrderRq.createNewOrderRq(
                        (Long) first.get(0), (String) first.get(1), (Long) first.get(2),
                        (LocalDateTime) second.get(0), (Side) second.get(1), (Integer) second.get(2),
                        (Integer) second.get(3),
                        (Long) second.get(4), (Long) second.get(5), (Integer) second.get(6)));
    }

    @Provide
    Arbitrary<OrderHandler> orderHandler() {
        Matcher matcher = new Matcher();

        OrderHandler orderHandler = new OrderHandler(TestSetup.securityRepo, TestSetup.brokerRepo,
                TestSetup.shareholderRepo, TestSetup.eventPublisher, matcher);

        for (int i = 0; i < Arbitraries.integers().between(1, 10).sample(); i++) {
            EnterOrderRq randomSellOrder = enterSellOrderRqProvider().sample();
            orderHandler.handleEnterOrder(randomSellOrder);
        }

        for (int i = 0; i < Arbitraries.integers().between(1, 10).sample(); i++) {
            EnterOrderRq randomBuyOrder = enterBuyOrderRqProvider().sample();
            orderHandler.handleEnterOrder(randomBuyOrder);

        }
        return Arbitraries.of(orderHandler);
    }

    @Provide
    Arbitrary<EnterOrderRq> updateBuyOrderRqProvider() {
        // Arbitrary instances for random values
        Arbitrary<Long> requestId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<String> securityIsin = Arbitraries.of(TestSetup.isin); // Wrap in Arbitrary
        Arbitrary<Long> orderId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<LocalDateTime> entryTime = Arbitraries.defaultFor(LocalDateTime.class);
        Arbitrary<Side> side = Arbitraries.of(Side.BUY); // Wrap in Arbitrary
        Arbitrary<Integer> quantity = Arbitraries.integers().between(1, 3);
        Arbitrary<Integer> price = Arbitraries.integers().between(1, 3);
        Arbitrary<Long> brokerId = Arbitraries.of(TestSetup.brokerId); // Wrap in Arbitrary
        Arbitrary<Long> shareholderId = Arbitraries.of(TestSetup.shareholderId); // Wrap in Arbitrary
        Arbitrary<Integer> peakSize = Arbitraries.integers().between(0, 0);
        // Combine first set of Arbitrary instances
        Arbitrary<List<Object>> firstCombine = Combinators.combine(requestId, securityIsin, orderId)
                .as((reqId, isin, ordId) -> List.of(reqId, isin, ordId));

        // Combine second set of Arbitrary instances
        Arbitrary<List<Object>> secondCombine = Combinators
                .combine(entryTime, side, quantity, price, brokerId, shareholderId, peakSize)
                .as((entry, s, qty, pr, brk, share, peak) -> List.of(entry, s, qty, pr, brk, share, peak));

        // Combine the results from both combinations
        return Combinators.combine(firstCombine, secondCombine)
                .as((first, second) -> EnterOrderRq.createUpdateOrderRq(
                        (Long) first.get(0), (String) first.get(1), (Long) first.get(2),
                        (LocalDateTime) second.get(0), (Side) second.get(1), (Integer) second.get(2),
                        (Integer) second.get(3),
                        (Long) second.get(4), (Long) second.get(5), (Integer) second.get(6)));
    }

    @Provide
    public Arbitrary<DeleteOrderRq> deleteBuyOrderRqProvider() {
        // Arbitrary instances for random values
        Arbitrary<Long> requestId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<String> securityIsin = Arbitraries.of(TestSetup.isin);
        Arbitrary<Long> orderId = Arbitraries.longs().between(1, Long.MAX_VALUE);
        Arbitrary<Side> side = Arbitraries.of(Side.BUY);
        // Combine the results from both combinations
        return Combinators.combine(requestId, securityIsin, side, orderId)
                .as((reqId, secIsin, s, ordId) -> new DeleteOrderRq(reqId, secIsin, s, ordId));
    }

    void revertMatchEngine(EnterOrderRq order, OrderExecutedEvent event, long broker_credit) {
        // For each trade in the event, create and process the appropriate orders
        boolean needBuySide = (order.getSide() == Side.SELL);
        boolean needSellSide = (order.getSide() == Side.BUY);
        TestSetup.brokerRepo.addBroker(Broker.builder().brokerId(TestSetup.brokerId).credit(1000_000).build());
        Event result_event = null;
        for (TradeDTO trade : event.getTrades()) {
            if (needBuySide) {
                do {
                    EnterOrderRq buyOrder = EnterOrderRq.createNewOrderRq(
                            trade.buyOrderId() + 1000, // Use a different request ID
                            trade.securityIsin(),
                            trade.buyOrderId(),
                            LocalDateTime.now(),
                            Side.BUY,
                            trade.quantity(),
                            trade.price(),
                            TestSetup.brokerId,
                            TestSetup.shareholderId,
                            0 // No peak size
                    );
                    this.orderHandler.handleEnterOrder(buyOrder);
                    result_event = TestSetup.eventPublisher.getLastEvent().get();
                } while (!(result_event instanceof OrderAcceptedEvent));
            } else if (needSellSide) {
                do {
                    EnterOrderRq sellOrder = EnterOrderRq.createNewOrderRq(
                            trade.sellOrderId() + 1000, // Use a different request ID
                            trade.securityIsin(),
                            trade.sellOrderId(),
                            LocalDateTime.now(),
                            Side.SELL,
                            trade.quantity(),
                            trade.price(),
                            TestSetup.brokerId,
                            TestSetup.shareholderId,
                            0 // No peak size
                    );
                    this.orderHandler.handleEnterOrder(sellOrder);
                    result_event = TestSetup.eventPublisher.getLastEvent().get();
                } while (!(result_event instanceof OrderAcceptedEvent));
            }
        }
        TestSetup.brokerRepo.addBroker(Broker.builder().brokerId(TestSetup.brokerId).credit(broker_credit).build());
    }

    // Wrapper method to be used in the test
    @BeforeTry
    private void testSetup() {
        Security security = Security.builder().isin(TestSetup.isin).build();
        Shareholder shareholder = Shareholder.builder().shareholderId(TestSetup.shareholderId).build();
        shareholder.incPosition(security, 100_000_000);
        TestSetup.securityRepo.addSecurity(security);
        TestSetup.shareholderRepo.addShareholder(shareholder);
        Broker broker = brokerProvider().sample();
        TestSetup.brokerRepo.addBroker(broker);
        this.orderHandler = orderHandler().sample();
    }

    public Order findOrderById(Side side, long orderId) {
        OrderBook orderBook = TestSetup.securityRepo.findSecurityByIsin(TestSetup.isin).getOrderBook();
        return orderBook.findByOrderId(side, orderId);
    }

}
