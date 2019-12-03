package com.cube.dao;

import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;

import io.cube.agent.CommonUtils;

import com.cube.core.Comparator.MatchType;
import com.cube.core.CompareTemplate;
import com.cube.dao.Event.RawPayload;
import com.cube.golden.ReqRespUpdateOperation;
import com.cube.utils.Constants;
import com.cube.ws.Config;

public class ThriftDataObject implements DataObj {


	private JsonDataObj jsonDataObj;
	public final String traceId;

	public ThriftDataObject(byte[] payloadBin, Config config, Map<String, Object> params) {
		try {
			TDeserializer tDeserializer = new TDeserializer();
			ClassLoader loader = (URLClassLoader) params.get(Constants.CLASS_LOADER);
			Class<?> clazz = loader
				.loadClass((String) params.get(Constants.THRIFT_CLASS_NAME));
			Constructor<?> constructor = clazz.getConstructor();
			Object obj1 = constructor.newInstance();
			tDeserializer.deserialize((TBase)obj1, payloadBin);
			String jsonSerialized = config.gson.toJson(obj1);
			traceId = CommonUtils.traceIdFromThriftSpan((TBase)obj1);
			jsonDataObj = new JsonDataObj(jsonSerialized, config.jsonMapper);
		} catch (Exception e) {
			throw new DataObjCreationException(e);
		}
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public DataObj getVal(String path) {
		return jsonDataObj.getVal(path);
	}

	@Override
	public String getValAsString(String path) throws PathNotFoundException {
		return jsonDataObj.getValAsString(path);
	}

	@Override
	public String serialize() {
		return null;
	}

	@Override
	public void collectKeyVals(Function<String, Boolean> filter, Collection<String> vals) {
		jsonDataObj.collectKeyVals(filter, vals);
	}

	@Override
	public MatchType compare(DataObj rhs, CompareTemplate template) {
		return jsonDataObj.compare(rhs, template);
	}

	@Override
	public DataObj applyTransform(DataObj rhs, List<ReqRespUpdateOperation> operationList) {
		return jsonDataObj.applyTransform(rhs, operationList);
	}

	@Override
	public RawPayload toRawPayload() {
		// TODO maybe need extra processing to convert back to binary
		return jsonDataObj.toRawPayload();
	}

	@Override
	public boolean wrapAsString(String path, String mimetype) {
		return false;
	}
}
