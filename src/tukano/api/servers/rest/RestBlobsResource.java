package tukano.api.servers.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import tukano.api.rest.RestBlobs;
import tukano.api.servers.java.JavaBlobs;

@Singleton
public class RestBlobsResource implements RestBlobs {
	
	final Blobs impl;
	
	public RestBlobsResource() {
		this.impl = new JavaBlobs();
	}

	@Override
	public void upload(String blobId, byte[] bytes) {
		Result<Void> result = impl.upload(blobId, bytes);
		if (result.isOK())
			return;
		else
			throw new WebApplicationException(statusCodeFrom(result));
	}

	@Override
	public byte[] download(String blobId) {
		return resultOrThrow( impl.download(blobId));
	}
	
	@Override
	public void generate(String blobId) {
		Result<Void> result = impl.generate(blobId);
		if(result.isOK())
			return;
		else
			throw new WebApplicationException(statusCodeFrom(result));
		
	}


	/**
	 * Given a Result<T>, either returns the value, or throws the JAX-WS Exception
	 * matching the error code...
	 */
	protected <T> T resultOrThrow(Result<T> result) {
		if (result.isOK())
			return result.value();
		else
			throw new WebApplicationException(statusCodeFrom(result));
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
