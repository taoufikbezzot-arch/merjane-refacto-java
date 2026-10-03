package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class ProductService {

    @Autowired
    ProductRepository pr;

    @Autowired
    NotificationService ns;

    public void notifyDelay(int leadTime, Product p) {
        p.setLeadTime(leadTime);
        pr.save(p);
        ns.sendDelayNotification(leadTime, p.getName());
    }

    // Oriente un produit vers sa règle de stock selon son type ; un type inconnu est ignoré (comportement d'origine conservé)
    public void processOrderItem(Product p) {
        ProductType type;
        try {
            type = ProductType.valueOf(p.getType());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return;
        }

        switch (type) {
            case NORMAL -> processNormalProduct(p);
            case SEASONAL -> processSeasonalProduct(p);
            case EXPIRABLE -> processExpirableProduct(p);
        }
    }

    private void processNormalProduct(Product p) {
        if (p.getAvailable() > 0) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else if (p.getLeadTime() > 0) {
            notifyDelay(p.getLeadTime(), p);
        }
    }

    private void processSeasonalProduct(Product p) {
        // Fusionne l'ancien "fast path" du contrôleur et le "slow path" de handleSeasonalProduct en un seul arbre de décision
        LocalDate today = LocalDate.now();
        boolean inSeasonWithStock = today.isAfter(p.getSeasonStartDate())
                && today.isBefore(p.getSeasonEndDate())
                && p.getAvailable() > 0;

        if (inSeasonWithStock) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else if (today.plusDays(p.getLeadTime()).isAfter(p.getSeasonEndDate())) {
            ns.sendOutOfStockNotification(p.getName());
            p.setAvailable(0);
            pr.save(p);
        } else if (p.getSeasonStartDate().isAfter(today)) {
            ns.sendOutOfStockNotification(p.getName());
            pr.save(p);
        } else {
            notifyDelay(p.getLeadTime(), p);
        }
    }

    private void processExpirableProduct(Product p) {
        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
            p.setAvailable(p.getAvailable() - 1);
            pr.save(p);
        } else {
            ns.sendExpirationNotification(p.getName(), p.getExpiryDate());
            p.setAvailable(0);
            pr.save(p);
        }
    }
}