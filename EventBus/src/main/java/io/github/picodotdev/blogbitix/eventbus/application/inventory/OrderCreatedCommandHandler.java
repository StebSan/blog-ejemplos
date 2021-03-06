package io.github.picodotdev.blogbitix.eventbus.application.inventory;

import io.github.picodotdev.blogbitix.eventbus.domain.inventory.OrderOversold;
import io.github.picodotdev.blogbitix.eventbus.domain.inventory.Product;
import io.github.picodotdev.blogbitix.eventbus.domain.inventory.ProductId;
import io.github.picodotdev.blogbitix.eventbus.domain.inventory.ProductRepository;
import io.github.picodotdev.blogbitix.eventbus.domain.order.Item;
import io.github.picodotdev.blogbitix.eventbus.domain.order.Order;
import io.github.picodotdev.blogbitix.eventbus.domain.order.OrderCreated;
import io.github.picodotdev.blogbitix.eventbus.domain.order.OrderId;
import io.github.picodotdev.blogbitix.eventbus.domain.order.OrderRepository;
import io.github.picodotdev.blogbitix.eventbus.domain.shared.commandbus.CommandHandler;
import io.github.picodotdev.blogbitix.eventbus.domain.shared.eventbus.EventBus;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderCreatedCommandHandler implements CommandHandler<OrderCreatedCommand> {

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private EventBus eventBus;

    public OrderCreatedCommandHandler(ProductRepository productRepository, OrderRepository orderRepository, EventBus eventBus) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(OrderCreatedCommand command) {
        OrderCreated event = command.getEvent();
        OrderId orderId = event.getOrderId();
        Order order = orderRepository.findById(orderId);

        List<ProductId> oversoldProductIds = order.getItems().stream().filter(it -> {
            Product product = productRepository.findById(it.getProductId());
            return product.hasStock(it.getQuantity());
        }).map(Item::getProductId).collect(Collectors.toList());

        order.getItems().forEach(it -> {
            Product product = productRepository.findById(it.getProductId());
            product.substractStock(it.getQuantity());
            eventBus.publish(product);
        });

        eventBus.publish(new OrderOversold(orderId, oversoldProductIds));
    }
}
