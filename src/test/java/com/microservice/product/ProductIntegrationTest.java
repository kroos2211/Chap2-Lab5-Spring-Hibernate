package com.microservice.product;

import com.microservice.product.config.AppConfig;
import com.microservice.product.dao.IDao;
import com.microservice.product.entity.Product;
import com.microservice.product.service.ProductService;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.orm.hibernate5.HibernateTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.Assert.*;

public class ProductIntegrationTest {
    private AnnotationConfigApplicationContext context;
    private ProductService service;
    private String previousProfile;

    @Before
    public void setUp() {
        previousProfile = System.getProperty("product.profile");
        System.setProperty("product.profile", "h2");
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        service = context.getBean(ProductService.class);
    }

    @After
    public void tearDown() {
        if (context != null) context.close();
        if (previousProfile == null) System.clearProperty("product.profile");
        else System.setProperty("product.profile", previousProfile);
    }

    @Test public void contextStarts() { assertTrue(context.isActive()); }
    @Test public void dataSourceExists() { assertNotNull(context.getBean(DataSource.class)); }
    @Test public void jdbcConnectionWorks() throws Exception {
        try (Connection connection = context.getBean(DataSource.class).getConnection()) {
            assertTrue(connection.isValid(3));
            assertEquals("H2", connection.getMetaData().getDatabaseProductName());
        }
    }
    @Test public void sessionFactoryExists() { assertFalse(context.getBean(SessionFactory.class).isClosed()); }
    @Test public void daoIsInjected() { assertNotNull(context.getBean(IDao.class)); }
    @Test public void serviceIsInjected() { assertNotNull(service); }
    @Test public void transactionManagerExists() { assertNotNull(context.getBean(HibernateTransactionManager.class)); }
    @Test public void saveProduct() {
        Product product = new Product("A", 1);
        service.addProduct(product);
        assertNotNull(product.getId());
    }
    @Test public void findById() {
        Product product = new Product("A", 1);
        service.addProduct(product);
        assertEquals("A", service.getProductById(product.getId()).getName());
    }
    @Test public void findAll() {
        service.addProduct(new Product("A", 1));
        service.addProduct(new Product("B", 2));
        assertEquals(2, service.getAllProducts().size());
    }
    @Test public void updateProduct() {
        Product product = new Product("A", 1);
        service.addProduct(product);
        product.setPrice(3);
        service.updateProduct(product);
        assertEquals(3, service.getProductById(product.getId()).getPrice(), 0);
    }
    @Test public void deleteProduct() {
        Product product = new Product("A", 1);
        service.addProduct(product);
        service.deleteProduct(product);
        assertNull(service.getProductById(product.getId()));
    }
    @Test public void missingIdReturnsNull() { assertNull(service.getProductById(Long.MAX_VALUE)); }
    @Test public void nullProductRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addProduct(null));
    }
    @Test public void blankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addProduct(new Product("  ", 1)));
    }
    @Test public void negativePriceRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addProduct(new Product("A", -1)));
        assertTrue(service.getAllProducts().isEmpty());
    }
    @Test public void nonFinitePriceRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addProduct(new Product("A", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> service.addProduct(new Product("A", Double.POSITIVE_INFINITY)));
    }
    @Test public void invalidUpdateRejected() {
        Product product = new Product("A", 1);
        service.addProduct(product);
        product.setPrice(-1);
        assertThrows(IllegalArgumentException.class, () -> service.updateProduct(product));
        assertEquals(1, service.getProductById(product.getId()).getPrice(), 0);
    }
    @Test public void commitPersists() {
        Product product = new Product("Commit", 1);
        service.addProduct(product);
        assertNotNull(service.getProductById(product.getId()));
    }
    @Test public void rollbackAfterWriteRemovesProduct() {
        Product product = new Product("Rollback", 1);
        assertThrows(IllegalStateException.class, () -> service.addProductThenFail(product));
        assertTrue(service.getAllProducts().stream().noneMatch(p -> "Rollback".equals(p.getName())));
    }
    @Test public void serviceUsesTransactionalProxy() {
        assertTrue(AopUtils.isAopProxy(service));
        service.getAllProducts();
        assertThrows(Exception.class, () -> context.getBean(IDao.class).findAll());
    }
}
