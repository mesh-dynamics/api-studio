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

'use strict';

const detectNewline = string => {
	if (typeof string !== 'string') {
		throw new TypeError('Expected a string');
	}

	const newlines = string.match(/(?:\r?\n)/g) || [];

	if (newlines.length === 0) {
		return;
	}

	const crlf = newlines.filter(newline => newline === '\r\n').length;
	const lf = newlines.length - crlf;

	return crlf > lf ? '\r\n' : '\n';
};

module.exports = detectNewline;
module.exports.graceful = string => (typeof string === 'string' && detectNewline(string)) || '\n';
