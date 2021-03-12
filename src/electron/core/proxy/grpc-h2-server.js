const http2 = require("http2");
const logger = require("electron-log");

const { fetch } = require("fetch-h2");
const { getApplicationConfig, store } = require("../fs-utils");
const { getServiceNameFromUrl, getServiceConfig } = require("./proxy-utils");
const { Deferred } = require("../../../shared/utils");

const { storeReqResEvent, getHttp2FetchUrl } = require("./h2server.utility");
const { getTraceDetails } = require("./trace-utils");

const setupGrpcH2Server = (mockContext, user) => {
  const { proxyDestination } = getApplicationConfig();
  const gRPCProxyPort =  store.get("gRPCProxyPort");
  const mockTarget = {
    protocol: proxyDestination.protocol, //`${mock.protocol}:`, // Do not forget the darn colon
    host: proxyDestination.host,
    port: proxyDestination.port,
  };

  const server = http2.createServer(async (req, res) => {
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader(
      "Access-Control-Allow-Methods",
      "GET, PUT, PATCH, POST, DELETE"
    );
    res.setHeader("Access-Control-Allow-Headers", "*");
    let body = [];

    // Event handlers for reading request body

    // 'data' event on request
    req.on("data", (data) => body.push(data));

    // 'end' event on request
    req.on("end", () => {
      const reqBodyBuffer = Buffer.concat(body);
      
      const path = req.url;
      logger.info("Received HTTP2 request at proxy: ", 
      { 
        "method": req.method, 
        "path": path, 
        "headers": req.headers,
        "body (base64 encoded)": reqBodyBuffer.toString("base64"),
      })

      const pathParts = path.split("/");
      const grpcMethod = pathParts.pop();
      var servicePart = pathParts.pop();
      var grpcService = servicePart.substr(servicePart.lastIndexOf(".") + 1);
      var grpcPackage = servicePart.substr(0, servicePart.lastIndexOf("."));
      pathParts.push(grpcPackage);


      const {
        config: { serviceConfigs },
      } = mockContext;

      const serviceConfigObject = getServiceConfig(serviceConfigs, path);
      logger.info(
        "Matched service prefix: ",
        serviceConfigObject
          ? serviceConfigObject.servicePrefix || serviceConfigObject.service
          : "(none, mocked)"
      );

      const matchedService =
        serviceConfigObject?.service || grpcService;
      logger.info("Matched service: ", matchedService);

      logger.info("Selected service config object :", serviceConfigObject);

      const isLive = serviceConfigObject && !serviceConfigObject.isMocked;
     
      const service = matchedService;

      // set trace and other headers
      let targetReqHeaders = {
        "content-type": "application/grpc",
        ...req.headers,
      };

      for (const name in targetReqHeaders) {
        if (name.startsWith(":")) {
          delete targetReqHeaders[name];
        }
      }

      const traceDetails = getTraceDetails(mockContext, targetReqHeaders);
      const {
        traceKeys: { traceIdKey, spanIdKey, parentSpanIdKeys },
        traceIdDetails: { traceId, traceIdForEvent },
        spanId,
        parentSpanId,
      } = traceDetails;
      const resourcePath = path.startsWith("/") ? path.substr(1) : path;

      if (!isLive) {
        const { accessToken, tokenType } = user;
        const token = `${tokenType} ${accessToken}`;
        logger.info("Setting authorization header authorization:", token);
        targetReqHeaders.authorization = token;
      }

      if (spanIdKey && !(spanIdKey in targetReqHeaders)) {
        logger.info(`Setting spanId header (${spanIdKey}): `, spanId);
        targetReqHeaders[spanIdKey] = spanId;
      }

      parentSpanIdKeys.forEach((key) => {
        if (!(key in targetReqHeaders)) {
          logger.info(`Setting parentSpanId header (${key}): `, parentSpanId);
          targetReqHeaders[key] = parentSpanId;
        }
      });

      if (traceIdKey && !(traceIdKey in targetReqHeaders)) {
        logger.info(`Setting traceId header (${traceIdKey}): `, traceId);
        targetReqHeaders[traceIdKey] = traceId;
      }
      
      const {strictMock, selectedApp} = mockContext;
      if (!strictMock) {
        logger.info('Setting dynamicInjectionConfigVersion', `Default${selectedApp}`);
        targetReqHeaders['dynamicInjectionConfigVersion'] = `Default${selectedApp}`;
      }
      
      // construct outgoing url
      let fetchUrl = "";
      if (isLive) {
        // live
        logger.info("Live service");
  
        fetchUrl = serviceConfigObject.url + "/" + resourcePath;
      } else {
        // mocked
        logger.info("Mocked service");
        const {
          runId,
          selectedApp,
          collectionId,
          customerName,
          recordingCollectionId,
          strictMock,
          replayInstance,
        } = mockContext;

        if (strictMock) {
          const strictMockApiPrefix = 'api/ms'
          fetchUrl = `${mockTarget.protocol}//${mockTarget.host}:${mockTarget.port}/${strictMockApiPrefix}/${customerName}/${selectedApp}/${replayInstance}/${service}/${resourcePath}`
        } else {
          const mockApiPrefix = "api/msc/mockWithRunId";
          fetchUrl = `${mockTarget.protocol}//${mockTarget.host}:${mockTarget.port}/${mockApiPrefix}/${collectionId}/${recordingCollectionId}/${customerName}/${selectedApp}/${traceIdForEvent}/${runId}/${service}/${resourcePath}`;
        }
      }

      logger.info("fetch URL: ", fetchUrl);
      
      let fetchConfig = {
        method: req.method,
        body: reqBodyBuffer,
        headers: targetReqHeaders,
        allowForbiddenHeaders: true,
      };

      const targetRespTrailersPromise = new Deferred();

      fetchConfig.onTrailers = (trailers) => {
        const trailersObject = trailers.toJSON();
        logger.info("Received target response trailers: ", trailersObject);
        targetRespTrailersPromise.resolve(trailersObject);
      };

      logger.info("http 2 target Request Headers: ", targetReqHeaders);

      // for non-https, the fetch-h2 library defaults to http1.1, so changing it to http2 to force http2 calls
      fetchUrl = getHttp2FetchUrl(fetchUrl)

      fetch(fetchUrl, fetchConfig)
        .then(async (targetResponse) => {
          const respBuffer = Buffer.from(await targetResponse.arrayBuffer());
          const base64BodyString = respBuffer.toString("base64");
          const targetRespHeaders = {}
          const mdTrailerHeaders = {}
          const mdTrailerHeadersPrefix = "md_trailer_header_"
          for(let [key, value] of targetResponse.headers.entries()) {
            if(key.startsWith(mdTrailerHeadersPrefix)) {
              mdTrailerHeaders[key.substr(mdTrailerHeadersPrefix.length)] = value
            } else {
              targetRespHeaders[key] = value
            }
          }
          logger.info("Response from target (base64 encoded): ", base64BodyString);
          logger.info("Target response HTTP status: ", targetResponse.status)
          logger.info("Target response HTTP headers: ", targetRespHeaders)
          logger.info("Target response HTTP MD trailers (in headers): ", mdTrailerHeaders)
          
          // some headers (of HTTP1) are not supported in http2 response
          delete targetRespHeaders["transfer-encoding"];

          res.writeHead(targetResponse.status, targetRespHeaders);
          res.write(respBuffer);

          const targetRespTrailers = await targetRespTrailersPromise.promise; // wait for trailers to be received
          const allTrailers = {...targetRespTrailers, ...mdTrailerHeaders}
          logger.info("Adding trailers: ", allTrailers);
          res.addTrailers(allTrailers);
          // res.addTrailers({"grpc-status": 0});
          res.end();

          if (isLive) {
            // live: store event into collection
            const responseFields = {
              body: base64BodyString,
              headers: targetRespHeaders,
              status: targetResponse.statusText,
              statusCode: targetResponse.status,
              outgoingApiPath: resourcePath, 
              service: service,
              method: "POST",
              trailers: targetRespTrailers,
            };

            const options = {
              mockContext,
              user,
              traceDetails: traceDetails,
              service: responseFields.service,
              outgoingApiPath: responseFields.outgoingApiPath,
              requestData: reqBodyBuffer.toString("base64"),
              headers: targetReqHeaders, //These are request headers, used to form requestEvent
              grpcEndpoint: serviceConfigObject.url + pathParts.join("/"),
              grpcService: grpcService,
              grpcMethod: grpcMethod,
            };

            storeReqResEvent(responseFields, options);
          }
        })
        .catch((error) => {
          logger.log("Error in fetch", error);
          res.write(error.message);
          res.end();
        });
    });
  });

  // TODO: We should add the below logger in listen callback.
  logger.info(`gRPC HTTP2 server listening on ${gRPCProxyPort}`);
  server.listen(gRPCProxyPort);
};

module.exports = setupGrpcH2Server;
