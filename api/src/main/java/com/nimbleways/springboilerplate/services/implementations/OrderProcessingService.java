package com.nimbleways.springboilerplate.services.implementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.repositories.OrderRepository;

// Orchestre le cas d'usage "traiter une commande" ; décharge MyController de tout accès repository/règle métier
@Service
public class OrderProcessingService {

    @Autowired
    OrderRepository or;

    @Autowired
    ProductService productService;

    public Order processOrder(Long orderId) {
        // .get() conservé tel quel : préserve le comportement d'origine (NoSuchElementException si commande inconnue)
        Order order = or.findById(orderId).get();
        order.getItems().forEach(productService::processOrderItem);
        return order;
    }
}
