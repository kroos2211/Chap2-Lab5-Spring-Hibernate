package com.microservice.product.service;

import com.microservice.product.dao.IDao;
import com.microservice.product.entity.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private final IDao<Product> productDao;

    public ProductServiceImpl(IDao<Product> productDao) {
        this.productDao = productDao;
    }

    @Override
    @Transactional
    public void addProduct(Product product) {
        validate(product);
        productDao.save(product);
    }

    @Override
    @Transactional
    public void updateProduct(Product product) {
        validate(product);
        if (product.getId() == null) {
            throw new IllegalArgumentException("Identifiant du produit requis");
        }
        productDao.update(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Produit et identifiant requis");
        }
        productDao.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Identifiant requis");
        }
        return productDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productDao.findAll();
    }

    @Override
    @Transactional
    public void addProductThenFail(Product product) {
        validate(product);
        productDao.save(product);
        throw new IllegalStateException("Echec intentionnel apres ecriture pour verifier le rollback");
    }

    private void validate(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Produit nul interdit");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nom du produit requis");
        }
        if (!Double.isFinite(product.getPrice()) || product.getPrice() < 0) {
            throw new IllegalArgumentException("Prix invalide : valeur negative ou non finie");
        }
    }
}
