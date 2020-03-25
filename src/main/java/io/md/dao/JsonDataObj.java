package io.md.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.MenuElement;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.md.constants.Constants;
import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.cryptography.EncryptionAlgorithm;
import io.md.utils.JsonTransformer;

public class JsonDataObj implements DataObj {

	private static final Logger LOGGER = LogManager.getLogger(JsonDataObj.class);

	public static JsonDataObj createEmptyObject(ObjectMapper jsonMapper) {
		return new JsonDataObj(MissingNode.getInstance(), jsonMapper);
	}

	JsonDataObj(String json, ObjectMapper jsonMapper) {
		this(jsonStrToTreeNode(json, jsonMapper), jsonMapper);
	}

	JsonDataObj(byte[] jsonByteArray, ObjectMapper jsonMapper) {
		this(jsonBinaryToTreeNode(jsonByteArray, jsonMapper), jsonMapper);
	}

	JsonDataObj(Payload obj, ObjectMapper jsonMapper) {
		// The serializer always serializes a Paylaod as an array(Because of our custom serializer)
		// The first element is typeInfo and the second element is the payload object itself
		// we just need the payload object parsed into a json tree (ignoring type info)
		ArrayNode payloadTreeMap   = jsonMapper.valueToTree(obj);
		this.objRoot = payloadTreeMap.get(1);
		this.jsonMapper = jsonMapper;
	}

	public JsonDataObj(JsonNode root, ObjectMapper jsonMapper) {
		this.objRoot = root;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public boolean isLeaf() {
		return objRoot.isValueNode() || objRoot.isMissingNode();
	}

	@Override
	public boolean isDataObjEmpty() {
		return objRoot.isMissingNode();
	}

	@Override
	public DataObj getVal(String path) {
		return new JsonDataObj(getNode(path), jsonMapper);
	}

	@Override
	public String getValAsString(String path) throws PathNotFoundException {
		JsonNode node = getNode(path);
		if (!node.isMissingNode()) {
			return nodeToString(getNode(path));
		} else {
			throw new PathNotFoundException(path);
		}
	}

	@Override
	public byte[] getValAsByteArray(String path) throws PathNotFoundException{
		JsonNode node = getNode(path);
		if (!node.isMissingNode()) {
			return nodeToByteArray(node);
		} else {
			throw new PathNotFoundException(path);
		}
	}

	private byte[] nodeToByteArray(JsonNode node) {
		try {
			if (node.isBinary()) {
				return ((BinaryNode) node).binaryValue();
			}
			if (node.isTextual()) {
				return node.asText().getBytes();
			}
			return jsonMapper.writeValueAsString(node).getBytes();
		} catch (JsonProcessingException e) {
			LOGGER.error("Error in converting json node to byte array: " + node.toString());
			//TODO maybe make the return type of this function as Optional
			return node.toString().getBytes();
		}
	}

	@Override
	public String serializeDataObj() throws DataObjProcessingException {
		try {
			return jsonMapper.writeValueAsString(objRoot);
		} catch (JsonProcessingException e) {
			throw  new DataObjProcessingException(e);
		}
	}

	@Override
	public String toString() {
		try {
			return jsonMapper.writeValueAsString(objRoot);
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(Map.of("message", "Not able to serialize json",
				"value", objRoot.toString())));
			// TODO .. isn't this prone to errors ?
			return objRoot.toString();
		}
	}

