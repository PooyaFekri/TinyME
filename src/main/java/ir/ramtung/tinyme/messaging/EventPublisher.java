package ir.ramtung.tinyme.messaging;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import ir.ramtung.tinyme.messaging.event.Event;

@Component
public class EventPublisher {

    private final Queue<Event> eventQueue = new ConcurrentLinkedQueue<>();

    public void publish(Event event) {
        // log.info("Published: " + event);
        eventQueue.add(event);
    }

    public Optional<Event> searchEvent(Event event) {
        return eventQueue.stream().filter(e -> e.equals(event)).findFirst();
    }

    public Optional<Event> getLastEvent() {
        return eventQueue.isEmpty() ? Optional.empty() : Optional.ofNullable(eventQueue.stream().reduce((first, second) -> second).orElse(null));
    }

    public int size() {
        return eventQueue.size();
    }

    public Optional<Event> peek() {
        return Optional.ofNullable(eventQueue.peek());
    }

    public Optional<Event> poll() {
        return Optional.ofNullable(eventQueue.poll());
    }
}
