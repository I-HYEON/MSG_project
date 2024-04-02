package spharos.msg.domain.brand.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spharos.msg.domain.brand.dto.BrandResponseDto;
import spharos.msg.domain.brand.repository.BrandRepository;
import spharos.msg.global.api.ApiResponse;
import spharos.msg.global.api.code.status.SuccessStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    @Transactional(readOnly = true)
    public ApiResponse<?> getBrands() {
        List<BrandResponseDto> brandResponseDtos = brandRepository.findAll()
                .stream()
                .map(BrandResponseDto::new)
                .distinct()
                .toList();
        return ApiResponse.of(SuccessStatus.BRAND_GET_SUCCESS, brandResponseDtos);
    }
}
