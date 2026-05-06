//package tech.iraelie.practice;
//
//import org.springframework.stereotype.Repository;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public class ProductRepositoryImpl implements ProductRepository{
//    private final List<Product> products = new ArrayList<>();
//
//    public ProductRepositoryImpl() {
//        products.add(new Product("1", "Laptop", 1200.50));
//        products.add(new Product("2", "Phone", 800.00));
//        products.add(new Product("3", "Headphones", 150.75));
//        products.add(new Product("4", "Keyboard", 75.20));
//        products.add(new Product("5", "Mouse", 40.99));
//    }
//
//    @Override
//    public List<Product> findAll() {
//        return products;
//    }
//
//    @Override
//    public Optional<Product> findById(String id) {
//        return products.stream()
//                .filter(product -> product.getId().equals(id))
//                .findFirst();
//    }
//
//    @Override
//    public Product save(Product product) {
//        products.add(product);
//        return product;
//    }
//
//    @Override
//    public Optional<Product> update(String id, Product product) {
//        return Optional.ofNullable(products.stream()
//                .filter(p -> p.getId().equals(id))
//                .findFirst()
//                .map(p -> {
//                    p.setId(product.getId());
//                    p.setName(product.getName());
//                    p.setPrice(product.getPrice());
//                    return p;
//                }).orElseThrow(
//                        () -> new ProductNotFoundException(id)
//                ));
//    }
//
//    @Override
//    public boolean deleteById(String id) {
//        return products.stream()
//                        .filter(p -> p.getId().equals(id))
//                        .findFirst()
//                        .map(products::remove)
//                        .orElseThrow(
//                                () -> new ProductNotFoundException(id)
//                        );
//    }
//
//}
