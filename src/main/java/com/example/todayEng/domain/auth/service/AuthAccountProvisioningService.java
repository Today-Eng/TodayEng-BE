package com.example.todayEng.domain.auth.service;

import com.example.todayEng.domain.user.entity.AuthAccount;
import com.example.todayEng.domain.user.entity.User;
import com.example.todayEng.domain.user.entity.enums.AuthProvider;
import com.example.todayEng.domain.user.repository.AuthAccountRepository;
import com.example.todayEng.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthAccountProvisioningService {
    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuthAccount create(AuthProvider provider, String providerSubject, String email) {
        User user = userRepository.save(
                provider == AuthProvider.GOOGLE ? User.create(email) : User.create());
        AuthAccount account = provider == AuthProvider.GOOGLE
                ? AuthAccount.google(user, providerSubject)
                : AuthAccount.test(user, providerSubject);
        return authAccountRepository.saveAndFlush(account);
    }
}
