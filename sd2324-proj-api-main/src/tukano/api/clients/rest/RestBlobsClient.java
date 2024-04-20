package tukano.api.clients.rest;

import java.net.URI;

import org.glassfish.jersey.client.ClientConfig;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import tukano.api.rest.RestBlobs;
import tukano.api.java.Result.ErrorCode;

public class RestBlobsClient implements Blobs {
		
	final URI serverURI;
	final Client client;
	final ClientConfig config;

	final WebTarget target;
	
	public RestBlobsClient( URI serverURI ) {
		this.serverURI = serverURI;
		this.config = new ClientConfig();
		this.client = ClientBuilder.newClient(config);

		target = client.target( serverURI ).path( RestBlobs.PATH );
	}
	
	@Override
	public Result<Void> upload(String blobId, byte[] bytes) {
		Response r = target.path(blobId).request()
				.post(Entity.entity(bytes, MediaType.APPLICATION_JSON));

		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok();
	}

	@Override
	public Result<byte[]> download(String blobId) {
		Response r = target.path(blobId).request()
				.accept(MediaType.APPLICATION_JSON)
				.get();

		var status = r.getStatus();
		if( status != Status.OK.getStatusCode() )
			return Result.error( getErrorCodeFrom(status));
		else
			return Result.ok( r.readEntity( byte[].class ));
	}
	
	@Override
	public Result<Void> generate(String blobId) {
		Entity<?> empty = Entity.text("");
		Response r = target.path( blobId ).request()
				.accept(MediaType.APPLICATION_JSON)
				.put(empty);
		var status = r.getStatus();
		if( (status != Status.OK.getStatusCode()) && (status != Status.NO_CONTENT.getStatusCode())) {
			return Result.error( getErrorCodeFrom(status));			
		}
		else
			return Result.ok();
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
