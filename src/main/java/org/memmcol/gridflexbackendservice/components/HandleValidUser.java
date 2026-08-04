package org.memmcol.gridflexbackendservice.components;


import org.memmcol.gridflexbackendservice.mapper.AuthMapper;
import org.memmcol.gridflexbackendservice.model.user.CustomUserPrincipal;
import org.memmcol.gridflexbackendservice.model.user.UserModel;
import org.memmcol.gridflexbackendservice.exception.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class HandleValidUser {

    private static AuthMapper staticOperatorMapper;

    @Autowired
    public void setOperatorMapper(AuthMapper operatorMapper) {
        HandleValidUser.staticOperatorMapper = operatorMapper;
    }

    public static UserModel handleUserValidation() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = null;

        if (authentication != null &&
                authentication.getPrincipal() instanceof CustomUserPrincipal principal) {

            username = principal.getUsername();
        }

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal) {
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            username = principal.getUsername();
        }
        if(username == null || username.isEmpty()) {
            System.out.println("Throwing Username not found");
            throw new GlobalExceptionHandler.NotFoundException("Username not found");
        }
        UserModel user = staticOperatorMapper.findAuthByUserEmail(username);

        if (user == null) {
            throw new GlobalExceptionHandler.NotFoundException("User not found");
        }

        if (!user.getStatus()) {
            throw new LockedException("Access has been revoked");
        }

        return user;
    }
}