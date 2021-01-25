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
  const traceId = cryptoRandomString({length:16})
  if (tracer==="meshd" || tracer==="jaeger" || !tracer) {
      if (!spanId)
          throw new Error("Error generating traceId: spanId not present")
      
      return {traceId:`${traceId}:${spanId}:0:1`, traceIdForEvent: traceId}; // full and only traceId part for event
  } else {
      return {traceId, traceIdForEvent: traceId}; // both same
  }
}

const generateSpanId = (tracer) => {
  return cryptoRandomString({length:16})
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

module.exports = {
  generateTraceKeys,
  generateSpanId, 
  generateSpecialParentSpanId,
  generateTraceIdDetails,
  extractTraceIdDetails,
};