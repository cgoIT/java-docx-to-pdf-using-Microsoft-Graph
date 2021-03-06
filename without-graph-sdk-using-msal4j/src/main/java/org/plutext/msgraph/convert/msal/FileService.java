/*
 *  Copyright 2020, Plutext Pty Ltd.
 *   
    This module is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */

package org.plutext.msgraph.convert.msal;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.jdk.JDKHttpClient;
import com.github.scribejava.core.httpclient.jdk.JDKHttpClientConfig;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

public class FileService {
	
	private static final Logger log = LoggerFactory.getLogger(FileService.class);

    public FileService(ConfidentialClientApplication authenticationService) {
        this.authenticationService = authenticationService;
    }
	
	public FileService(ConfidentialClientApplication authenticationService, HttpClient httpClient) {
        this.authenticationService = authenticationService;
		this.httpClient = httpClient;
	}
	
    private final ConfidentialClientApplication authenticationService;
    private HttpClient httpClient;
    private IAuthenticationResult authResult = null;


    private CompletableFuture<HttpClient> getHttpClient() {
    	
    	return CompletableFuture.supplyAsync(new Supplier<HttpClient>() {
    		
    		//@Override
    		public HttpClient get() {

    	        if (httpClient != null) return httpClient;
    	        
    	        httpClient = new JDKHttpClient(JDKHttpClientConfig.defaultConfig()); // uses HttpURLConnection, but not async
    	        log.info("Using HTTP client implementation: " + httpClient.getClass().getName() );
    	        return httpClient;
    		}
    	});
    	
    }
    
    private Future<IAuthenticationResult> getBearerToken() {
    	    	        
	    	Set<String> scopes = new HashSet<String>();
	    	scopes.add("https://graph.microsoft.com/.default"); // see https://stackoverflow.com/questions/51781898/aadsts70011-the-provided-value-for-the-input-parameter-scope-is-not-valid 
//    	    	not scopes.add("Files.ReadWrite.All"); 
//    	    	com.microsoft.aad.msal4j.MsalServiceException: AADSTS70011: The provided request must include a 'scope' input parameter. 
//    	    	The provided value for the input parameter 'scope' is not valid. 
//    	    	The scope openid profile offline_access Files.ReadWrite.All openid is not valid.
	    	ClientCredentialParameters ccParameters = ClientCredentialParameters.builder(scopes).build();
	    	
	    	return authenticationService.acquireToken(ccParameters);
    	
    }
        
    public Future<Boolean> uploadStreamAsync(String requestUrl, byte[] bodyContents, String contentType) throws InterruptedException, ExecutionException {
    	
    	HttpClient client = getHttpClient().get();
    	

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("ContentType",  contentType);
        
        if (authResult == null) {
        	authResult = getBearerToken().get();
        }
        headers.put("Authorization",  "Bearer " + authResult.accessToken() );
        log.debug(authResult.accessToken());
        // 'Accept':'application/json;odata.metadata=minimal'}
        headers.put("Accept",  "application/json;odata.metadata=minimal");
        
      log.debug(requestUrl);
      OAuthRequest.ResponseConverter uploadResponseConverter = new UploadResponseConverter(); 
      OAuthAsyncRequestCallback callback = new UploadOAuthAsyncRequestCallback(); 
        return client.executeAsync("ScribeJava", headers, Verb.PUT, requestUrl, bodyContents, 
        		callback,  uploadResponseConverter);
        		
    }

