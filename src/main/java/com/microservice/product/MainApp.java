package com.microservice.product;

import com.microservice.product.config.AppConfig;
import com.microservice.product.entity.Product;
import com.microservice.product.service.ProductService;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

public class MainApp {
    public static void main(String[] args) throws Exception {
        System.out.println("=== CHAPITRE 2 - LAB 5 ===");
        System.out.println("=== INITIALISATION SPRING ===");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            ProductService service = context.getBean(ProductService.class);
            System.out.println("Contexte et ProductService disponibles.");

            System.out.println("=== CONFIGURATION HIBERNATE ===");
            System.out.println("SessionFactory ouverte : " + !context.getBean(SessionFactory.class).isClosed());
            try (Connection connection = context.getBean(DataSource.class).getConnection()) {
                if (!connection.isValid(3)) throw new IllegalStateException("Connexion JDBC invalide");
                System.out.println("Base connectee : " + connection.getMetaData().getDatabaseProductName());
            }

            System.out.println("=== CREATION DES PRODUITS ===");
            Product gateway = new Product("Microservice Gateway", 149.99);
            Product laptop = new Product("Laptop", 999.99);
            Product smartphone = new Product("Smartphone", 499.99);
            Product keyboard = new Product("Clavier", 49.99);
            for (Product product : new Product[]{gateway, laptop, smartphone, keyboard}) {
                service.addProduct(product);
                System.out.println("Cree : " + product);
            }

            System.out.println("=== LISTE DES PRODUITS ===");
            List<Product> products = service.getAllProducts();
            products.forEach(System.out::println);
            System.out.println("Total en base : " + products.size());

            System.out.println("=== RECHERCHE PAR ID ===");
            System.out.println("ID existant : " + service.getProductById(laptop.getId()));
            System.out.println("ID inexistant : " + service.getProductById(Long.MAX_VALUE));

            System.out.println("=== MODIFICATION PRODUIT ===");
            laptop.setPrice(899.99);
            service.updateProduct(laptop);
            Product updated = service.getProductById(laptop.getId());
            if (updated == null || updated.getPrice() != 899.99) throw new IllegalStateException("Modification non persistee");
            System.out.println("Prix persiste : " + updated);

            System.out.println("=== SUPPRESSION PRODUIT ===");
            service.deleteProduct(keyboard);
            Product deleted = service.getProductById(keyboard.getId());
            if (deleted != null) throw new IllegalStateException("Suppression non persistee");
            System.out.println("Produit supprime, recherche en base : " + deleted);

            System.out.println("=== VALIDATION PRIX NEGATIF ===");
            int beforeInvalid = service.getAllProducts().size();
            try {
                service.addProduct(new Product("Invalide", -1));
                throw new IllegalStateException("Le prix negatif a ete accepte");
            } catch (IllegalArgumentException expected) {
                System.out.println("Prix negatif refuse : " + expected.getMessage());
            }
            if (service.getAllProducts().size() != beforeInvalid) throw new IllegalStateException("Produit invalide persiste");
            System.out.println("Aucun produit invalide en base.");

            System.out.println("=== TEST TRANSACTION COMMIT ===");
            Product committed = new Product("Commit demo", 10);
            service.addProduct(committed);
            if (service.getProductById(committed.getId()) == null) throw new IllegalStateException("Commit absent");
            System.out.println("Commit confirme en base : " + committed);

            System.out.println("=== TEST TRANSACTION ROLLBACK ===");
            Product rolledBack = new Product("Rollback demo", 20);
            try {
                service.addProductThenFail(rolledBack);
                throw new IllegalStateException("Exception transactionnelle absente");
            } catch (IllegalStateException expected) {
                System.out.println("Exception apres ecriture : " + expected.getMessage());
            }
            boolean found = service.getAllProducts().stream().anyMatch(p -> "Rollback demo".equals(p.getName()));
            if (found) throw new IllegalStateException("Rollback non effectue");
            System.out.println("Verification en base apres rollback : produit absent.");

            System.out.println("=== RESULTAT FINAL ===");
            System.out.println("Produits restants en base : " + service.getAllProducts().size());
        }
    }
}
