package com.cube.utils;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import io.md.core.TemplateKey;
import io.md.core.ValidateCompareTemplate;
import io.md.services.Analyzer;
import io.md.utils.Constants;

import com.cube.core.ServerUtils;
import com.cube.dao.ReqRespStore;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;
import com.cube.golden.transform.TemplateSetTransformer;
import com.cube.golden.transform.TemplateUpdateOperationSetTransformer;

public class AnalysisUtils {

	private static final Logger LOGGER = LogManager.getLogger(AnalysisUtils.class);

	public static final DateTimeFormatter templateLabelFormatter =  DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss_SSS");

	public static void updateTemplateUpdateOperationSet(String customerId, String operationSetId,
		String updateOperations, ObjectMapper jsonMapper, ReqRespStore rrStore) throws Exception {

		TypeReference<HashMap<TemplateKey, SingleTemplateUpdateOperation>> typeReference =
			new TypeReference<>() {};
		Map<TemplateKey, SingleTemplateUpdateOperation> updates = jsonMapper
			.readValue(updateOperations,
				typeReference);
		// get existing operation set against the id specified
		Optional<TemplateUpdateOperationSet> updateOperationSetOpt = rrStore
			.getTemplateUpdateOperationSet(operationSetId);
		TemplateUpdateOperationSetTransformer transformer = new TemplateUpdateOperationSetTransformer();
		// merge operations
		TemplateUpdateOperationSet transformed = updateOperationSetOpt
			.flatMap(updateOperationSet -> Optional.of
				(transformer.updateTemplateOperationSet(updateOperationSet, updates)))
			.orElseThrow(() -> new Exception("Missing template update operation set for given id"));
		// save the merged operation set
		rrStore.saveTemplateUpdateOperationSet(transformed, customerId);
		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully updated template "
			+ "rules update op set", Constants.TEMPLATE_UPD_OP_SET_ID_FIELD, operationSetId)));
	}

	public static String updateTemplateSet(String templateUpdateOperationSetId,
		Optional<TemplateSet>
			templateSetOpt, ReqRespStore rrStore) throws Exception {
		// transform the template set based on the operations specified
		TemplateUpdateOperationSet updateOperationSet = rrStore
			.getTemplateUpdateOperationSet(templateUpdateOperationSetId).orElseThrow(()
				-> new Exception("Missing template update operation set"));
		TemplateSetTransformer transformer = new TemplateSetTransformer();
		TemplateSet updated = templateSetOpt.map(UtilException.rethrowFunction(
			templateSet -> transformer.updateTemplateSet(templateSet, updateOperationSet,
				rrStore))).orElseThrow(() -> new Exception("Missing template set"));
		// Validate updated template set
		ValidateCompareTemplate validTemplate = ServerUtils.validateTemplateSet(updated);
		if (!validTemplate.isValid()) {
			throw new Exception("Updated Template Not Valid " + validTemplate.getMessage());
		}
		// save the new template set (and return the new version as a part of the response)
		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully updated template "
				+ "set", Constants.OLD_TEMPLATE_SET_VERSION, templateSetOpt.map(templateSet ->
				templateSet.version).orElse("NA"), Constants.NEW_TEMPLATE_SET_VERSION,
			updated.version, Constants.CUSTOMER_ID_FIELD, updated.customer, Constants.APP_FIELD,
			updated.app, Constants.TEMPLATE_UPD_OP_SET_ID_FIELD, templateUpdateOperationSetId)));
		rrStore.saveTemplateSet(updated);
		return updated.version;
	}

	public static Response runAnalyze(Analyzer analyzer, ObjectMapper jsonMapper, String replayId,
		Optional<String> templateVersion) {
		Optional<io.md.dao.Analysis> analysis = analyzer.analyze(replayId, templateVersion);
		return analysis.map(av -> {
			String json;
			try {
				json = jsonMapper.writeValueAsString(av);
				return Response.ok(json, MediaType.APPLICATION_JSON).build();
			} catch (JsonProcessingException e) {
				LOGGER.error(String
					.format("Error in converting Analysis object to Json for replayid %s",
						replayId), e);
				return Response.serverError().build();
			}
		}).orElse(Response.serverError().build());
	}

}
