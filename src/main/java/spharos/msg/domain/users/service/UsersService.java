package spharos.msg.domain.users.service;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spharos.msg.domain.users.dto.KakaoLoginRequestDto;
import spharos.msg.domain.users.dto.LoginRequestDto;
import spharos.msg.domain.users.dto.SignUpRequestDto;
import spharos.msg.domain.users.entity.Address;
import spharos.msg.domain.users.entity.Users;
import spharos.msg.domain.users.repository.KakaoUsersRepository;
import spharos.msg.domain.users.repository.UsersRepository;
import spharos.msg.global.api.code.status.ErrorStatus;
import spharos.msg.global.api.exception.JwtTokenException;
import spharos.msg.global.api.exception.UsersException;
import spharos.msg.global.redis.RedisService;
import spharos.msg.global.security.JwtTokenProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;
    private final KakaoUsersRepository kakaoUsersRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final KakaoUsersService kakaoUsersService;
    private final AddressService addressService;

    public static final String BEARER = "Bearer";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String BASIC_ADDRESS_NAME = "기본 배송지";

    @Value("${JWT.access-token-expiration}")
    private long accessTokenExpiration;
    @Value("${JWT.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional(readOnly = true)
    public Users login(LoginRequestDto loginRequestDto) {
        Users users = usersRepository.findByLoginId(loginRequestDto.getLoginId())
                .orElseThrow(() -> new UsersException(ErrorStatus.LOGIN_ID_NOT_FOUND));

        //비밀번호 검증
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(loginRequestDto.getPassword(), users.getPassword())) {
            throw new UsersException(ErrorStatus.LOGIN_ID_PW_VALIDATION);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        users.getUsername(),
                        loginRequestDto.getPassword()
                )
        );

        log.info("통합 로그인 Login={}", users.getId());

        return users;
    }

    @Transactional
    public Users createUsers(SignUpRequestDto signUpRequestDto) {
        Users newUser = Users
                .builder()
                .loginId(signUpRequestDto.getLoginId())
                .password(signUpRequestDto.getPassword())
                .userName(signUpRequestDto.getUsername())
                .email(signUpRequestDto.getEmail())
                .phoneNumber(signUpRequestDto.getPhoneNumber())
                .uuid(UUID.randomUUID().toString())
                .baseAddressId(0L)
                .build();
        newUser.passwordToHash(signUpRequestDto.getPassword());

        Address newAddress = Address
                .builder()
                .addressName(BASIC_ADDRESS_NAME)
                .recipient(signUpRequestDto.getUsername())
                .mobileNumber(signUpRequestDto.getPhoneNumber())
                .addressPhoneNumber(signUpRequestDto.getPhoneNumber())
                .address(signUpRequestDto.getAddress())
                .users(newUser)
                .build();

        newUser.addAddress(newAddress);

        usersRepository.save(newUser);
        addressService.createNewAddress(newAddress);

        return newUser;
    }

    public String createRefreshToken(Users users) {
        String token = jwtTokenProvider.generateToken(users, refreshTokenExpiration, "Refresh");
        redisService.saveRefreshToken(users.getUuid(), token, refreshTokenExpiration);
        return BEARER + "%20" + token;
    }

    public String createAccessToken(Users users) {
        return BEARER + " " + jwtTokenProvider.generateToken(users, accessTokenExpiration,
                "Access");
    }

    public void createTokenAndCreateHeaders(HttpServletResponse response, Users users) {

        //refreshToken 확인 후, 있을 시, Delete 처리
        if (Boolean.TRUE.equals(redisService.isRefreshTokenExist(users.getUuid()))) {
            log.info("Delete Token is Complete={}", redisService.getRefreshToken(users.getUuid()));
            redisService.deleteRefreshToken(users.getUuid());
        }

        String accessToken = createAccessToken(users);
        String refreshToken = createRefreshToken(users);

        doResponseCookieAndHeader(response, refreshToken, accessToken);
    }

    private void doResponseCookieAndHeader(
            HttpServletResponse response, String refreshToken, String accessToken) {
        response.addHeader(ACCESS_TOKEN, accessToken);
        Cookie cookie = new Cookie(REFRESH_TOKEN, refreshToken);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) refreshTokenExpiration);
        response.addCookie(cookie);
    }

    @Transactional(readOnly = true)
    public Users getUsersByUuid(String uuid){
        return usersRepository.findByUuid(uuid).orElseThrow(
                () -> new JwtTokenException(ErrorStatus.REISSUE_TOKEN_FAIL));
    }

    public void userLogout(String uuid) {
        if (Boolean.TRUE.equals(redisService.isRefreshTokenExist(uuid))) {
            redisService.deleteRefreshToken(uuid);
        }
    }

    @Transactional
    public void deleteUsers(String uuid) {
        Users users = usersRepository.findByUuid(uuid).orElseThrow();
        usersRepository.deleteById(users.getId());
    }

    @Transactional(readOnly = true)
    public Boolean isUsers(SignUpRequestDto signUpRequestDto) {
        return usersRepository.findByUserNameAndEmail(
                signUpRequestDto.getUsername(),
                signUpRequestDto.getEmail()).isPresent();
    }

    @Transactional
    public void createEasyAndUnionUsers(SignUpRequestDto signUpRequestDto) {
        //KakaoService 어떻게 해결하지..?
        if (!isUsers(signUpRequestDto)) {
            log.info("간편회원가입 중, 가입된 회원이 아니라 통합회원가입 진행");
            this.createUsers(signUpRequestDto);
        }
        Users users = usersRepository.findByUserNameAndEmail(signUpRequestDto.getUsername(),
                        signUpRequestDto.getEmail())
                .orElseThrow(() -> new UsersException(ErrorStatus.SIGN_UP_EASY_FAIL));

        if (kakaoUsersRepository.findKakaoUsersByUserUuid(users.getUuid()).isPresent()) {
            throw new UsersException(ErrorStatus.ALREADY_EASY_USER);
        }
        kakaoUsersService.createKakaoUsers(users.getUuid());
    }

    @Transactional(readOnly = true)
    public Users easyLogin(KakaoLoginRequestDto kakaoLoginRequestDto) {
        Users findUser = usersRepository.findByUserNameAndEmail(
                        kakaoLoginRequestDto.getUsername(),
                        kakaoLoginRequestDto.getEmail())
                .orElseThrow(() -> new UsersException(ErrorStatus.EASY_LOGIN_ID_NOT_FOUND));

        kakaoUsersRepository.findKakaoUsersByUserUuid(findUser.getUuid())
                .orElseThrow(() -> new UsersException(ErrorStatus.IS_NOT_EASY_USER));

        log.info("간편 로그인 Login={}", findUser.getId());

        return findUser;
    }
}
