package com.bookshop.controllers;

import java.util.List;
import java.util.Optional;

import com.bookshop.dao.User;
import com.bookshop.dto.UserDTO;
import com.bookshop.exceptions.DuplicateRecordException;
import com.bookshop.exceptions.InvalidException;
import com.bookshop.exceptions.NotFoundException;
import com.bookshop.helpers.CheckValid;
import com.bookshop.helpers.ConvertObject;
import com.bookshop.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("@userAuthorizer.authorizeAdmin(authentication, 'ADMIN')")
    public ResponseEntity<?> getAllUsers(@RequestParam(name = "page", required = false) Integer pageNum) {
        if (pageNum != null) {
            Page<User> page = userRepository.findAll(PageRequest.of(pageNum.intValue(), 10));
            if (page.getNumberOfElements() == 0) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            return ResponseEntity.ok().body(page.getContent());
        }
        List<User> users = userRepository.findAll();
        if (users.size() == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok().body(users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@userAuthorizer.authorizeGetUserById(authentication, 'ADMIN', #userId)")
    public ResponseEntity<?> getUserById(@PathVariable("userId") Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (!user.isPresent()) {
            throw new NotFoundException("User not found");
        }
        return ResponseEntity.ok().body(user);
    }

    @PostMapping
    @PreAuthorize("@userAuthorizer.authorizeAdmin(authentication, 'ADMIN')")
    public ResponseEntity<?> createNewUser(@RequestBody UserDTO userDTO) {
        User oldUser = userRepository.findByUsername(userDTO.getUsername());
        if (oldUser != null) {
            throw new DuplicateRecordException("Username has already exists");
        }
        if (CheckValid.isUser(userDTO)) {
            User user = ConvertObject.fromUserDTOToUserDAO(userDTO);

            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            User newUser = userRepository.save(user);

            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        }
        throw new InvalidException("Invalid user");
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("@userAuthorizer.authorizeAdmin(authentication, 'ADMIN')")
    public ResponseEntity<?> editUser(@RequestBody UserDTO userDTO, @PathVariable("userId") Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new NotFoundException("User not found");
        }
        User user = optionalUser.get();

        if (userDTO.getFirstName() != null) {
            user.setFirstName(userDTO.getFirstName());
        }
        if (userDTO.getLastName() != null) {
            user.setLastName(userDTO.getLastName());
        }
        if (userDTO.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        if (userDTO.getAddress() != null) {
            user.setAddress(userDTO.getAddress());
        }
        if (userDTO.getAmount() != null) {
            user.setAmount(userDTO.getAmount());
        }
        if (userDTO.getRole() != null) {
            user.setRole(userDTO.getRole());
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@userAuthorizer.authorizeAdmin(authentication, 'ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable("userId") Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new NotFoundException("User not found");
        }
        userRepository.deleteById(userId);

        return ResponseEntity.status(HttpStatus.OK).body(optionalUser.get());
    }
}
