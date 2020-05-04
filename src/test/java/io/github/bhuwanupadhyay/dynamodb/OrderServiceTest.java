package io.github.bhuwanupadhyay.dynamodb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceTest {

  @Autowired private OrderService orderService;

  @Test
  void canPlaceOrderSuccessfully() {
    Order order = order();
    order.setOrderId(UUID.randomUUID().toString());
    orderService.createOrder(order);
  }

  @Test
  void whenNotPlaceOrderThenWriteToDLQ() {
    Order order = order();
    orderService.createOrder(order);
  }

  private Order order() {
    final Order order = new Order();
    final OrderLine orderLine = new OrderLine();

    final List<OrderItem> orderItems = new ArrayList<>();

    final OrderItem orderItem = new OrderItem();
    orderItem.setItemId("I#0001");
    orderItem.setQuantity(10);

    orderItems.add(orderItem);
    orderLine.setOrderItems(orderItems);
    order.setOrderLine(orderLine);

    return order;
  }
}