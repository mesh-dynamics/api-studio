package com.cube.ws;

import static io.md.core.Utils.buildErrorResponse;

import io.md.dao.MockWithCollection;
import io.md.dao.Recording;
import java.time.Instant;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import io.md.injection.DynamicInjector;
import io.md.injection.DynamicInjectorFactory;
import io.md.tracer.TracerMgr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.FnReqResponse;
import io.md.dao.Event;
import io.md.dao.HTTPResponsePayload;
import io.md.services.FnResponse;
import io.md.services.MockResponse;
import io.md.services.Mocker;
import io.md.services.RealMocker;
import io.md.utils.Constants;
import io.md.core.Utils;

import com.cube.core.ServerUtils;
import com.cube.dao.ReqRespStore;

/**
 * @author prasad
 *
 */
@Path("/ms")
public class MockServiceHTTP {

    private static final Logger LOGGER = LogManager.getLogger(MockServiceHTTP.class);

	@Path("/health")
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        Map solrHealth = ServerUtils.solrHealthCheck(config.solr);
        Map respMap = new HashMap(solrHealth);
        respMap.put(Constants.SERVICE_HEALTH_STATUS, "MS is healthy");
        return Response.ok().type(MediaType.APPLICATION_JSON).entity((new JSONObject(respMap)).toString()).build();
    }

	// TODO: unify the following two methods and extend them to support all @Consumes types -- not just two.
	// An example here: https://stackoverflow.com/questions/27707724/consume-multiple-resources-in-a-restful-web-service

    @POST
    @Path("{customerId}/{app}/{instanceId}/{service}/{method}/{var:.+}")
    @Consumes(MediaType.WILDCARD)
    public Response postForms(@Context UriInfo ui,
                              @Context HttpHeaders headers,
                              @PathParam("var") String path,
                              @PathParam("customerId") String customerId,
                              @PathParam("app") String app,
                              @PathParam("instanceId") String instanceId,
                              @PathParam("service") String service,
                              @PathParam("method") String httpMethod,
                              String body) {
        LOGGER.info(String.format("customerId: %s, app: %s, path: %s, uriinfo: %s, body: %s", customerId, app, path,
            ui.toString(), body));
        MockWithCollection mockWithCollection = io.md.utils.Utils.getMockCollection(rrstore , customerId , app , instanceId, false);
        return getResp(ui, path, new MultivaluedHashMap<>(), customerId, app, instanceId, service, httpMethod , body, headers.getRequestHeaders(), mockWithCollection
            , Optional.empty());
    }


    private Response errorResponse(String errorReason) {
        return Response.serverError().type(MediaType.APPLICATION_JSON).
            entity((new JSONObject(Map.of(Constants.REASON, errorReason))).toString()).build();
    }

    private Response getFuncResp(Event event, FnResponse fnResponse) {
        try {
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(fnResponse)
                .build();
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(
                Map.of(
                    Constants.API_PATH_FIELD, event.apiPath,
                    Constants.TRACE_ID_FIELD, event.getTraceId())) , e);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    "Unable to find response path in json ")).build();
        }
    }

    @POST
    @Path("/mockEvent")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mockEvent(Event event, @Context UriInfo ui) {
        try {
            MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
            Optional<Instant> lowerBound =
                Optional.ofNullable(queryParams.getFirst(io.md.constants.Constants.LOWER_BOUND)).flatMap(Utils::msStrToTimeStamp);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(mocker.mock(event, lowerBound, Optional.empty()))
                .build();
        } catch (Mocker.MockerException e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, e.errorType, e.getMessage())).build();
        }
    }

    @POST
    @Path("/thrift")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mockThrift(Event thriftMockRequest) {
        try {
            return mocker.mock(thriftMockRequest, Optional.empty(), Optional.empty()).response
                .map(matchingResponse ->
                    Response.ok().type(MediaType.APPLICATION_JSON).entity(matchingResponse).build())
                .orElseThrow(() -> new Exception("No Matching Response Event Found"));
        } catch (Exception e) {
            return Response.serverError()
                .entity((new JSONObject(Map.of(Constants.MESSAGE, e.getMessage()))).toString())
                .build();
        }
    }

    // TODO: this will be deprecated
    @POST
    @Path("/fr")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response funcJson(@Context UriInfo uInfo,
                             String fnReqResponseAsString) {
        try {
            FnReqResponse fnReqResponse = jsonMapper.readValue(fnReqResponseAsString, FnReqResponse.class);
            String traceIdString = fnReqResponse.traceId.orElse("N/A");
            LOGGER.info(new ObjectMessage(Map.of("state" , "Before Mock", "func_name" ,  fnReqResponse.name ,
                "trace_id" , traceIdString)));
            var counter = new Object() {int x = 0;};
            if (fnReqResponse.argVals != null) {
                Arrays.stream(fnReqResponse.argVals).forEach(argVal ->
                    LOGGER.info(new ObjectMessage(Map.of("state" , "Before Mock", "func_name" ,  fnReqResponse.name ,
                        "trace_id" , traceIdString , "arg_hash" , fnReqResponse.argsHash[counter.x] , "arg_val_" + counter.x++ , argVal))));
            }
            ServerUtils.preProcess(fnReqResponse);
            Optional<String> collection = rrstore.getCurrentRecordingCollection(Optional.of(fnReqResponse.customerId),
                Optional.of(fnReqResponse.app), Optional.of(fnReqResponse.instanceId));
            return collection.map(collec ->
                rrstore.getFunctionReturnValue(fnReqResponse, collec).map(retValue -> {
                        LOGGER.info(new ObjectMessage(Map.of("state" , "After Mock" , "func_name" , fnReqResponse.name ,
                            "trace_id" , traceIdString , "ret_val" , retValue.retVal)));
                        try {
                            String retValueAsString = jsonMapper.writeValueAsString(retValue);
                            return Response.ok().type(MediaType.APPLICATION_JSON).entity(retValueAsString).build();
                        } catch (JsonProcessingException e) {
                            LOGGER.error(new ObjectMessage(Map.of("func_name", fnReqResponse.name,
                                "trace_id", traceIdString)) , e);
                            String errorReason = "Unable to parse func response ";
                            return errorResponse(errorReason + e.getMessage());
                        }
                    }
                ).orElseGet(() -> {
                        String errorReason = "Unable to find matching request";
                        LOGGER.error(new ObjectMessage(Map.of("func_name" , fnReqResponse.name , "trace_id"
                            , traceIdString , Constants.REASON , errorReason)));
                        return errorResponse(errorReason);}))
                .orElseGet(() -> {
                        String errorReason = "Unable to locate collection for given customer, app, instance combo";
                        LOGGER.error(new ObjectMessage(Map.of("func_name" , fnReqResponse.name , "trace_id"
                            , traceIdString , Constants.REASON , errorReason)));
                        return errorResponse(errorReason);});
        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).
                entity("{\"reason\" : \"Unable to parse function request object " + e.getMessage()
                    + " \"}").build();
        }
    }

    @POST
    @Path("mockWithCollection/{replayCollection}/{recordingId}/{traceId}/{service}/{method}/{var:.+}")
    @Consumes(MediaType.WILDCARD)
    public Response postMockWithCollection(@Context UriInfo ui, @PathParam("var") String path,
        @Context HttpHeaders headers,
        @PathParam("replayCollection") String replayCollection,
        @PathParam("recordingId") String recordingId,
        @PathParam("traceId") String traceId,
        @PathParam("service") String service, @PathParam("method") String httpMethod,
        byte[] body) {
	    LOGGER.info("RECEIVED BYTE BODY IN MOCK : " + Base64.getEncoder().encodeToString(body));
	    MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
	    String runId = queryParams.getFirst(Constants.RUN_ID_FIELD);

	    LOGGER.info(String.format("path: %s, uriinfo: %s, body: %s, replayCollection: %s, recordingId: %s", path,
            ui.toString(), body, replayCollection, recordingId));
        Optional<Recording> optionalRecording = rrstore.getRecording(recordingId);
        if(optionalRecording.isEmpty()) {
            LOGGER.error(new ObjectMessage(
                Map.of(
                    Constants.RECORDING_ID, recordingId,
                    Constants.SERVICE_FIELD, service)));
            return notFound();
        }
        Recording recording = optionalRecording.get();
        MultivaluedMap<String, String> hdrs = headers.getRequestHeaders();
        Optional<String> dynamicInjCfgVersion = ServerUtils.getCustomHeaderValue(hdrs , Constants.DYNACMIC_INJECTION_CONFIG_VERSION_FIELD).or(()->recording.dynamicInjectionConfigVersion);
        Response response = getResp(ui, path, new MultivaluedHashMap<>(), recording.customerId, recording.app, recording.instanceId, service,
            httpMethod , body, hdrs, new MockWithCollection(replayCollection, recording.collection, recording.templateVersion, runId, dynamicInjCfgVersion, true), Optional.of(traceId));
        rrstore.commit();
        return response;
    }

    @POST
    @Path("mockWithRunId/{replayCollection}/{recordingId}/{traceId}/{runId}/{service}/{method}/{var:.+}")
    @Consumes(MediaType.WILDCARD)
    public Response postMockWithRunId(@Context UriInfo ui, @PathParam("var") String path,
        @Context HttpHeaders headers,
        @PathParam("replayCollection") String replayCollection,
        @PathParam("recordingId") String recordingId,
        @PathParam("traceId") String traceId,
        @PathParam("service") String service,
        @PathParam("runId") String runId , @PathParam("method") String httpMethod,
        String body) {

        LOGGER.info(String.format(" path: %s, uriinfo: %s, body: %s, replayCollection: %s, recordingId: %s", path,
            ui.toString(), body, replayCollection, recordingId));
        Optional<Recording> optionalRecording = rrstore.getRecording(recordingId);
        if(optionalRecording.isEmpty()) {
            LOGGER.error(new ObjectMessage(
                Map.of(
                    Constants.RECORDING_ID, recordingId,
                    Constants.SERVICE_FIELD, service)));
            return notFound();
        }
        Recording recording = optionalRecording.get();
        String recCollection = (recording.recordingType == Recording.RecordingType.History) ? "NA" : recording.collection;

        MultivaluedMap<String, String> hdrs = headers.getRequestHeaders();
        Optional<String> dynamicInjCfgVersion = ServerUtils.getCustomHeaderValue(hdrs , Constants.DYNACMIC_INJECTION_CONFIG_VERSION_FIELD).or(()->recording.dynamicInjectionConfigVersion);
        LOGGER.debug(String.format("MockWithRunId Passing collection %s for recordingType %s dynamicInjCfgVersion %s" , recCollection , recording.recordingType.toString(), dynamicInjCfgVersion.orElse(null)  ));

        Response response = getResp(ui, path, new MultivaluedHashMap<>(), recording.customerId, recording.app, recording.instanceId, service,
            httpMethod , body, hdrs, new MockWithCollection(replayCollection, recCollection, recording.templateVersion, runId, dynamicInjCfgVersion, true), Optional.of(traceId));
        rrstore.commit();
        return response;
    }



    private Response getResp(UriInfo ui, String path, MultivaluedMap<String, String> formParams,
        String customerId, String app, String instanceId,
        String service, String method, String body, MultivaluedMap<String, String> headers,
        MockWithCollection collection, Optional<String> traceId)
    {

        LOGGER.info(io.md.utils.Utils.createLogMessasge(io.md.constants.Constants.MESSAGE, "Attempting to mock request",
            io.md.constants.Constants.CUSTOMER_ID_FIELD, customerId, io.md.constants.Constants.APP_FIELD, app
            , io.md.constants.Constants.INSTANCE_ID_FIELD, instanceId, io.md.constants.Constants.SERVICE_FIELD, service,
            io.md.constants.Constants.METHOD_FIELD, method, io.md.constants.Constants.PATH_FIELD, path));

        Optional<Event> respEvent = Optional.empty();
        try {
            Event mockRequestEvent = io.md.utils.Utils
                .createRequestMockNew(path, formParams, customerId, app, instanceId,
                    service, method, body, headers, ui.getQueryParameters(), traceId, tracerMgr);
            MockResponse mockResponse = mocker.mock(mockRequestEvent, Optional.empty(),  Optional.of(collection));
            respEvent = mockResponse.response;

        } catch (Exception e) {
                LOGGER.error(io.md.utils.Utils.createLogMessasge(
                    io.md.constants.Constants.MESSAGE, "Unable to mock request, exception while creating request",
                    io.md.constants.Constants.CUSTOMER_ID_FIELD, customerId, io.md.constants.Constants.APP_FIELD, app
                    , io.md.constants.Constants.INSTANCE_ID_FIELD, instanceId, io.md.constants.Constants.SERVICE_FIELD, service,
                    io.md.constants.Constants.METHOD_FIELD, method, io.md.constants.Constants.PATH_FIELD, path, io.md.constants.Constants.BODY,
                    body), e);
        }

        return respEvent
            .flatMap(this::createResponseFromEvent)
            .orElseGet(this::notFound);
    }

    private Optional<Response> createResponseFromEvent(
        Event respEventVal) {

        HTTPResponsePayload responsePayload;
        try {
            responsePayload =  (HTTPResponsePayload) respEventVal.payload;
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Not able to deserialize response event",
                Constants.ERROR, e.getMessage()
            )), e);
            return Optional.empty();
        }

        ResponseBuilder builder = Response.status(responsePayload.getStatus());
        responsePayload.getHdrs().forEach((fieldName, fieldValList) -> fieldValList.forEach((val) -> {
            // System.out.println(String.format("key=%s, val=%s", fieldName, val));
            // looks like setting some headers causes a problem, so skip them
            // TODO: check if this is a comprehensive list
            if (Utils.ALLOWED_HEADERS.test(fieldName) && !fieldName.startsWith(":")) {
                builder.header(fieldName, val);
            }
        }));
        return Optional.of(builder.entity(responsePayload.getBody()).build());
    }


    private Response notFound() {
	    return Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
    }


    /**
     *
     * @param config
     */
	@Inject
	public MockServiceHTTP(Config config) {
		super();
		this.config = config;
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
        //LOGGER.info("Cube mock service started");

        mocker = new RealMocker(rrstore);
        tracerMgr = new TracerMgr(rrstore);
	}


	private ReqRespStore rrstore;
	private ObjectMapper jsonMapper;
    private static String tracefield = Constants.DEFAULT_TRACE_FIELD;
	private final Config config;

	private Mocker mocker;
	private TracerMgr tracerMgr;


}
