package tukano.api.servers.java;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import tukano.api.java.Result.ErrorCode;

@Singleton
public class JavaBlobs implements Blobs {
	
	private Map<String,byte[]> blobs = new HashMap<>();

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	@Override
	public Result<Void> upload(String blobId, byte[] bytes) {
		Log.info("upload: " + blobId);
	
		if(blobId == null) {
			Log.info("blobId or bytes null.");
			return Result.error( ErrorCode.FORBIDDEN);
		}
		
		if(!blobs.containsKey(blobId)) {
			return Result.error( ErrorCode.FORBIDDEN);
		}

		
		byte[] aux = blobs.get(blobId);
		
		if(aux == null || aux == bytes) {
			blobs.put(blobId, bytes);
			return Result.ok();
		} else {
			Log.info("Bad byte match.");
			return Result.error( ErrorCode.CONFLICT);
		}
	}

	@Override
	public Result<byte[]> download(String blobId) {
		Log.info("Downloading: "+blobId);
		
		if(blobId == null) {
			Log.info("blobId is null.");
			return Result.error( ErrorCode.NOT_FOUND);
		}
		
		byte[] aux = blobs.get(blobId);
		if(aux == null) {
			Log.info("Blob does not exist.");
			return Result.error( ErrorCode.NOT_FOUND);
		} 
		
		return Result.ok(aux);
	}
	
	public Result<Void> generate(String blobId){
		
		if(blobId == null) {
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		blobs.put(blobId, null);
		
		return Result.ok();
	}
}
