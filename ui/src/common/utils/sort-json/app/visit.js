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

/**
 * Sorts the keys on objects
 * @param {*} old                           - An object to sort the keys of, if not object just
 *                                            returns whatever was given
 * @param {Object} [sortOptions = {}]           - optional parameters
 * @param [options.reverse = false]         - When sorting keys, converts all keys to lowercase so
 *                                            that capitalization doesn't interfere with sort order
 * @param [options.ignoreCase = false]      - When sorting keys, converts all keys to
 * @param [options.depth = Infinity]        - Depth's level sorting keys on a
 *                                            multidimensional object
 * @returns {*}                             - Object with sorted keys, if old wasn't an object
 *                                            returns whatever was passed
 */
function visit(old, options) {
  const sortOptions = options || {};

  const ignoreCase = sortOptions.ignoreCase || false;
  const reverse = sortOptions.reverse || false;
  const depth = sortOptions.depth || Infinity;
  const level = sortOptions.level || 1;
  const processing = level <= depth;

  if (typeof (old) !== 'object' || old === null) {
    return old;
  }

  const copy = Array.isArray(old) ? [] : {};
  let keys = Object.keys(old);
  if (processing) {
    keys = ignoreCase ?
      keys.sort((left, right) => left.toLowerCase().localeCompare(right.toLowerCase())) :
      keys.sort();
  }

  if (reverse) {
    keys = keys.reverse();
  }

  keys.forEach((key) => {
    const subSortOptions = Object.assign({}, sortOptions);
    subSortOptions.level = level + 1;
    copy[key] = visit(old[key], subSortOptions);
  });

  return copy;
}

module.exports = visit;
