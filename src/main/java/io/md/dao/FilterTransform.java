package io.md.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.md.dao.Event.EventType;
import io.md.logger.LogMgr;
import io.md.utils.CubeObjectMapperProvider;

public class FilterTransform {

	private static final Logger LOGGER = LogMgr.getLogger(FilterTransform.class);

	private static String encodedMsg(DataObj dObj){
		int checksum = dObj.getChecksum(Optional.empty());
		//Adding extra prefix for recognition
		return String.format("MD-%d", checksum /*new BigInteger(1, checksum.getBytes())*/);
	}


	@JsonTypeInfo(use = Id.NAME , property = "type" )
	@JsonSubTypes({@Type(value = ServiceApiPathFilter.class , name = ServiceApiPathFilter.TYPE)})
	public interface  Filter {

		/*
		   return true means , transform operation needs to be applied on this event payload
		 */
		boolean filter(Event event);

		String getService();

		String getApiPath();


	}

	@JsonTypeInfo(use = Id.NAME , property = "type" )
	@JsonSubTypes({@Type(value = JsonPathTransformer.class , name = JsonPathTransformer.TYPE)})
	public interface  Transform {

		public void transformPayload(Payload payload);
	}

	public enum Operation{
		TRUNCATE    //replace with checksum/hash
	}

	public static class JsonPathTransformer implements Transform {

		public static final String TYPE = "JsonPathTransformer";

		@JsonProperty("jsonPaths")
		public final List<String> jsonPaths;
		@JsonProperty("operation")
		public final Operation operation;

		@JsonIgnore
		private String name;

		@Override
		public String toString(){
			if(name!=null) return name;
			name = "JsonPathTransformer "+operation + " jsonPaths "+ jsonPaths.stream().collect(Collectors.joining(","));
			return name;
		}


		@Override
		public void transformPayload(Payload payload) {
			switch (operation){
				case TRUNCATE:
					truncateWithChecksum(payload);
					return;
				default:
					throw new UnsupportedOperationException();
			}
		}

		private void truncateWithChecksum(Payload payload){
			Map<String , String> pathChecksumMap = new HashMap<>();

			for(String path : jsonPaths){
				DataObj dObj = payload.getVal(path.startsWith("/") ? path : "/"+path);
				if(dObj==null || dObj.isDataObjEmpty()) continue;

				pathChecksumMap.put(path , encodedMsg(dObj));
			}
			if(pathChecksumMap.isEmpty()) return;

			try{
				ObjectMapper mapper =  CubeObjectMapperProvider.getInstance();
				for(Entry<String,String> e : pathChecksumMap.entrySet()){
					String path = e.getKey();
					String checksum = e.getValue();
					payload.put(path , new JsonDataObj(JsonNodeFactory.instance.textNode(checksum),mapper));
				}

			}catch (Exception e){
				LOGGER.error("Payload modification error " , e);
			}
		}

		public JsonPathTransformer(@JsonProperty("jsonPaths") List<String> paths , @JsonProperty("operation") Operation op){
			jsonPaths = paths;
			operation = op;
		}
	}

	public static class ServiceApiPathFilter implements Filter {

		public static final String TYPE = "ServiceApiPath";
		public static String MATCH_ALL_REGEX = ".*";

		@JsonProperty("service")
		public final Optional<String> service;
		@JsonProperty("apiPath")
		public final Optional<String> apiPath;

		@JsonIgnore
		private String name;

		@Override
		public String toString(){
			if(name!=null) return name;
			name = "ServiceApiPathFilter "+" service "+ service + " apiPath "+apiPath ;
			return name;
		}

		@Override
		public boolean filter(Event e) {
			if(e.eventType != EventType.HTTPResponse) return false;

			//This filter is service and apiPath based   getService() and getApiPath()
			// have already done the filtering.
			//Nothing else to do
			return true;
		}

		@Override
		public String getService() {
			return service.orElse(MATCH_ALL_REGEX);
		}

		@Override
		public String getApiPath() {
			return apiPath.orElse(MATCH_ALL_REGEX);
		}

		public ServiceApiPathFilter(@JsonProperty("service") String srvc , @JsonProperty("apiPath")String path){
			service = Optional.ofNullable(srvc);
			apiPath = Optional.ofNullable(path);
		}
	}

	public Filter filter;

	public Transform transform;

	public  FilterTransform(Filter f , Transform t){
		filter = f;
		transform = t;
	}
	public  FilterTransform(){}



	/*
	public static void main(String[] args){

		List<String> list = new ArrayList<>();
		list.add("papa");
		list.add("tatatta");
		FilterTransform ft = new FilterTransform(new ServiceApiPathFilter("service1" , "path2") , new JsonPathTransformer(list , Operation.TRUNCATE));

		List<FilterTransform> ftlist = new ArrayList<>();
		ftlist.add(ft);
		try{
			String serializedStr =  CubeObjectMapperProvider.getInstance().writeValueAsString(ftlist);
			System.out.println(serializedStr);

			ftlist = CubeObjectMapperProvider.getInstance().readValue(serializedStr , new TypeReference<List<FilterTransform>>(){});

			System.out.println("ddd "+ ftlist);



		}catch (Exception e){
			e.printStackTrace();
		}
	}*/

}


