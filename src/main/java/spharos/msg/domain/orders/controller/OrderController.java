package spharos.msg.domain.orders.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spharos.msg.domain.orders.dto.OrderRequest.OrderDto;
import spharos.msg.domain.orders.dto.OrderResponse.OrderProductDto;
import spharos.msg.domain.orders.service.OrderService;
import spharos.msg.global.api.ApiResponse;
import spharos.msg.global.api.code.status.SuccessStatus;
import spharos.msg.global.security.JwtTokenProvider;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Order", description = "주문 API")
public class OrderController {

    private static final String AUTHORIZATION = "Authorization";

    private final JwtTokenProvider jwtTokenProvider;
    private final OrderService orderService;

    @Operation(summary = "상품 주문",
        description = "상품을 구매하여, Order 객체를 만든다.")
    @PostMapping("/product-order")
    public ApiResponse<?> productOrderAPI(
        @RequestBody List<OrderDto> orderDtos,
        @RequestHeader(AUTHORIZATION) String token) {

        String uuid = jwtTokenProvider.validateAndGetUserUuid(token);
        OrderProductDto orderProductDto = orderService.saveOrder(orderDtos, uuid);
        return ApiResponse.of(SuccessStatus.ORDER_SUCCESS, orderProductDto);
    }
}