	@Override
	public void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals) {
		// Using json pointer to handle proper escaping in case keys have special characters
		JsonPointer path = JsonPointer.compile("");
		processNode(objRoot, filter, vals, path);
	}

	@Override
	public void getPathRules(CompareTemplate template, Map<String, TemplateEntry> vals) {
		// Using json pointer to handle proper escaping in case keys have special characters
		JsonPointer path = JsonPointer.compile("");
		getPathRules(objRoot, template, vals, path);
	}

	@Override
	public Comparator.MatchType compare(DataObj rhs, CompareTemplate template) {
		return null;
	}

	/**
	 * Unwrap the string at path into a json object. The type for interpreting the
	 * string is given by mimetype
	 * @param path
	 * @param mimetype
	 */
	public boolean unwrapAsJson(String path, String mimetype) {
		return unwrapAsJson(objRoot, path, mimetype);
	}

	private boolean unwrapAsJson(JsonNode root, String path, String mimetype) {
		JsonPointer pathPtr = JsonPointer.compile(path);
		JsonNode valParent = root.at(pathPtr.head());
		if (valParent != null &&  valParent.isObject()) {
			ObjectNode valParentObj = (ObjectNode) valParent;
			String fieldName = pathPtr.last().getMatchingProperty();
			JsonNode val = valParentObj.get(fieldName);
			if (val != null && (val.isTextual() || val.isBinary())) {
				// parse it as per mime type
				// currently handling only json type
				if (mimetype.startsWith(MediaType.APPLICATION_JSON)) {
					try {
						// This will work irrespective if the val is a TextNode (if unwrapped in
						// collector or CubeServer after serialization by agent)
						// or BinaryNode (if unwrapped in agent before serialization)
						JsonNode parsedVal = jsonMapper.readTree(val.binaryValue());
						valParentObj.set(fieldName, parsedVal);
						return true;
					} catch (IOException e) {
						try {
							JsonNode parsedVal = jsonMapper.readTree(val.textValue());
							valParentObj.set(fieldName, parsedVal);
							return true;
						} catch (IOException ex) {
							LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
								"Exception in parsing json string", Constants.PATH_FIELD, path
								, "value" , val.toString())) , e);
						}
					}
				} else if (val.isTextual()) {
					try {
						// if the value is not object, it is always a byte array ,( for now )
						// since we are using generic objectMapper.readTree(jsonParser) in serializer,
						// all leaf values in quotes will be read as TextNode (even though they might a Base64
						// encoded byte array)
						// so we are just converting a TextNode to a BinaryNode here (to avoid confusion)
						if (mimetype.startsWith(MediaType.APPLICATION_FORM_URLENCODED)) {
							JsonNode parserVal = new TextNode(new String(val.binaryValue(),
								StandardCharsets.UTF_8));
							valParentObj.set(fieldName, parserVal);
						} else {
							JsonNode parsedVal = new BinaryNode(val.binaryValue());
							valParentObj.set(fieldName, parsedVal);
						}
						return true;
					} catch (IOException e) {
						LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
							"Exception in parsing json string", Constants.PATH_FIELD, path
							, "value" , val.toString())) , e);
					}
				}
			}
		}
		return false;
	}

	/**
	 * wrap the value at path into a string
	 * @param path
	 * @param mimetype
	 */
	@Override
	public boolean wrapAsString(String path, String mimetype) {
		return wrap(objRoot, path, mimetype, false);
	}

	/**
	 * wrap the value at path into a byte array
	 * @param path
	 * @param mimetype
	 */
	@Override
	public boolean wrapAsByteArray(String path, String mimetype) {
		return wrap(objRoot, path, mimetype, true);
	}

	private boolean wrap(JsonNode root, String path, String mimetype, boolean asByteArray) {
		JsonPointer pathPtr = JsonPointer.compile(path);
		JsonNode valParent = root.at(pathPtr.head());
		if (valParent != null &&  valParent.isObject()) {
			ObjectNode valParentObj = (ObjectNode) valParent;
			String fieldName = pathPtr.last().getMatchingProperty();
			JsonNode val = valParentObj.get(fieldName);
			if (val != null && !val.isValueNode()) {
				// convert to string
				// currently handling only json type
				if (mimetype.startsWith(MediaType.APPLICATION_JSON)) {
					if (asByteArray) {
						valParentObj.set(fieldName, new BinaryNode(val.toString().getBytes()));
					} else {
						valParentObj.set(fieldName, new TextNode(val.toString()));
					}
					return true;
				}
			} else if (val != null && val.isBinary() && !asByteArray) {
				if (mimetype.equals(MediaType.APPLICATION_XML) || mimetype.equals(MediaType.TEXT_HTML)
					|| mimetype.equals(MediaType.TEXT_PLAIN)) {
					try {
						valParentObj.set(fieldName,
							new TextNode(new String(val.binaryValue(), StandardCharsets.UTF_8)));
						return true;
					} catch (IOException e) {
						LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Error while"
							+ " wrapping byte array as UTF-8 string")), e);

					}
				}
			}
		}
		return false;
	}

	@Override
	public Optional<Object> encryptField(String path, EncryptionAlgorithm encrypter) {
		return encryptField(objRoot, path, encrypter);
	}

	private Optional<Object> encryptField(JsonNode root, String path,
		EncryptionAlgorithm encrypter) {
		JsonPointer pathPtr = JsonPointer.compile(path);
		JsonNode valParent = root.at(pathPtr.head());
		if (valParent != null && valParent.isObject()) {
			ObjectNode valParentObj = (ObjectNode) valParent;
			String fieldName = pathPtr.last().getMatchingProperty();
			JsonNode val = valParentObj.get(fieldName);
			if (val != null && val.isValueNode()) {
				// TODO handle types other than binary and text node
				// corresponding changes in the encrypter module might be required
				if (val.isBinary()) {
					try {
						return encrypter.encrypt(val.binaryValue()).map(newVal -> {
							valParentObj.set(fieldName, new BinaryNode(newVal));
							return newVal;
						});
					} catch (IOException e) {
						LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
							, "Error while retrieving binary node value")), e);
					}
				} else {
					return encrypter.encrypt(val.toString()).map(newVal -> {
						valParentObj.set(fieldName, new TextNode(newVal));
						return newVal;
					});
				}
			}
		}
		return Optional.empty();
	}


	@Override
	public Optional<String> decryptField(String path, EncryptionAlgorithm encrypter) {
		return decryptField(objRoot, path, encrypter);
	}

	@Override
	public <T> Optional<T> getValAsObject(String path, Class<T> className) {
		JsonPointer pathPtr = JsonPointer.compile(path);
		JsonNode valueNode = objRoot.at(pathPtr);
		if (valueNode != null && ! valueNode.isMissingNode()) {
			try {
				return Optional.of(jsonMapper.treeToValue(valueNode, className));
			} catch (Exception e) {
				LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Unable to convert path to desired object" , Constants.PATH_FIELD,
					path , "className" , className.getName())));
			}
		} else {
			LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "path does not exist",
				Constants.PATH_FIELD, path)));
		}
		return Optional.empty();
	}

	private Optional<String> decryptField(JsonNode root, String path, EncryptionAlgorithm decrypter) {
		JsonPointer pathPtr = JsonPointer.compile(path);
		JsonNode valParent = root.at(pathPtr.head());
		if (valParent != null &&  valParent.isObject()) {
			ObjectNode valParentObj = (ObjectNode) valParent;
			String fieldName = pathPtr.last().getMatchingProperty();
			JsonNode val = valParentObj.get(fieldName);
			if (val != null && val.isValueNode()) {
				String strToDecrypt = val.isTextual() ? val.textValue() : val.toString();
				return decrypter.decrypt(strToDecrypt).map(newVal -> {
					valParentObj.set(fieldName, new TextNode(newVal));
					return newVal;
				});
			}
		}
		return Optional.empty();
	}

	private void getPathRules(JsonNode node, CompareTemplate template, Map<String, TemplateEntry> vals, JsonPointer path) {
		vals.put(path.toString(), template.getRule(path.toString()));
		if (node.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> child = fields.next();
				getPathRules(child.getValue(), template, vals, path.append(JsonPointer.compile("/" + child.getKey())));
			}
		} else if (node.isArray()) {
			int idx = 0;
			for (JsonNode child : node) {
				getPathRules(child, template, vals, path.append(JsonPointer.compile("/" + idx)));
				idx++;
			}
		}
	}

	@Override
	public DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList) {
		if (!(rhs instanceof JsonDataObj)) {
			LOGGER.error(new ObjectMessage(Map.of(
				Constants.MESSAGE, "Rhs not Json obj type. Ignoring the transformation",
				Constants.DATA, rhs.toString()
			)));
			return this;
		}
		JsonTransformer jsonTransformer = new JsonTransformer(jsonMapper);
		JsonNode transformedRoot = jsonTransformer.transform(this.objRoot, ((JsonDataObj)rhs).getRoot(),
			operationList);

		return new JsonDataObj(transformedRoot, jsonMapper);
	}


	private void processNode(JsonNode node, Function<String, Boolean> filter,
		Collection<String> vals, JsonPointer path) {
		if (node.isValueNode()) {
			if (filter.apply(path.toString())) {
				vals.add(path + "=" + node.asText());
			}
		} else if (node.isObject()) {
			Iterator<Entry<String, JsonNode>> fields = node.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> child = fields.next();
				processNode(child.getValue(), filter, vals, path.append(JsonPointer
					.compile("/" + child.getKey())));
			}
		} else if (node.isArray()) {
			int idx = 0;
			for (JsonNode child : node) {
				processNode(child, filter, vals, path.append(JsonPointer.compile("/" + idx)));
				idx++;
			}
		}
	}

	private static JsonNode jsonStrToTreeNode(String json, ObjectMapper jsonMapper) {
		try {
			return jsonMapper.readTree(json);
		} catch (IOException e) {
			LOGGER.error("Not able to parse json: " + json);
			return MissingNode.getInstance();
		}
	}

	private static JsonNode jsonBinaryToTreeNode(byte[] json, ObjectMapper jsonMapper) {
		try {
			return jsonMapper.readTree(json);
		} catch (IOException e) {
			LOGGER.error("Not able to parse json: " + json);
			return MissingNode.getInstance();
		}
	}

	protected JsonNode getNode(String path) {
		return objRoot.at(path);
	}

	private String nodeToString(JsonNode node) {
		try {
			if (node.isTextual()) {
				return node.asText();
			} else if (node.isBinary()) {
				return new String(node.binaryValue() , StandardCharsets.UTF_8);
			}
			return jsonMapper.writeValueAsString(node);
		} catch (IOException e) {
			LOGGER.error("Error in converting json node to string: " + node.toString());
			return node.toString();
		}
	}

	@JsonIgnore
	protected final JsonNode objRoot;
	@JsonIgnore
	protected final ObjectMapper jsonMapper;

	public JsonNode getRoot() {
		return objRoot;
	}
}

