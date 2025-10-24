package com.alotra.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginUserModel {

	private String usernameOrEmail;
	
	private String password;
}
