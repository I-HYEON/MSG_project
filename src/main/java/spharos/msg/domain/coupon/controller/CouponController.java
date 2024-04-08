package spharos.msg.domain.coupon.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import spharos.msg.domain.coupon.dto.CouponResponseDto;
import spharos.msg.domain.coupon.service.CouponService;
import spharos.msg.global.api.ApiResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Coupon", description = "쿠폰 API")
public class CouponController {
    private final CouponService couponService;

    @GetMapping("/coupon")
    private ApiResponse<List<CouponResponseDto>> getCoupon(
    ) {
        return couponService.getCoupon();
    }

    @PostMapping("/coupon/{couponId}")
    private ApiResponse<Void> downloadCoupon(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long couponId
    ) {
        return couponService.downloadCoupon(userDetails.getUsername(), couponId);
    }

    @GetMapping("/coupon-user")
    private ApiResponse<List<CouponResponseDto>> getUsersCoupon(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return couponService.getUsersCoupon(userDetails.getUsername());
    }
}
