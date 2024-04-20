package tukano.api.servers.java;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.TreeMap;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.Short;
import tukano.api.User;
import tukano.api.clients.rest.RestUsersClient;
import tukano.api.clients.rest.RestBlobsClient;
import tukano.api.java.Result;
import tukano.api.java.Shorts;
import tukano.api.java.Result.ErrorCode;
import tukano.api.discovery.*;

@Singleton
public class JavaShorts implements Shorts {
	
	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());
	
	//userId, Arraylist<String> /// followed, followers
	private Map<String, ArrayList<String>> followers = new HashMap<String, ArrayList<String>>();
	
	private Map<String, ArrayList<String>> following = new HashMap<String, ArrayList<String>>();
	
	//userId, Arraylist<Short>
	private Map<String, ArrayList<Short>> shorts = new HashMap<String, ArrayList<Short>>();
	
	//shortId, Short
	private Map<String, Short> shortGlossary = new HashMap<String, Short>();
	
	//shortid, likes of that shortid under userids
	private Map<String, ArrayList<String>> likes = new HashMap<String, ArrayList<String>>();
	
	@Override
	public Result<Short> createShort(String userId, String password) {
		Log.info("createShort: " + userId);
		
		if(userId == null || password == null) {
			Log.info("userId or password null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}

		Result<User> resultUser = this.getUserConfirmation(userId, password);
		
		if (resultUser.isOK()) {
			//user exists, create the shorts
			
			//create blob
			Discovery d = Discovery.getInstance();
			URI[] auxURI = d.knownUrisOf("blobs", 1);
			URI serverUrl = auxURI[auxURI.length-1];
			RestBlobsClient rBlobsClient = new RestBlobsClient(serverUrl);
			
			String blobShortId = userId + System.currentTimeMillis();
			Result<Void> resultBlob = rBlobsClient.generate(blobShortId);
			
			if(resultBlob.isOK()) {
				//blob created
				//create short
				Short s = new Short(blobShortId, userId, serverUrl.toString() + "/" + blobShortId, System.currentTimeMillis(), 0);
				
				ArrayList<Short> auxList = shorts.get(userId);
				if(auxList == null) {
					auxList = new ArrayList<Short>();
				}
				auxList.add(s);
				shorts.put(userId, auxList);
				shortGlossary.putIfAbsent(blobShortId, s);
				
				return Result.ok(s);
				
			} else throw new WebApplicationException(statusCodeFrom(resultBlob));
			
		} else throw new WebApplicationException(statusCodeFrom(resultUser));
		
	}

	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Log.info("deleteShort: " + shortId);
		
		if(shortId == null || password == null) {
			Log.info("shortId or password null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		String userId = shortGlossary.get(shortId).getOwnerId();
		if(userId == null) 
			return Result.error( ErrorCode.NOT_FOUND);
		
		Result<User> resultUser = this.getUserConfirmation(userId, password);

		if(resultUser.isOK()) {
			shortGlossary.remove(shortId);
			ArrayList<Short> userShorts = shorts.get(userId);
			ArrayList<Short> newShorts = new ArrayList<Short>();
			for(Short s : userShorts) {
				if (!s.getShortId().equals(shortId)) {
					newShorts.add(s);
				}
			}
			shorts.put(userId, newShorts);
			return Result.ok();
		} else throw new WebApplicationException(statusCodeFrom(resultUser));
	}

	@Override
	public Result<Short> getShort(String shortId) {
		Log.info("getShort: " + shortId);
		
		if(shortId == null) {
			Log.info("shortId null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		Short s = shortGlossary.get(shortId);
		if(s == null) return Result.error(ErrorCode.NOT_FOUND);
		String userId = s.getOwnerId();
		
		if(userId != null) {
			return Result.ok(s);
		} else return Result.error( ErrorCode.NOT_FOUND);
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info("getShortsFromUser: " + userId);
		
		if(userId == null) {
			Log.info("userId null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
				
		Result<Boolean> result = this.isUserHere(userId);
		
		if(result.isOK()) {
			
			if(result.value()) {
				ArrayList<Short> auxList = shorts.get(userId);
				if(auxList == null) {
					return Result.ok(new ArrayList<String>());
				} else {
					ArrayList<String> auxReturn = new ArrayList<String>(); 
					for(Short s : auxList) {
						auxReturn.add(s.getShortId());
					}
					return Result.ok(auxReturn);
				}
				
			} else return Result.error(ErrorCode.NOT_FOUND);
			
		} else throw new WebApplicationException(statusCodeFrom(result));
		
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
		Log.info("follow: " + userId2 + "by" + userId1 + " isFollowing: " + isFollowing);
		
		if(userId1 == null || userId2 == null || password == null) {
			Log.info("follow null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		Result<User> resultUser = this.getUserConfirmation(userId1, password);
		
		if(resultUser.isOK()) {
			
			Result<Boolean> resultFollowed = this.isUserHere(userId2);
			
			if(resultFollowed.isOK()) {
				
				ArrayList<String> userFollows = followers.get(userId2);
				if(userFollows == null) {
					userFollows = new ArrayList<String>();
				}
				
				if(isFollowing) {
				
					if(userFollows.contains(userId1)) {
						return Result.error(ErrorCode.CONFLICT);
					}
					//add following
					ArrayList<String> followingUser = following.get(userId1);
					if(followingUser == null) {
						followingUser = new ArrayList<>();
					}
					followingUser.add(userId2);
					following.put(userId1, followingUser);
					//
					
					userFollows.add(userId1);
					followers.put(userId2, userFollows);
					return Result.ok();
					
				} else {
	
					if(userFollows.contains(userId1)) {
						
						//remove following
						ArrayList<String> followingUser = following.get(userId1);
						if(followingUser == null) {
							throw new WebApplicationException(Status.BAD_REQUEST);
						}
						followingUser.remove(userId2);
						following.put(userId1, followingUser);
						//
						
						userFollows.remove(userId1);
						followers.put(userId2, userFollows);
						return Result.ok();
					} else return Result.ok();
					
				}
			} else throw new WebApplicationException(statusCodeFrom(resultFollowed));
		} else throw new WebApplicationException(statusCodeFrom(resultUser));		
	}

	@Override
	public Result<List<String>> followers(String userId, String password) {
		Log.info("followers: " + userId);
		
		if(userId == null) {
			Log.info("followers null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		Result<User> resultUser = this.getUserConfirmation(userId, password);
		
		if(resultUser.isOK()) {
			
			ArrayList<String> fol = followers.get(userId);
			if(fol == null){
				return Result.ok(new ArrayList<String>());
			} else return Result.ok(fol);
			
		} else throw new WebApplicationException(statusCodeFrom(resultUser));
	}

	@Override
	public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
		Log.info("like: " + userId + " short: " + shortId + " isLiked:" + isLiked);
		
		if(userId == null || shortId == null || password == null) {
			Log.info("like null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		Result<User> resultUser = this.getUserConfirmation(userId, password);
		
		if(resultUser.isOK()) {
			
			Short s = shortGlossary.get(shortId);
		
			if(s == null) return Result.error(ErrorCode.BAD_REQUEST);
			
			ArrayList<String> usersLiking = likes.get(shortId);
			if(usersLiking == null) {
				usersLiking = new ArrayList<String>();
			}
			
			if(isLiked) {
				
				if(usersLiking.contains(userId)) {
					return Result.error(ErrorCode.CONFLICT);
				}
				
				s.setTotalLikes(s.getTotalLikes()+1);
				shortGlossary.put(shortId, s);
				ArrayList<Short> auxArr = shorts.get(s.getOwnerId());
				for(Short sh : auxArr) {
					if(sh.getShortId().equals(shortId)) {
						sh.setTotalLikes(s.getTotalLikes());
					}
				}
				usersLiking.add(userId);
				likes.put(shortId, usersLiking);
				return Result.ok();
				
			} else {
				
				if(!usersLiking.contains(userId)) {
					return Result.error(ErrorCode.NOT_FOUND);
				}
				
				s.setTotalLikes(s.getTotalLikes()-1);
				shortGlossary.put(shortId, s);
				ArrayList<Short> auxArr = shorts.get(s.getOwnerId());
				for(Short sh : auxArr) {
					if(sh.getShortId().equals(shortId)) {
						sh.setTotalLikes(s.getTotalLikes());
					}
				}
				usersLiking.remove(userId);
				likes.put(shortId, usersLiking);
				return Result.ok();
			}
		} else throw new WebApplicationException(statusCodeFrom(resultUser));
	}

	@Override
	public Result<List<String>> likes(String shortId, String password) {
		Log.info("likes: " + shortId );
		
		if(shortId == null || password == null) {
			Log.info("likes null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		Short s = shortGlossary.get(shortId);
		if(s == null) {
			return Result.error(ErrorCode.NOT_FOUND);
		}
		
		Result<User> resultUser = this.getUserConfirmation(s.getOwnerId(), password);
		
		if(resultUser.isOK()){
			
			ArrayList<String> likesGet = likes.get(shortId);
			if(likesGet == null || likesGet.size() <= 0) {
				return Result.ok(new ArrayList<String>());
			} 
			
			return Result.ok(likesGet);
			
		} else throw new WebApplicationException(statusCodeFrom(resultUser));
	}

	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Log.info("getFeed: " + userId);
		
		if(userId == null) {
			Log.info("getFeed null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		Result<User> resultUser = this.getUserConfirmation(userId, password);
		
		if(resultUser.isOK()) {
			
			ArrayList<String> followingUsers = following.get(userId);
			
			if(followingUsers == null) {
				followingUsers = new ArrayList<String>();
			}
			
			//own person feed too for some reason
			followingUsers.add(userId);
			
			TreeMap<String,String> ordered = new TreeMap<String,String>();
			
			for(String userString : followingUsers) {
				
				ArrayList<Short> shortsThisOne = shorts.get(userString);
				if(shortsThisOne != null) {
					for(Short s : shortsThisOne) {
						ordered.put((s.getShortId().replace(s.getOwnerId(),"").trim()), s.getShortId());
					}
				}
				
			}
			
			if(ordered.isEmpty()) {
				return Result.ok(new ArrayList<String>());
			}
			
			TreeMap<String,String> orderedInv = new TreeMap<>(Collections.reverseOrder());
			orderedInv.putAll(ordered);
			
			return Result.ok(new ArrayList<String>(orderedInv.values()));
			
		} else throw new WebApplicationException(statusCodeFrom(resultUser));
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
	
	
	private Result<User> getUserConfirmation(String userId, String password){
		RestUsersClient rUsersClient = this.getUserClient();
		
		Result<User> resultUser = rUsersClient.getUser(userId, password);
		
		return resultUser;
	}
	
	private Result<Boolean> isUserHere(String userId){
		RestUsersClient rUsersClient = this.getUserClient();
		
		Result<Boolean> resultUser = rUsersClient.userExists(userId);
		return resultUser;
	}
	
	private RestUsersClient getUserClient() {
		Discovery d = Discovery.getInstance();
		URI[] auxURI = d.knownUrisOf("users", 1);
		URI serverUrl = auxURI[auxURI.length-1];
		
		RestUsersClient rUsersClient = new RestUsersClient(serverUrl);
		
		return rUsersClient;
	}
	
	@SuppressWarnings("unused")
	private RestBlobsClient getBlobsClient() {
		Discovery d = Discovery.getInstance();
		URI[] auxURI = d.knownUrisOf("blobs", 1);
		URI serverUrl = auxURI[auxURI.length-1];
		
		RestBlobsClient rBlobsClient = new RestBlobsClient(serverUrl);
		
		return rBlobsClient;
	}


	

}
