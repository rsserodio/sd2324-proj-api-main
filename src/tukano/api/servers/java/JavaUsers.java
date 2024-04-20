package tukano.api.servers.java;

import java.util.HashMap;
import java.util.List;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.User;
import tukano.api.clients.rest.RestShortsClient;
import tukano.api.discovery.Discovery;
import tukano.api.java.Result.ErrorCode;
import tukano.api.java.Result;
import tukano.api.java.Users;

@Singleton
public class JavaUsers implements Users {
	
	private Map<String,User> users = new HashMap<>();

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	@Override
	public Result<String> createUser(User user) {
		Log.info("createUser : " + user);
		
		// Check if user data is valid
		if(user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null) {
			Log.info("User object invalid.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		// Insert user, checking if name already exists
		if( users.putIfAbsent(user.userId(), user) != null ) {
			Log.info("User already exists.");
			return Result.error( ErrorCode.CONFLICT);
		}
		return Result.ok( user.userId() );
	}

	@Override
	public Result<User> getUser(String userId, String pwd) {
		Log.info("getUser : user = " + userId + "; pwd = " + pwd);
		
		// Check if user is valid
		if(userId == null || pwd == null) {
			Log.info("Name or Password null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		User user = users.get(userId);			
		// Check if user exists 
		if( user == null ) {
			Log.info("User does not exist.");
			return Result.error( ErrorCode.NOT_FOUND);
		}
		
		//Check if the password is correct
		if( !user.pwd().equals( pwd)) {
			Log.info("Password is incorrect.");
			return Result.error( ErrorCode.FORBIDDEN);
		}
		
		return Result.ok(user);
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + pwd);
		
		// Check if user is valid
		if(userId == null || pwd == null || user.getUserId() != null) {
			Log.info("Name or Password null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		User userAux = users.get(userId);			
		// Check if user exists 
		if( userAux == null ) {
			Log.info("User does not exist.");
			return Result.error( ErrorCode.NOT_FOUND);
		}
		
		//Check if the password is correct
		if( !userAux.pwd().equals( pwd)) {
			Log.info("Password is incorrect.");
			return Result.error( ErrorCode.FORBIDDEN);
		}
		
		if(user.getDisplayName() != null) {
			userAux.setDisplayName(user.getDisplayName());
		}
		if(user.getEmail() != null) {
			userAux.setEmail(user.getEmail());
		}
		if(user.getPwd() != null) {
			userAux.setPwd(user.getPwd());
		}
		
		users.put(userAux.getUserId(), userAux);
		
		return Result.ok(userAux);
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + pwd);
		
		// Check if user is valid
		if(userId == null || pwd == null) {
			Log.info("Name or Password null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
				
		User userAux = users.get(userId);			
		// Check if user exists 
		if( userAux == null ) {
			Log.info("User does not exist.");
			return Result.error( ErrorCode.NOT_FOUND);
		}
				
		//Check if the password is correct
		if( !userAux.pwd().equals( pwd)) {
			Log.info("Password is incorrect.");
			return Result.error( ErrorCode.FORBIDDEN);
		}
				
		users.remove(userAux.userId());
		
		return Result.ok(userAux);
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info("searchUsers: " + pattern);
		
		if(pattern == null) {
			Log.info("Pattern null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		List<User> listOfUsers = new ArrayList<User>();
		
		for (Entry<String, User> e : users.entrySet()) {
			if(e.getKey().toUpperCase().contains(pattern.toUpperCase()) || pattern.equals("")) {
				User u = e.getValue();
				String pass = u.getPwd();
				u.setPwd("");
				listOfUsers.add(u);
				u.setPwd(pass);
			}
		}
		
		return Result.ok(listOfUsers);
	}

	@Override
	public Result<Boolean> userExists(String userId) {
		Log.info("Exists?: " + userId);
		
		if(userId == null) {
			Log.info("id null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		User u = users.get(userId);
		if(u == null) {
			return Result.ok(false);
		} else return Result.ok(true);
	}

	
	
	private RestShortsClient getShortClient() {
		Discovery d = Discovery.getInstance();
		URI[] auxURI = d.knownUrisOf("shorts", 1);
		URI serverUrl = auxURI[auxURI.length-1];
		
		RestShortsClient rShortsClient = new RestShortsClient(serverUrl);
		
		return rShortsClient;
	}
	
	
	
	/**
	 * Translates a Result<T> to a HTTP Status code
	 */
	private static Status statusCodeFrom(Result<?> result) {
		return switch (result.error()) {
			case CONFLICT -> Status.CONFLICT;
			case NOT_FOUND -> Status.NOT_FOUND;
			case FORBIDDEN -> Status.FORBIDDEN;
			case BAD_REQUEST -> Status.BAD_REQUEST;
			case INTERNAL_ERROR -> Status.INTERNAL_SERVER_ERROR;
			case NOT_IMPLEMENTED -> Status.NOT_IMPLEMENTED;
			case OK -> result.value() == null ? Status.NO_CONTENT : Status.OK;
			default -> Status.INTERNAL_SERVER_ERROR;
		};
	}
	
}
