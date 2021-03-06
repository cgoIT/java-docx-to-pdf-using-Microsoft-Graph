package org.plutext.msgraph.convert.msal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.plutext.msgraph.convert.AuthConfig;
import org.plutext.msgraph.convert.ConversionException;

public class DocxToPdfConverter extends PdfConverter implements org.plutext.msgraph.convert.DocxToPdfConverter {

	public DocxToPdfConverter(AuthConfig authConfig) {
		super(authConfig);
	}

	private static final String DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	
	
	@Override
	public byte[] convert(byte[] docx) throws ConversionException {
		return convertMime(docx, DOCX_MIME_TYPE);
	}

	@Override
	public byte[] convert(byte[] bytes, String ext) throws ConversionException {
		return convertMime(bytes, DOCX_MIME_TYPE);
		
	}
	
 
	public byte[] convert(File inFile) throws ConversionException, IOException {

		return convertMime(inFile, DOCX_MIME_TYPE);
	}

	@Override
	public byte[] convert(InputStream docx) throws ConversionException, IOException {
		return convert( IOUtils.toByteArray(docx) );
	}

	@Override
	public byte[] convert(InputStream is, String ext) throws ConversionException, IOException {
		return convert( IOUtils.toByteArray(is)  );
	}
	
}