    public Future<Boolean> uploadStreamAsync(String requestUrl, File bodyContents, String contentType) throws InterruptedException, ExecutionException, IOException {
    	
    	HttpClient client = getHttpClient().get();
    	log.debug(client.getClass().getName());
        if (client.getClass().getName().equals("com.github.scribejava.core.httpclient.jdk.JDKHttpClient")) {
        	//java.lang.UnsupportedOperationException: JDKHttpClient does not support File payload for the moment
        	log.debug( client.getClass().getName() + "does not support File payload; reading byte[] " ); 
    		byte[] docxBytes = FileUtils.readFileToByteArray(bodyContents);
    		return uploadStreamAsync(requestUrl, docxBytes, contentType);
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("ContentType",  contentType);
        
        if (authResult == null) {
        	authResult = getBearerToken().get();
        }
        headers.put("Authorization",  "Bearer " + authResult.accessToken() );
        log.debug(authResult.accessToken());
        // 'Accept':'application/json;odata.metadata=minimal'}
        headers.put("Accept",  "application/json;odata.metadata=minimal");
        
        log.debug(requestUrl);
      OAuthRequest.ResponseConverter uploadResponseConverter = new UploadResponseConverter(); 
      OAuthAsyncRequestCallback callback = new UploadOAuthAsyncRequestCallback(); 
        return client.executeAsync("ScribeJava", headers, Verb.PUT, requestUrl, bodyContents, 
        		callback,  uploadResponseConverter);
        		
    }
    
    class UploadOAuthAsyncRequestCallback implements OAuthAsyncRequestCallback<Boolean> /* must match ResponseConverter */ {

		public void onCompleted(Boolean response) {
			log.debug("UploadOAuthAsyncRequestCallback: " + response);  // fileid; its the response converter output
			
		}

		public void onThrowable(Throwable t) {
			throw new RuntimeException(t);
			
		}
    	
    }
    
	class UploadResponseConverter implements OAuthRequest.ResponseConverter<Boolean> {
	
		public Boolean convert(Response response) throws IOException {
	        log.info("received response for upload");
	        String body=null; 
	        if (log.isDebugEnabled()) {
	            log.debug("response status code: " + response.getCode());
	            body = response.getBody();
	            log.debug("response body: " + body);
	        }
	        if (!response.isSuccessful() ) {
	        	log.warn(response.getBody());
		        response.close();
	        	return null;
	        }
	        response.close();
	        return true; 
	    }
	
	}
	
    public Future<byte[]> downloadConvertedFileAsync(String requestUrl) throws InterruptedException, ExecutionException {
    	
    	HttpClient client = getHttpClient().get();
                        
        Map<String, String> headers = new HashMap<String, String>();
//        headers.put("ContentType",  contentType);
        headers.put("Authorization",  "Bearer " + authResult.accessToken() );
        // 'Accept':'application/json;odata.metadata=minimal'}
//        headers.put("Accept",  "application/json;odata.metadata=minimal");
        
		log.debug(requestUrl);
		byte[] nullBytes = null;
		return client.executeAsync("ScribeJava", headers, Verb.GET, requestUrl, nullBytes,
        		new DownloadOAuthAsyncRequestCallback(), new DownloadResponseConverter() );
        		
    }
    
    class DownloadOAuthAsyncRequestCallback implements OAuthAsyncRequestCallback<byte[]> {

		public void onCompleted(byte[] response) {
			log.debug("callback oncompleted: downloaded " + response.length + " bytes");
			
			
		}

		public void onThrowable(Throwable t) {
			throw new RuntimeException(t);
			
		}
    	
    }
    

	class DownloadResponseConverter implements OAuthRequest.ResponseConverter<byte[]> {
		
		public byte[] convert(Response response) throws IOException {
	        log.info("received response for upload");
	        byte[] bytes = IOUtils.toByteArray(response.getStream());
	        response.close();
	        return bytes;
	    }
	
	}

    public Future<Boolean> deleteFileAsync(String requestUrl) throws InterruptedException, ExecutionException {
    	
    	HttpClient client = getHttpClient().get();
        
        Map<String, String> headers = new HashMap<String, String>();
//        headers.put("ContentType",  contentType);
        headers.put("Authorization",  "Bearer " + authResult.accessToken() );
        // 'Accept':'application/json;odata.metadata=minimal'}
//        headers.put("Accept",  "application/json;odata.metadata=minimal");
        
		log.debug(requestUrl);
		byte[] nullBytes = new byte[0];
		return client.executeAsync("ScribeJava", headers, Verb.DELETE, requestUrl, nullBytes, null,
				new DeleteResponseConverter());
        		
    }

	class DeleteResponseConverter implements OAuthRequest.ResponseConverter<Boolean> {
		
		public Boolean convert(Response response) throws IOException {
	        log.info("received response for delete");
	        String body=null; 
	        if (log.isDebugEnabled()) {
	            log.debug("response status code: " + response.getCode());
	            body = response.getBody();
	            log.debug("response body: " + body);
	        }
	        
	        return (response.isSuccessful());
	    }
	
	}
    
}
