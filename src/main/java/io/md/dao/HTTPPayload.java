package io.md.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.core.WrapUnwrapContext;
import io.md.logger.LogMgr;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 11/08/20
 * Common parts of HTTPRequest and HTTPResponse
 */
public class HTTPPayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LogMgr.getLogger(HTTPPayload.class);
	static final String BODY = "body";
	static final String PAYLOADSTATEPATH = "/payloadState";

	private static Pattern pattern = Pattern.compile("/hdrs/([^/\\n$]+)");

	public enum HTTPPayloadState {
		WrappedEncoded,
	    WrappedDecoded,
	    UnwrappedDecoded
	}

	@JsonDeserialize(as= MultivaluedHashMap.class)
	@JsonProperty("hdrs")
	protected MultivaluedMap<String, String> hdrs;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	@JsonProperty("body")
	protected byte[] body;

	// in case of bodies that need to be interpreted as string, there is no way to distinguish
	// between the case where the body is a base64 encoded body or decoded body. So we keep this
	// additional state to keep track of whether the body has already been unwrapped
	public HTTPPayloadState payloadState = HTTPPayloadState.WrappedEncoded;

	protected HTTPPayload(
		@JsonProperty("hdrs") MultivaluedMap<String, String> hdrs,
		@JsonProperty("body") byte[] body) {
		if (hdrs != null) this.hdrs = Utils.setLowerCaseKeys(hdrs);
		this.body = body;
		this.payloadState = HTTPPayloadState.WrappedEncoded;
	}

	protected HTTPPayload(JsonNode deserializedJsonTree) {
		super(deserializedJsonTree);
		this.hdrs =  this.dataObj.getValAsObject("/".concat("hdrs"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
	}


	@Override
	public byte[] rawPayloadAsByteArray() throws NotImplementedException {
		throw new NotImplementedException("Payload can be accessed as a json string");
	}

	@Override
	public String rawPayloadAsString()
		throws RawPayloadProcessingException {
		parseIfRequired();
		return this.rawPayloadAsString(false);
	}

	@Override
	public long size() {
		return this.body != null ? this.body.length : 0;
	}

	public String rawPayloadAsString(boolean wrapForDisplay) throws
		NotImplementedException, RawPayloadProcessingException {
		try {
			if (this.dataObj.isDataObjEmpty()) {
				return mapper.writeValueAsString(this);
			} else {
				String mimeType = Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN);
				if (wrapForDisplay && !Utils.startsWithIgnoreCase(mimeType,
					MediaType.MULTIPART_FORM_DATA)) {
					wrapBody();
				}
				return dataObj.serializeDataObj();
			}
		} catch (Exception e) {
			throw  new RawPayloadProcessingException(e);
		}
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return false;
	}

	@Override
	public void postParse() {
		this.payloadState = HTTPPayloadState.WrappedEncoded;
		//TODO this is added only for the case when a GRPCRequestPayload is constructed in mock.
		// We would need to study its effects at other places.
		if (this.dataObj == null) {
			this.dataObj = new JsonDataObj(this, CubeObjectMapperProvider.getInstance());
		}
		if (!this.dataObj.isDataObjEmpty()) {
			this.dataObj.getValAsObject(PAYLOADSTATEPATH, HTTPPayloadState.class)
				.ifPresent(v -> payloadState=v);
			unWrapBody();
		}
	}

	@JsonIgnore
	public byte[] getBody() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			try {
				wrapBody();
				return this.dataObj.getValAsByteArray("/".concat(BODY));
			} catch (PathNotFoundException e) {
				//do nothing
			}
		} else if (this.body != null && !(this.body.length == 0)) {
			return body;
		}
		return new byte[]{};
	}


	@JsonIgnore
	public MultivaluedMap<String, String> getHdrs() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			return this.dataObj.getValAsObject("/".concat("hdrs"),
				MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		}
		return hdrs;
	}

	@Override
	public void updatePayloadBody() throws PathNotFoundException  {
		if (this.dataObj.isDataObjEmpty()) {
			return;
		} else {
			this.body = this.dataObj.getValAsByteArray("/".concat(BODY));
		}
	}

	public void wrapBody() {
		if (payloadState == HTTPPayloadState.UnwrappedDecoded) {
			this.dataObj.wrapAsString("/".concat(HTTPRequestPayload.BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN) , getWrapUnwrapContext());
			setPayloadState(HTTPPayloadState.WrappedDecoded);
		}
	}

	public void wrapBodyAndEncode() {
		if (payloadState == HTTPPayloadState.UnwrappedDecoded) {
			if (this.dataObj.wrapAsEncoded("/".concat(HTTPRequestPayload.BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN), getWrapUnwrapContext()));
			setPayloadState(HTTPPayloadState.WrappedEncoded);
		}
	}


	protected Optional<WrapUnwrapContext> getWrapUnwrapContext() {
		return Optional.empty();
	}

	public void unWrapBody() {
		// Currently unwrapAsJson does both decoding and unwrapping.
		// Will cleanup and separate the functions later
		if (payloadState == HTTPPayloadState.WrappedDecoded || payloadState == HTTPPayloadState.WrappedEncoded) {
			this.dataObj.unwrapAsJson("/".concat(HTTPRequestPayload.BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN), Optional.empty());
			setPayloadState(HTTPPayloadState.UnwrappedDecoded);
		}
	}

	/*
	 * this will update state both in this object and the parsed dataObj
	 */
	void setPayloadState(HTTPPayloadState payloadState) {
		this.payloadState = payloadState;
		try {
			dataObj.put(PAYLOADSTATEPATH, new JsonDataObj(payloadState, mapper));
		} catch (PathNotFoundException e) {
			LOGGER.error("Payload not an object, should not happen");
		}
	}

	private List<String> hdrKeyCaseInsensitivePath(String path) {
		List<String> result = new ArrayList<>( );
		Matcher matcher = pattern.matcher(path);
		if (matcher.find()) {
			String hdrKeyValue = matcher.group(1);
			String[] parts = hdrKeyValue.split("[-]");
			String beforeMatch = path.substring(0, matcher.start(1));
			String afterMatch = path.substring(matcher.end(1));
			String allLower = Arrays.stream(parts).sequential()
				.map(String::toLowerCase).collect(Collectors.joining("-"));
			String allCaps = Arrays.stream(parts).sequential()
				.map(String::toUpperCase).collect(Collectors.joining("-"));
			String firstLetterCaps = Arrays.stream(parts).sequential().map(String::toLowerCase)
				.map(x -> x.substring(0,1).toUpperCase().concat(x.substring(1)))
				.collect(Collectors.joining("-"));
			result.add(beforeMatch.concat(allLower).concat(afterMatch));
			result.add(beforeMatch.concat(allCaps).concat(afterMatch));
			result.add(beforeMatch.concat(firstLetterCaps).concat(afterMatch));
		}
		return result;
	}


	@Override
	public DataObj getVal(String path) {
		DataObj originalResult = super.getVal(path);
		if (originalResult == null || ((JsonDataObj) originalResult).objRoot.isMissingNode()) {
			originalResult = hdrKeyCaseInsensitivePath(path).stream().map(super::getVal)
				.filter(dataObj -> dataObj != null && !((JsonDataObj) dataObj)
					.objRoot.isMissingNode()).findAny()
				.orElse(new JsonDataObj(MissingNode.getInstance(),
					dataObj.jsonMapper));
		}
		return originalResult;
	}

	@Override
	public String getValAsString(String path) throws PathNotFoundException {
		try {
			return super.getValAsString(path);
		} catch (PathNotFoundException e) {
			List<String> caseVariants = hdrKeyCaseInsensitivePath(path);
			for (String caseVariant : caseVariants) {
				try {
					return super.getValAsString(caseVariant);
				} catch (PathNotFoundException e1) {
					// do nothing
				}
			}
		}
		throw new PathNotFoundException();
	}


}
