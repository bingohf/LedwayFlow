package com.ledway;

import android.content.SharedPreferences;

public class LoginInfo {
	private static LoginInfo _loginInfo = null;
	private String name,server;
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	private LoginInfo(){

	}
	public static LoginInfo getInstance(){
		if (_loginInfo == null){
			_loginInfo = new LoginInfo();
		}
		return _loginInfo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	
}
