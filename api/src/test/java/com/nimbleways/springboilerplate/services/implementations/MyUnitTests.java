package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks 
    private ProductService productService;

    @Test
    public void test() {
        // GIVEN
        Product product =new Product(null, 15, 0, "NORMAL", "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendDelayNotification(product.getLeadTime(), product.getName());
    }

    // NORMAL en stock : simple décrémentation, aucune notification
    @Test
    public void normalProductInStockIsDecremented() {
        // GIVEN
        Product product = new Product(null, 15, 5, "NORMAL", "USB Cable", null, null, null);

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(4, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    // NORMAL en rupture avec un délai de réapprovisionnement : le client est notifié du délai
    @Test
    public void normalProductOutOfStockWithLeadTimeNotifiesDelay() {
        // GIVEN
        Product product = new Product(null, 15, 0, "NORMAL", "USB Cable", null, null, null);

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendDelayNotification(15, product.getName());
    }

    // NORMAL en rupture sans délai connu : comportement d'origine conservé, aucune action
    @Test
    public void normalProductOutOfStockWithoutLeadTimeDoesNothing() {
        // GIVEN
        Product product = new Product(null, 0, 0, "NORMAL", "USB Cable", null, null, null);

        // WHEN
        productService.processOrderItem(product);

        // THEN
        Mockito.verifyNoInteractions(productRepository);
        Mockito.verifyNoInteractions(notificationService);
    }

    // SEASONAL en saison avec stock : simple décrémentation, aucune notification
    @Test
    public void seasonalProductInSeasonWithStockIsDecremented() {
        // GIVEN
        Product product = new Product(null, 15, 5, "SEASONAL", "Watermelon",
                null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(4, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    // SEASONAL dont le délai dépasse la fin de saison : produit considéré non disponible (available=0)
    @Test
    public void seasonalProductBecomesUnavailableWhenDelayExceedsSeasonEnd() {
        // GIVEN: lead time would push delivery past the end of the season
        Product product = new Product(null, 30, 0, "SEASONAL", "Watermelon",
                null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(5));

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(0, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendOutOfStockNotification(product.getName());
    }

    // SEASONAL avant le début de la saison : notifié indisponible sans toucher au stock
    @Test
    public void seasonalProductNotYetInSeasonIsReportedOutOfStock() {
        // GIVEN: season has not started yet
        Product product = new Product(null, 15, 30, "SEASONAL", "Grapes",
                null, LocalDate.now().plusDays(180), LocalDate.now().plusDays(240));

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(30, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendOutOfStockNotification(product.getName());
    }

    // SEASONAL en saison mais en rupture, délai compatible avec la fin de saison : notification de délai
    @Test
    public void seasonalProductInSeasonOutOfStockWithinLeadTimeNotifiesDelay() {
        // GIVEN: in season, no stock, but lead time still fits within the season
        Product product = new Product(null, 5, 0, "SEASONAL", "Watermelon",
                null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(58));

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(5, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendDelayNotification(5, product.getName());
    }

    // EXPIRABLE en stock et non périmé : vente normale, simple décrémentation
    @Test
    public void expirableProductInStockAndNotExpiredIsDecremented() {
        // GIVEN
        Product product = new Product(null, 15, 30, "EXPIRABLE", "Butter",
                LocalDate.now().plusDays(26), null, null);

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(29, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    // EXPIRABLE périmé : notification d'expiration et stock forcé à 0
    @Test
    public void expiredProductIsMarkedUnavailable() {
        // GIVEN
        Product product = new Product(null, 90, 6, "EXPIRABLE", "Milk",
                LocalDate.now().minusDays(2), null, null);

        // WHEN
        productService.processOrderItem(product);

        // THEN
        assertEquals(0, product.getAvailable());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendExpirationNotification(product.getName(), product.getExpiryDate());
    }
}
