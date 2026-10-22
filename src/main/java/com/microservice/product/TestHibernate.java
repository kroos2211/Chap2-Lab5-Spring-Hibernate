package com.microservice.product;

import com.microservice.product.config.AppConfig;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;

public class TestHibernate {
    public static void main(String[] args) throws Exception {
        System.out.println("=== TEST INITIALISATION SPRING ===");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            System.out.println("Contexte Spring initialise avec succes.");
            SessionFactory factory = context.getBean(SessionFactory.class);
            if (factory.isClosed()) throw new IllegalStateException("SessionFactory fermee");
            System.out.println("SessionFactory disponible.");
            try (Connection connection = context.getBean(DataSource.class).getConnection()) {
                if (!connection.isValid(3)) throw new IllegalStateException("Connexion JDBC invalide");
                System.out.println("Connexion a la base reussie : " + connection.getMetaData().getDatabaseProductName());
            }
        }
        System.out.println("=== FIN DU TEST ===");
    }
}
