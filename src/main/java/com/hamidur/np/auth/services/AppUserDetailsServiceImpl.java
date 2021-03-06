package com.hamidur.np.auth.services;

import com.hamidur.np.auth.models.User;
import com.hamidur.np.auth.repos.UserRepository;

import com.hamidur.np.exceptions.custom.AccountDisabledException;
import com.hamidur.np.exceptions.custom.UserNameNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class AppUserDetailsServiceImpl implements UserDetailsService
{
    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userRepository.getUserByUsername(s);
        if(user == null) throw new UserNameNotFoundException("No such user with username="+s);
        if(!user.getEnabled()) throw new AccountDisabledException("User is disabled, need to verify their account");
        return new AppUserDetails(user);
    }
}
