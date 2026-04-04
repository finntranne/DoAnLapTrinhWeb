package com.alotra.service.product;

import com.alotra.entity.product.Category;
import com.alotra.entity.product.Product;
import com.alotra.model.ProductSaleDTO;
import com.alotra.repository.product.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    // Cập nhật: Thêm shopId vào tham số
    public Page<ProductSaleDTO> findProductSaleData(Integer shopId, Pageable pageable) {
        return productRepository.findProductSaleData(shopId, pageable);
    }
    
    public Page<ProductSaleDTO> findProductSaleDataByCategory(Category category, Integer shopId, Pageable pageable) {
        return productRepository.findProductSaleDataByCategory(category, shopId, pageable);
    }
    
    public Optional<ProductSaleDTO> findProductSaleDataById(Integer id, Integer shopId) {
        return productRepository.findProductSaleDataById(id, shopId);
    }
    
    public Page<ProductSaleDTO> findProductSaleDataByKeyword(String keyword, Integer shopId, Pageable pageable){
    	return productRepository.findProductSaleDataByKeyword(keyword, shopId, pageable);
    }

    public Page<ProductSaleDTO> findProductSaleDataSorted(Integer shopId, int page, int size, String sortType) {
        List<ProductSaleDTO> allProducts = productRepository
                .findProductSaleData(shopId, PageRequest.of(0, 1000, Sort.unsorted()))
                .getContent();
        return sortAndPage(allProducts, page, size, sortType);
    }

    public Page<ProductSaleDTO> findProductSaleDataByCategorySorted(Category category, Integer shopId, int page, int size,
            String sortType) {
        List<ProductSaleDTO> allProducts = productRepository
                .findProductSaleDataByCategory(category, shopId, PageRequest.of(0, 1000, Sort.unsorted()))
                .getContent();
        return sortAndPage(allProducts, page, size, sortType);
    }

    public Page<ProductSaleDTO> findProductSaleDataByKeywordSorted(String keyword, Integer shopId, int page, int size,
            String sortType) {
        List<ProductSaleDTO> allProducts = productRepository
                .findProductSaleDataByKeyword(keyword, shopId, PageRequest.of(0, 1000, Sort.unsorted()))
                .getContent();
        return sortAndPage(allProducts, page, size, sortType);
    }
    
    public Page<Product> findAll(Pageable pageable) {
		return productRepository.findAll(pageable);
	}
    
    public Optional<Product> findById(Integer id) {
    	return productRepository.findById(id);
    }

    // ✅ SỬA: Chuyển '1' thành '(byte) 1' để khớp với kiểu Byte trong Product Entity
	public Page<Product> findAllApproved(Pageable pageable) {
		return productRepository.findByStatus((byte) 1, pageable);
	}
	
	public List<Product> findAllActive(){
		return productRepository.findAll();
	}

	public List<Integer> findProductIdsByShopIds(List<Integer> applicableShopIds) {
		return productRepository.findProductIdsByShopIds(applicableShopIds);
	}
	
	public void save(Product product) {
		productRepository.save(product);
	}

    private Page<ProductSaleDTO> sortAndPage(List<ProductSaleDTO> products, int page, int size, String sortType) {
        List<ProductSaleDTO> sorted = products.stream()
                .sorted(resolveComparator(sortType))
                .toList();

        int start = page * size;
        int end = Math.min(start + size, sorted.size());
        List<ProductSaleDTO> content = start >= sorted.size() ? List.of() : sorted.subList(start, end);
        return new PageImpl<>(content, PageRequest.of(page, size), sorted.size());
    }

    private Comparator<ProductSaleDTO> resolveComparator(String sortType) {
        String normalized = sortType == null ? "newest" : sortType;
        return switch (normalized) {
            case "priceAsc" -> Comparator.comparing(this::getDisplayPrice, Comparator.nullsLast(Double::compareTo))
                    .thenComparing(this::getProductNameSafe);
            case "priceDesc" -> Comparator.comparing(this::getDisplayPrice, Comparator.nullsLast(Double::compareTo))
                    .reversed()
                    .thenComparing(this::getProductNameSafe);
            case "nameAsc" -> Comparator.comparing(this::getProductNameSafe, String.CASE_INSENSITIVE_ORDER);
            case "nameDesc" -> Comparator.comparing(this::getProductNameSafe, String.CASE_INSENSITIVE_ORDER).reversed();
            case "bestSelling" -> Comparator.comparing(this::getTotalSoldSafe).reversed()
                    .thenComparing(this::getProductIdSafe, Comparator.reverseOrder());
            case "topRated" -> Comparator.comparing(this::getAvgRatingSafe).reversed()
                    .thenComparing(this::getReviewCountSafe, Comparator.reverseOrder())
                    .thenComparing(this::getLikeCountSafe, Comparator.reverseOrder())
                    .thenComparing(this::getProductIdSafe, Comparator.reverseOrder());
            case "topLiked" -> Comparator.comparing(this::getLikeCountSafe).reversed()
                    .thenComparing(this::getAvgRatingSafe, Comparator.reverseOrder())
                    .thenComparing(this::getReviewCountSafe, Comparator.reverseOrder())
                    .thenComparing(this::getProductIdSafe, Comparator.reverseOrder());
            case "newest" -> Comparator.comparing(this::getProductIdSafe, Comparator.reverseOrder());
            default -> Comparator.comparing(this::getProductIdSafe, Comparator.reverseOrder());
        };
    }

    private String getProductNameSafe(ProductSaleDTO dto) {
        return dto.getProduct() != null && dto.getProduct().getProductName() != null
                ? dto.getProduct().getProductName()
                : "";
    }

    private Integer getProductIdSafe(ProductSaleDTO dto) {
        return dto.getProduct() != null && dto.getProduct().getProductID() != null
                ? dto.getProduct().getProductID()
                : 0;
    }

    private Long getTotalSoldSafe(ProductSaleDTO dto) {
        return dto.getTotalSold() != null ? dto.getTotalSold() : 0L;
    }

    private Double getAvgRatingSafe(ProductSaleDTO dto) {
        return dto.getAvgRating() != null ? dto.getAvgRating() : 0.0;
    }

    private Long getReviewCountSafe(ProductSaleDTO dto) {
        return dto.getReviewCount() != null ? dto.getReviewCount() : 0L;
    }

    private Long getLikeCountSafe(ProductSaleDTO dto) {
        return dto.getLikeCount() != null ? dto.getLikeCount() : 0L;
    }

    private Double getDisplayPrice(ProductSaleDTO dto) {
        if (dto.getProduct() == null || dto.getProduct().getVariants() == null || dto.getProduct().getVariants().isEmpty()
                || dto.getProduct().getVariants().get(0).getPrice() == null) {
            return Double.MAX_VALUE;
        }
        return dto.getProduct().getVariants().get(0).getPrice().doubleValue();
    }
}
