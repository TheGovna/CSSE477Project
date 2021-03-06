/*
 * HttpResponseFactory.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 */

package protocol;

import java.io.File;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * This is a factory to produce various kind of HTTP responses.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class HttpResponseFactory {

	private static HashMap<String, String[]> requestStrings = new HashMap<String, String[]>();
	private static HashMap<String, String[]> responseCodes = new HashMap<String, String[]>();

	public static HttpResponse createRequest(String type, String connection) {
		if (requestStrings.isEmpty()) {
			String[] codes = { Protocol.BAD_REQUEST_CODE + "", Protocol.BAD_REQUEST_TEXT };
			requestStrings.put(Protocol.BAD_REQUEST_CODE + "", codes);
			String[] codes2 = { Protocol.NOT_FOUND_CODE + "", Protocol.NOT_FOUND_TEXT };
			requestStrings.put(Protocol.NOT_FOUND_CODE + "", codes2);
			String[] codes3 = { Protocol.RESOURCE_NOT_MODIFIED_CODE + "", Protocol.RESOURCE_NOT_MODIFIED_TEXT };
			requestStrings.put(Protocol.RESOURCE_NOT_MODIFIED_CODE + "", codes3);
			String[] codes4 = { Protocol.NOT_SUPPORTED_CODE + "", Protocol.NOT_SUPPORTED_TEXT };
			requestStrings.put(Protocol.NOT_SUPPORTED_CODE + "", codes4);
		}
		String[] info = requestStrings.get(type);
		HttpResponse response = new HttpResponse(Protocol.VERSION, Integer.valueOf(info[0]), info[1],
				new HashMap<String, String>(), null);

		// Lets fill up header fields with more information
		fillGeneralHeader(response, connection);

		return response;

	}

	/**
	 * Convenience method for adding general header to the supplied response
	 * object.
	 * 
	 * @param response
	 *            The {@link HttpResponse} object whose header needs to be
	 *            filled in.
	 * @param connection
	 *            Supported values are {@link Protocol#OPEN} and
	 *            {@link Protocol#CLOSE}.
	 */
	private static void fillGeneralHeader(HttpResponse response, String connection) {
		// Lets add Connection header
		response.put(Protocol.CONNECTION, connection);

		// Lets add current date
		Date date = Calendar.getInstance().getTime();
		response.put(Protocol.DATE, date.toString());

		// Lets add server info
		response.put(Protocol.Server, Protocol.getServerInfo());

		// Lets add extra header with provider info
		response.put(Protocol.PROVIDER, Protocol.AUTHOR);
	}

	public static HttpResponse createRequestWithFile(File file, String connection, String requestType) {
		if (responseCodes.isEmpty()) {
			String[] codes = { "" + Protocol.OK_CODE, Protocol.OK_TEXT };
			responseCodes.put(Protocol.GET, codes);

			String[] codes2 = { "" + Protocol.POST_CODE, Protocol.POST_TEXT };
			responseCodes.put(Protocol.POST, codes2);

			String[] codes3 = { "" + Protocol.PUT_CODE, Protocol.PUT_TEXT };
			responseCodes.put(Protocol.PUT, codes3);

			String[] codes4 = { "" + Protocol.DELETE_CODE, Protocol.DELETE_TEXT };
			responseCodes.put(Protocol.DELETE, codes4);
		}

		String[] codes = responseCodes.get(requestType);

		HttpResponse response = new HttpResponse(Protocol.VERSION, Integer.parseInt(codes[0]), codes[1],
				new HashMap<String, String>(), file);

		// Lets fill up header fields with more information
		fillGeneralHeader(response, connection);

		if (file != null) {
			// Lets add last modified date for the file
			long timeSinceEpoch = file.lastModified();
			Date modifiedTime = new Date(timeSinceEpoch);
			response.put(Protocol.LAST_MODIFIED, modifiedTime.toString());

			// Lets get content length in bytes
			long length = file.length();
			response.put(Protocol.CONTENT_LENGTH, length + "");

			// Lets get MIME type for the file
			FileNameMap fileNameMap = URLConnection.getFileNameMap();
			String mime = fileNameMap.getContentTypeFor(file.getName());
			// The fileNameMap cannot find mime type for all of the documents,
			// e.g.
			// doc, odt, etc.
			// So we will not add this field if we cannot figure out what a mime
			// type is for the file.
			// Let browser do this job by itself.
			if (mime != null) {
				response.put(Protocol.CONTENT_TYPE, mime);
			}
		}

		return response;
	}
}
