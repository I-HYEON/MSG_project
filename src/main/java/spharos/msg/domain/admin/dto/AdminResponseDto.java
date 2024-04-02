package spharos.msg.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class AdminResponseDto {

    @Builder
    @AllArgsConstructor
    @Getter
    public static class SearchAllInfo {

        private Long userId;
        private String userName;
        private String userInfo;
        private spharos.msg.domain.users.entity.LoginType LoginType;
        private Boolean status;
    }

    @Builder
    @AllArgsConstructor
    @Getter
    public static class ConnectCount {
        private Long connectCount;
    }

    @Builder
    @AllArgsConstructor
    @Getter
    public static class UsersCount {
        private Long usersCount;
    }
}
