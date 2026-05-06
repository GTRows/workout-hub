package com.workouthub.users;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.users.dto.UpdateProfileRequest;
import com.workouthub.users.dto.UserMeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Current-user profile read, update, and account management.")
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/me")
    public UserMeResponse getMe(@AuthenticationPrincipal AppUserPrincipal principal) {
        return usersService.getMe(principal.userId());
    }

    @PutMapping("/me")
    public UserMeResponse updateMe(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest req) {
        return usersService.updateMe(principal.userId(), req);
    }
}
