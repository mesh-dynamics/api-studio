package io.md.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.utils.Utils;

//Dummy implementation only status works.
// TODO: Complete this later.
public class ThriftResponsePayload implements ResponsePayload {

	public int status;

	public ThriftResponsePayload( int status) {
		this.status = status;
	}

	@Override
	public String getStatusCode() {
		return String.valueOf(status);
	}

	@Override
	public Payload applyTransform(Payload rhs, List<ReqRespUpdateOperation> operationList) {
		return null;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public boolean isDataObjEmpty() {
		return false;
	}

	@Override
	public DataObj getVal(String path) {
		return null;
	}

	@Override
	public String getValAsString(String path) throws PathNotFoundException {
		return null;
	}

	@Override
	public String serializeDataObj() throws DataObjProcessingException {
		return null;
	}

	@Override
	public void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals) {

	}

	@Override
	public MatchType compare(DataObj rhs, CompareTemplate template) {
		return null;
	}

	@Override
	public boolean wrapAsString(String path, String mimetype) {
		return false;
	}

	@Override
	public boolean wrapAsByteArray(String path, String mimetype) {
		return false;
	}

	@Override
	public Optional<Object> encryptField(String path, EncryptionAlgorithm encrypter) {
		return Optional.empty();
	}

	@Override
	public Optional<String> decryptField(String path, EncryptionAlgorithm decrypter) {
		return Optional.empty();
	}

	@Override
	public void getPathRules(CompareTemplate template, Map<String, TemplateEntry> vals) {

	}

	@Override
	public DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList) {
		return null;
	}

	@Override
	public <T> Optional<T> getValAsObject(String path, Class<T> className) {
		return Optional.empty();
	}

	@Override
	public byte[] getValAsByteArray(String path) throws PathNotFoundException {
		return new byte[0];
	}

	@Override
	public boolean put(String path, DataObj value) throws PathNotFoundException {
		return false;
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws NotImplementedException, RawPayloadEmptyException {
		return new byte[0];
	}

	@Override
	public String rawPayloadAsString()
		throws NotImplementedException, RawPayloadEmptyException, RawPayloadProcessingException {
		return null;
	}

	@Override
	public String rawPayloadAsString(boolean wrapForDisplay)
		throws NotImplementedException, RawPayloadEmptyException, RawPayloadProcessingException {
		return null;
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return false;
	}

	@Override
	public  void replaceContent(List<String> pathsToKeep, String path) {
		return;
	}

	@Override
	public long size() {
		return 0;
	}

	@Override
	public void updatePayloadBody() throws PathNotFoundException {
		return;
	}
}
