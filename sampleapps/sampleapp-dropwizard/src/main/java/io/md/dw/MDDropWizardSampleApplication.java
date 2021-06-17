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

package io.md.dw;

import io.cube.jaxrs.ingress.LoggingFilter;
import io.cube.jaxrs.ingress.TracingFilter;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.md.dw.health.TemplateHealthCheck;
import io.md.dw.resources.HelloWorldResource;

public class MDDropWizardSampleApplication extends Application<MDDropWizardSampleConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MDDropWizardSampleApplication().run(args);
    }

    @Override
    public String getName() {
        return "MDDropWizardSample";
    }

    @Override
    public void initialize(final Bootstrap<MDDropWizardSampleConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final MDDropWizardSampleConfiguration configuration,
                    final Environment environment) {
        final HelloWorldResource resource = new HelloWorldResource(
            configuration.getTemplate(),
            configuration.getDefaultName()
        );
        final TemplateHealthCheck healthCheck =
            new TemplateHealthCheck(configuration.getTemplate());
        environment.healthChecks().register("template", healthCheck);
        environment.jersey().register(LoggingFilter.class);
        environment.jersey().register(TracingFilter.class);
        environment.jersey().register(resource);
    }

}
