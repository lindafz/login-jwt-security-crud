package com.lindazf.login.jwt.service;

import com.lindazf.login.jwt.entity.Role;
import com.lindazf.login.jwt.entity.User;
import com.lindazf.login.jwt.exception.ApplicationExceptionDetails;
import com.lindazf.login.jwt.exception.ErrorCode;
import com.lindazf.login.jwt.exception.ErrorMessage;
import com.lindazf.login.jwt.model.UserDto;
import com.lindazf.login.jwt.model.UserResponse;
import com.lindazf.login.jwt.repository.RoleRepository;
import com.lindazf.login.jwt.repository.UserRepository;
import com.lindazf.login.jwt.security.JwtProvider;
import com.lindazf.login.jwt.utils.Constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class UserService {
	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private static final String INVALID_USER = "INVALID_USER";
	private static final String ENTITY_CREATE = "Create: ";
	private static final String ENTITY_UPDATE = "Update: ";
//    private static final String ENTITY_DELETE = "Delete: ";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public UserResponse signin(String username, String password) {
		log.info("New user attempting to sign in");
		List<Role> roles = new ArrayList<>();
		UserResponse response = new UserResponse();
		Optional<User> existedUser = findByUserName(username);
		if (!existedUser.isPresent()) {
			response.setAuthCode(INVALID_USER);
			response.setAuthorized(false);
			log.error("invalid user or password : " + username + ", password = " + password);
		} else {
			User user = existedUser.get();			
			if (!passwordEncoder.matches(password, user.getPassword())) {
				response.setAuthCode(INVALID_USER);
				response.setAuthorized(false);
				log.error("invalid password : " + username + ", password = " + password);
				
			} else {
				log.info("user is valid: " + username + ", password = " + password);
				roles.add(user.getRole());
				authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
				String token = jwtProvider.createToken(username, roles);
				int validMinutes = jwtProvider.getValidMinutes();

				response.setAuthCode(token);
				response.setValidMinutes(validMinutes);

				response.setAuthorized(true);
			}
		}
		return response;
	}

	public String encodePassword(String passwordText) {
		return passwordEncoder.encode(passwordText);
	}

	public List<User> findAllUsers() {
		return userRepository.findAll();
	}

	public List<Role> findAllRoles() {
		return roleRepository.findAll();
	}

	public List<Role> saveRoles(List<Role> roles) {
		return roleRepository.saveAll(roles);
	}

	public Role findByRoleName(String name) {
		return roleRepository.findByRoleName(name).get();
	}

	public Optional<User> findByUserName(String userName) {
		return userRepository.findByUserName(userName);
	}

	public void deleteUserById(Long userId) throws NoSuchElementException {
		log.warn("Delete user for userId = " + userId);
		userRepository.deleteById(userId);
	}

	public User createUser(UserDto dto) throws ApplicationExceptionDetails {
		User user = convertModelToEntity(dto);
		String result = ENTITY_CREATE + user.getUserName();
		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		log.info(result);
		return userRepository.save(user);
	}

	//User should able to remember his/her password
	public User updateUser(UserDto dto) throws ApplicationExceptionDetails {
		User user = convertModelToEntity(dto);
		String result = ENTITY_UPDATE + user.getUserName();
		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		log.info(result);	
		return userRepository.save(user);
	}
	
	private User convertModelToEntity(UserDto dto) throws ApplicationExceptionDetails{
		User user = new User();
		String userName = dto.getUserName();
		String roleName = dto.getRoleName();
		
		Optional<User> dbUser = findByUserName(userName);
		if(dbUser.isPresent()) {
			user.setUserId(dbUser.get().getUserId());
		}
		
		Role dbRole = roleRepository.findByRoleName(roleName).orElseThrow(() -> new ApplicationExceptionDetails(ErrorMessage.ROLE_NOT_EXIST, ErrorCode.NOT_FOUND));
		user.setRole(dbRole);
		user.setPassword(dto.getPassword());
		user.setUserName(userName);
		user.setFullName(dto.getFullName());
		return user;
	}
	
	/**
	 * Password set here is only for demo project. Real project should 
	 * never set password this way
	 * This demo project does not have the frontend
	 * @param user
	 * @return
	 * @throws ApplicationExceptionDetails
	 */
	private UserDto converEntityToModel(User user) throws ApplicationExceptionDetails{
		UserDto dto = new UserDto();
		dto.setUserId(user.getUserId());
		dto.setUserName(user.getUserName());
		//password should not set here in the real world
		dto.setPassword(user.getPassword());
		dto.setFullName(user.getFullName());
		dto.setRoleName(user.getRole().getRoleName());
		return dto;
	}
}
