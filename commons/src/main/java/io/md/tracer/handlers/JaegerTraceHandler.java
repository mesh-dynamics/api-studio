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

package io.md.tracer.handlers;

import io.md.constants.Constants;

public class JaegerTraceHandler extends  JaegerStyleTracerHandler{
    @Override
    public String getTracekey(String app) {
        return Constants.JAEGER_SPAN_CONTEXT_KEY;
    }

    @Override
    public String getParentSpankey(String app) {
        return Constants.JAEGER_BAGGAGE_PARENT_SPAN;
    }
    @Override
    public Tracer getTracer() {
        return Tracer.Jaeger;
    }
}
