package com.nimbleways.springboilerplate.entities;

// Enum utilisé uniquement pour le dispatch dans ProductService ; Product.type reste une colonne String (pas de migration BDD)
public enum ProductType {
    NORMAL,
    SEASONAL,
    EXPIRABLE
}
