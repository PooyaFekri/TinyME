package ir.ramtung.tinyme.config;

import java.util.*;

import ir.ramtung.tinyme.domain.entity.*;
import net.jqwik.api.*;

public class Providers {

    public static Arbitrary<Security> securities() {
        Arbitrary<Integer> tickSizes = Arbitraries.integers().between(-1, 100);
        Arbitrary<Integer> lotSizes = Arbitraries.integers().between(-1, 100);
        return Combinators.combine(tickSizes, lotSizes)
                .as((tickSize, lotSize) -> {
                    Security security = Security
                            .builder()
                            .tickSize(tickSize)
                            .lotSize(lotSize)
                            .build();
                    return security;
                });
    }

    public static Arbitrary<List<Order>> orderLists() {
        Arbitrary<Security> securities = securities();
        return securities.flatMap(security -> orders(security).list().ofMinSize(10));
    }

    public static Arbitrary<Order> orders(Security security) {
        Arbitrary<Integer> prices = Arbitraries.integers().greaterOrEqual(0);
        Arbitrary<Integer> quantities = Arbitraries.integers().greaterOrEqual(0);
        Arbitrary<Integer> orderIds = Arbitraries.integers().greaterOrEqual(0);
        Broker broker = Broker.builder().credit(100_000_000L).build();
        Shareholder shareholder = Shareholder.builder().build();
        shareholder.incPosition(security, 100_000);
        Arbitrary<Side> sides = Arbitraries.of(Side.class);

        return Combinators.combine(prices, quantities, orderIds, sides)
                .as((price, quantity, orderId, side) -> {
                    return new Order(
                            orderId,
                            security,
                            side,
                            quantity,
                            price,
                            broker,
                            shareholder);
                });

    }
}
