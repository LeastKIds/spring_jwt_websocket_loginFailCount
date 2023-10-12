package com.example.jwt.validation.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.jwt.dto.request.auth.AuthenticationRequest;
import com.example.jwt.error.ErrorResponse;
import com.example.jwt.repository.auth.UserRepository;
import com.example.jwt.util.redis.service.RedisLoginFailCountService;

public class AuthenticateValidation {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticateValidation.class);

    public ErrorResponse loginValidation(AuthenticationRequest request) {
        Map<String, String> errors = new HashMap<>();

        if(request.getEmail() != null) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            Pattern emailPattern = Pattern.compile(emailRegex);
            Matcher emailMatcher = emailPattern.matcher(request.getEmail());
            if(!emailMatcher.matches()) {
                errors.put("email", "Invalid email format.");
                logger.error(String.format("Invalid email format. \n email: %s", request.getEmail()));
            }
                
        } else {
            errors.put("email", "Invalid email format.");
            logger.error(String.format("Invalid email format. \n email: %s", request.getEmail()));
        }

        if(request.getPassword() == null || request.getPassword().length() < 4 || request.getPassword().length() > 20) {
            errors.put("password", "The password must be between 4 and 20 characters long.");
            logger.error(String.format("The password must be between 4 and 20 characters long. \n password: %s", request.getPassword()));
        }

        return ErrorResponse.builder().errors(errors).build();
        
    }

    public ErrorResponse authValidation(UserRepository repository, RedisLoginFailCountService redisLoginFailCountService, String userEmail) {
        
        
        Map<String, String> errors = new HashMap<>();

        if(repository.findByEmail(userEmail).isPresent()) {
            int count = redisLoginFailCountService.getLoginFailCount(userEmail);
        
            // 계정 락
            if(count + 1 >= 5) {

                var user = repository.findByEmail(userEmail).get();
                user.setIsAccountNonLocked(false);
                repository.save(user);

                count += 1;
                redisLoginFailCountService.deleteLoginFailCount(userEmail);
                logger.error("account lock");
            } else {
                if(count == -1) // 최초로 틀렸을 시 
                    count = 1;
                else    // 이미 데이터가 존재할 경우
                    count += 1;
                redisLoginFailCountService.setLoginFailCount(userEmail, count);
            } 

            errors.put("auth_validation", "Your password is incorrect.");
            logger.error(String.format("The username and password are incorrect."));
            errors.put("login_fail_count", String.valueOf(count));
            logger.error(String.format(userEmail + " login fail count: " + count));
        } else {
            errors.put("auth_validation", "The username and password are incorrect.");
            logger.error(String.format("The username and password are incorrect."));
        }

        
        
        

        return ErrorResponse.builder().errors(errors).build();
    }

    public ErrorResponse diableAuthError() {
        Map<String, String> errors = new HashMap<>();
        errors.put("auth_disable", "Account is disabled");
        logger.error(String.format("Account is disabled"));

        return ErrorResponse.builder().errors(errors).build();
    }

    public ErrorResponse lookedAuthError() {
        Map<String, String> errors = new HashMap<>();
        errors.put("auth_looked", "Account is locked");
        logger.error("Account is locked");

        return ErrorResponse.builder().errors(errors).build();
    }

    public ErrorResponse authError() {
        Map<String, String> errors = new HashMap<>();
        errors.put("auth", "An error occurred while trying to authenticate");
        logger.error("An error occurred while trying to authenticate");

        return ErrorResponse.builder().errors(errors).build();
    }

    public ErrorResponse notFoundUserError() {
        Map<String, String> errors = new HashMap<>();
        errors.put("user", "The user cannot be found in the database.");
        logger.error("The user cannot be found in the database.");

        return ErrorResponse.builder().errors(errors).build();
    }
    
}
