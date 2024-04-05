package spharos.msg.domain.likes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spharos.msg.domain.likes.entity.Likes;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class LikesResponseDto {
    private Long productId;
    private String productName;
    private int productPrice;
    private BigDecimal discountRate;

    public LikesResponseDto(Likes likes) {
        this.productId = likes.getProduct().getId();
        this.productName = likes.getProduct().getProductName();
        this.productPrice = likes.getProduct().getProductPrice();
        this.discountRate = likes.getProduct().getDiscountRate();
    }
}
