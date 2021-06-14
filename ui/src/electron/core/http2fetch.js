/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const { connect, constants } = require('http2');
const { URL } = require('url');

const {
  HTTP2_HEADER_STATUS,
  HTTP2_HEADER_METHOD,
  HTTP2_HEADER_PATH,
  HTTP2_HEADER_ACCEPT,
  HTTP2_HEADER_USER_AGENT,
  HTTP2_HEADER_CONTENT_TYPE,
  HTTP2_HEADER_CONTENT_LENGTH,
} = constants;

const USER_AGENT = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36';

const methodsWithBody = [
  'POST',
  'PUT',
  'PATCH',
];

function getHeaders(stream) {
  return new Promise(resolve => {
    stream.on('response', headers => resolve(headers));
  });
}

async function getBody(stream) {
  const buf = [];

  for await (const chunk of stream) {
    buf.push(chunk);
  }

  return Buffer.concat(buf);
}

 async function fetch(url, options) {
    const {method} = options;
  const { pathname, search, origin } = new URL(url);

  const outgoingHeaders = options.headers || {};
  outgoingHeaders[HTTP2_HEADER_METHOD] = method.toUpperCase();
  outgoingHeaders[HTTP2_HEADER_PATH] = pathname + search;
  outgoingHeaders[HTTP2_HEADER_ACCEPT] = '*/*';
  outgoingHeaders[HTTP2_HEADER_USER_AGENT] = USER_AGENT;

  let payload;

  if (options.body && methodsWithBody.includes(method.toUpperCase())) {
    payload = Buffer.from(options.body);
  }

  const client = connect(origin);

  const req = client.request(outgoingHeaders);

  if (payload) {
    req.write(payload);
  }

  await new Promise(resolve => {
    req.end(resolve);
  });

  const [headers, body] = await Promise.all([
    getHeaders(req),
    getBody(req),
  ]);

  client.close();

  const result = { headers, body };
  const contentType = headers[HTTP2_HEADER_CONTENT_TYPE];

  if (body.length === 0) {
    result.body = null;
  }
  else if (contentType  && !contentType.startsWith('application/grpc')) {
    result.body = body.toString();
  }
  result.statusCode = getStatusText(headers[HTTP2_HEADER_STATUS]);

  return result;
}

 function getStatusText(code) {
  if (code === 200) {
    return 'OK';
  }

  for (const [key, value] of Object.entries(constants)) {
    if (value === code) {
      return key
        .replace('HTTP_STATUS_', '')
        .split('_')
        .map(word => {
          return word[0] + word.slice(1).toLocaleLowerCase();
        })
        .join(' ');
    }
  }
}

module.exports = {
    fetch,
    getStatusText
}