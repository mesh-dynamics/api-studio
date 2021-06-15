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

package io.md.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey.Type;
import io.md.core.ValidateCompareTemplate;
import java.util.Collection;
import java.util.Optional;

public class CompareTemplateVersioned extends CompareTemplate {

  @JsonProperty("service")
  public final String service;
  @JsonProperty("requestPath")
  public String requestPath;
  @JsonProperty("type")
  public final Type type;
  @JsonProperty("method")
  public final Optional<String> method;

  public CompareTemplateVersioned() {
    super();
    this.service = "";
    this.requestPath = "";
    this.type = Type.DontCare;
    this.method = Optional.empty();
  }

  public CompareTemplateVersioned(Optional<String> service, Optional<String> requestPath,
      Optional<String> method, Type type, CompareTemplate contained) {
    super(contained.prefixpath);
    this.service = service.orElse("");
    this.requestPath = requestPath.map(reqPath -> {
      return CompareTemplate.normaliseAPIPath(reqPath);
    }).orElse("");
    this.type = type;
    this.method = method;
    setRules(contained.getRules());
  }

    /*public CompareTemplateVersioned(CompareTemplateVersioned source) {
        this(source, source.getRules());
    }*/

  public CompareTemplateVersioned(CompareTemplateVersioned source
      , Collection<TemplateEntry> newRules) {
    super(source.prefixpath);
    this.service = source.service;
    this.requestPath = CompareTemplate.normaliseAPIPath(source.requestPath);
    this.type = source.type;
    this.method = source.method;
    setRules(newRules);
  }

  @Override
  public ValidateCompareTemplate validate() {
    ValidateCompareTemplate validateCompareTemplate = super.validate();
    if(!validateCompareTemplate.isValid()) {
      return new ValidateCompareTemplate (validateCompareTemplate
          .isValid, Optional.of("For requestPath: "
          + requestPath + " and Type: " + type.toString() +  " - "
          + validateCompareTemplate.getMessage() ));
    }
    return validateCompareTemplate;
  }

  public static CompareTemplateVersioned EMPTY_COMPARE_TEMPLATE_VERSION = new CompareTemplateVersioned(Optional.of(io.md.constants.Constants.NOT_APPLICABLE),
      Optional.of(io.md.constants.Constants.NOT_APPLICABLE), Optional.empty(),
      Type.DontCare, new CompareTemplate(""));
}
