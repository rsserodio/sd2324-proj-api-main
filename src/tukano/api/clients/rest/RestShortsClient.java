package tukano.api.clients.rest;

import java.net.URI;
import java.util.List;

import org.glassfish.jersey.client.ClientConfig;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.Short;
import tukano.api.java.Result;
import tukano.api.java.Shorts;
import tukano.api.java.Result.ErrorCode;
import tukano.api.rest.RestShorts;

public class RestShortsClient implements Shorts {
	
	final URI serverURI;
	final Client client;
	final ClientConfig config;

	final WebTarget target;
	
	public RestShortsClient( URI serverURI ) {
		this.serverURI = serverURI;
		this.config = new ClientConfig();
		this.client = ClientBuilder.newClient(config);

		target = client.target( serverURI ).path( RestShorts.PATH );
	}

	@Override
	public Result<Short> createShort(String userId, String password) {
		Response r = target.path(userId)
				.queryParam(RestShorts.PWD, password)
				.request()
				.post(null);
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok( r.readEntity( Short.class ) );
	}

	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Response r = target.path( shortId )
				.queryParam(RestShorts.PWD, password).request()
				.delete();

		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() || status != Status.NO_CONTENT.getStatusCode())
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok();
	}

	@Override
	public Result<Short> getShort(String shortId) {
		Response r = target.path(shortId)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok( r.readEntity( Short.class ) );
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Response r = target.path(userId + RestShorts.SHORTS)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok( r.readEntity(new GenericType<List<String>>() {}) );
	}

	@Override
	public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
		Response r = target.path(userId1 + '/' + userId2 + RestShorts.FOLLOWERS)
				.queryParam(RestShorts.PWD, password)
				.request()
				.post(Entity.entity(isFollowing, MediaType.APPLICATION_JSON));
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok();
	}

	@Override
	public Result<List<String>> followers(String userId, String password) {
		Response r = target.path(userId + RestShorts.FOLLOWERS)
				.queryParam(RestShorts.PWD, password)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok( r.readEntity(new GenericType<List<String>>() {}) );
	}

	@Override
	public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
		Response r = target.path(shortId + '/' + userId + RestShorts.LIKES)
				.queryParam(RestShorts.PWD, password)
				.request()
				.post(Entity.entity(isLiked, MediaType.APPLICATION_JSON));
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok();
	}

	@Override
	public Result<List<String>> likes(String shortId, String password) {
		Response r = target.path(shortId + RestShorts.LIKES)
				.queryParam(RestShorts.PWD, password)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok( r.readEntity(new GenericType<List<String>>() {}) );
	}
	
	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Response r = target.path(userId + RestShorts.FEED)
				.queryParam(RestShorts.PWD, password)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get();
		
		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok( r.readEntity(new GenericType<List<String>>() {}) );
	}
	
	public static ErrorCode getErrorCodeFrom(int status) {
		return switch (status) {
		case 200, 209 -> ErrorCode.OK;
		case 409 -> ErrorCode.CONFLICT;
		case 403 -> ErrorCode.FORBIDDEN;
		case 404 -> ErrorCode.NOT_FOUND;
		case 400 -> ErrorCode.BAD_REQUEST;
		case 500 -> ErrorCode.INTERNAL_ERROR;
		case 501 -> ErrorCode.NOT_IMPLEMENTED;
		default -> ErrorCode.INTERNAL_ERROR;
		};
	}



}
