package org.plutext.msgraph.convert.scribe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.plutext.msgraph.convert.AbstractOpenXmlToPDF;
import org.plutext.msgraph.convert.AuthConfig;
import org.plutext.msgraph.convert.ConversionException;

import com.github.scribejava.core.httpclient.HttpClient;

public class XlsxToPdfConverter extends org.plutext.msgraph.convert.scribe.PdfConverter implements org.plutext.msgraph.convert.XlsxToPdfConverter {

	public XlsxToPdfConverter(AuthConfig authConfig) throws ConversionException {
		super(authConfig);
	}

	public XlsxToPdfConverter(AuthConfig authConfig, HttpClient httpClient) throws ConversionException {
		super(authConfig, httpClient);
	}
		
	@Override
	public byte[] convert(byte[] xlsx) throws ConversionException {
		return convertMime(xlsx, XLSX_MIME_TYPE);
	}

	@Override
	public byte[] convert(byte[] bytes, String ext) throws ConversionException {
		return convertMime(bytes, XLSX_MIME_TYPE);
		
	}
	
 
	public byte[] convert(File inFile) throws ConversionException, IOException {

		return convertMime(inFile, XLSX_MIME_TYPE);
	}

	@Override
	public byte[] convert(InputStream xlsx) throws ConversionException, IOException {
		return convert( IOUtils.toByteArray(xlsx) );
	}

	@Override
	public byte[] convert(InputStream is, String ext) throws ConversionException, IOException {
		return convert( IOUtils.toByteArray(is)  );
	}
	
}
