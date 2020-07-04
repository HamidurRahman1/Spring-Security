package com.hamidur.ss.auth.services;

import com.hamidur.ss.auth.models.Role;
import com.hamidur.ss.auth.models.User;
import com.hamidur.ss.auth.repos.RoleRepository;
import com.hamidur.ss.auth.repos.UserRepository;
import com.hamidur.ss.exceptions.custom.ConstraintViolationException;
import com.hamidur.ss.exceptions.custom.MissingAttribute;
import com.hamidur.ss.exceptions.custom.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService
{
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserService(final UserRepository userRepository, final RoleRepository roleRepository, final PasswordEncoder passwordEncoder)
    {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User insertUser(User user) throws MissingAttribute
    {
        if(user.getRoles() == null || user.getRoles().isEmpty())
            throw new MissingAttribute("At least 1 role be assigned to this user");
        try
        {
            System.out.println(user);
            System.out.println(user.getRoles());
            if(userRepository.insertUserEntity(user.getFirstName(), user.getLastName(), user.getUsername(), passwordEncoder.encode(user.getPassword()), user.getEnabled()) >= 1)
            {
                User savedUser = userRepository.getUserByUsername(user.getUsername());
                Set<Role> roles = roleRepository.getAll();
                Set<Role> assignedRoles = new HashSet<>();
                roles.forEach(role -> {
                    if(user.getRoles().contains(role)) assignedRoles.add(role);
                });
                savedUser.setRoles(assignedRoles);
                return userRepository.save(savedUser);
            }
            return null;
        }
        catch (DataIntegrityViolationException ex)
        {
            throw new ConstraintViolationException("A user with username="+user.getUsername()+" already exists");
        }
    }

    public boolean revokeRole(Integer userId, Integer roleId)
    {
        return userRepository.revokeRole(userId, roleId) >= 1;
    }

    public boolean addRole(Integer userId, Integer roleId) throws ConstraintViolationException
    {
        try
        {
            return userRepository.addRole(userId, roleId) >= 1;
        }
        catch (DataIntegrityViolationException e)
        {
            if(e.getMessage().toUpperCase().contains("USERS_ROLES FOREIGN KEY"))
                throw new ConstraintViolationException("Invalid id provided for userId or roleId. provided userId="+userId+", roleId="+roleId);
            else if(e.getMessage().toUpperCase().contains("PRIMARY_KEY"))
                throw new ConstraintViolationException("User with id="+userId+" already has the role with roleId="+roleId);
            else
                throw e;
        }
    }

    public boolean deleteUserById(Integer userId)
    {
        Integer authorId = userRepository.isUserAnAuthor(userId);
        if(authorId == null)
            return userRepository.deleteUserById(userId) >= 1;
        return userRepository.deleteAllInfoByUserId(authorId, userId) >= 1;
    }

    public User updateUser(User user) throws MissingAttribute, NotFoundException, ConstraintViolationException
    {
        if(user.getUserId() == null)
            throw new MissingAttribute("userId must be present to update User attributes");
        Optional<User> dbUser = userRepository.findById(user.getUserId());
        if(!dbUser.isPresent())
            throw new NotFoundException("No user found with userId="+user.getUserId());
        else
        {
            try
            {
                User user1 = dbUser.get();
                user1.setFirstName(user.getFirstName());
                user1.setLastName(user.getLastName());
                user1.setUsername(user.getUsername());
                user1.setPassword(passwordEncoder.encode(user.getPassword()));
                user1.setEnabled(user.getEnabled());
                return userRepository.save(user1);
            }
            catch (DataIntegrityViolationException ex)
            {
                throw new ConstraintViolationException("username="+user.getUsername()+" is already in use");
            }
        }
    }

    public boolean deactivateUserAccount(Integer userId) throws NotFoundException
    {
        Optional<User> optionalUser = userRepository.findById(userId);
        if(!optionalUser.isPresent())
            throw new NotFoundException("No user with id="+userId+" found to deactivate associated account");
        else
        {
            User user = optionalUser.get();
            user.setEnabled(false);
            return !userRepository.save(user).getEnabled();
        }
    }
}
