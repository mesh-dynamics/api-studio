const cryptoRandomString = require('crypto-random-string');

const generateTraceKeys = (tracer) => {
  let traceIdKey, spanIdKey, parentSpanIdKeys = [];
  switch (tracer) {
      case "jaeger":
          traceIdKey = "uber-trace-id"
          parentSpanIdKeys = ["uberctx-parent-span-id"]
          // no span id key
          break
          
      case "zipkin":
          traceIdKey = "x-b3-traceid"
          parentSpanIdKeys = ["baggage-parent-span-id", "x-b3-parentspanid"]
          spanIdKey = "x-b3-spanid"
          break;

      case "datadog":
          traceIdKey = "x-datadog-trace-id"
          parentSpanIdKeys = ["ot-baggage-parent-span-id"]
          spanIdKey = "x-datadog-parent-id"
          break

      case "meshd": // default to meshd
      default:
          traceIdKey = "md-trace-id";
          parentSpanIdKeys = ["mdctxmd-parent-span"];
          // no span id key    
  }
  return {traceIdKey, spanIdKey, parentSpanIdKeys}
}

const generateTraceIdDetails = (tracer, spanId) => {
  let traceId = cryptoRandomString({length:16})
  if (tracer==="meshd" || tracer==="jaeger" || !tracer) {
      if (!spanId)
          throw new Error("Error generating traceId: spanId not present")
      
      return {traceId:`${traceId}:${spanId}:0:1`, traceIdForEvent: traceId}; // full and only traceId part for event
  } else if (tracer==="datadog") {
      traceId = cryptoRandomString({length:19, type: "numeric"})
      return {traceId, traceIdForEvent: traceId}
  } else {
      return {traceId, traceIdForEvent: traceId}; // both same
  }
}

const generateSpanId = (tracer) => {
  if(tracer==="datadog") {
      return cryptoRandomString({length:19, type: "numeric"})
  } else {
      return cryptoRandomString({length:16})
  }
}

const generateSpecialParentSpanId = (tracer) => {
  return "ffffffffffffffff"    
}

const extractTraceIdDetails = (traceId, tracer) => {
  if (traceId && (tracer==="meshd" || tracer==="jaeger" || !tracer)) {
    const [traceIdForEvent] = traceId.split(":")
    return {traceId, traceIdForEvent}
  } else {
    return {traceId, traceIdForEvent: traceId}; // both same
  }
}


const getTraceDetails = (mockContext, headers) => {
  const tracer = mockContext.tracer
  const traceKeys = generateTraceKeys(tracer)
  const {traceIdKey, spanIdKey, parentSpanIdKeys} = traceKeys;
  
  let parentSpanId = mockContext.parentSpanId
  if(!parentSpanId) {
      for(const key of parentSpanIdKeys) {
          parentSpanId = headers[key]
          if (parentSpanId)
              break;
      }
  }

  if (!parentSpanId) {
      parentSpanId = generateSpecialParentSpanId(tracer)
  }

  const spanId = headers[spanIdKey] || generateSpanId(tracer);
  
  let traceIdDetails = {} 
  if(mockContext.traceId) {
      traceIdDetails = extractTraceIdDetails(mockContext.traceId);
  } else if(headers[traceIdKey]) {
      traceIdDetails = extractTraceIdDetails(headers[traceIdKey]);
  } else {
      traceIdDetails = generateTraceIdDetails(tracer, spanId)
  }

  const traceDetails = {tracer, traceKeys, traceIdDetails, spanId, parentSpanId};
  return traceDetails;
}

const extractSpanId = (tracer, headers) => {
  let spanId;
  switch (tracer) {
    case "jaeger":
      traceIdKey = "uber-trace-id"
      spanId = headers[traceIdKey] && headers[traceIdKey].split(":")[1]
      break

    case "zipkin":
      spanIdKey = "x-b3-spanid"
      spanId = headers[spanIdKey]
      break;

    case "datadog":
      spanIdKey = "x-datadog-parent-id"
      spanId = headers[spanIdKey]
      break

    case "meshd": // default to meshd
    default:
      traceIdKey = "md-trace-id";
      spanId = headers[traceIdKey] && headers[traceIdKey].split(":")[1]
    // no span id key
  }
  return spanId;
}

module.exports = {
  generateTraceKeys,
  generateSpanId, 
  generateSpecialParentSpanId,
  generateTraceIdDetails,
  extractTraceIdDetails,
  getTraceDetails,
  extractSpanId
};