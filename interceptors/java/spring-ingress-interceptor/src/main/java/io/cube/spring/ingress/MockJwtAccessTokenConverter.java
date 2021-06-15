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

package io.cube.spring.ingress;

import java.util.Calendar;
import java.util.Map;

import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.util.JsonParser;
import org.springframework.security.oauth2.common.util.JsonParserFactory;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

public class MockJwtAccessTokenConverter extends JwtAccessTokenConverter {

  private JsonParser objectMapper = JsonParserFactory.create();
  @Override
  protected Map<String, Object> decode(String token) {
    try {
      Jwt jwt = JwtHelper.decode(token);
      String claimsStr = jwt.getClaims();
      Map<String, Object> claims = this.objectMapper.parseMap(claimsStr);
      if (claims.containsKey("exp") && claims.get("exp") instanceof Integer) {
        //Integer intValue = (Integer)claims.get("exp");
        //claims.put("exp", new Long((long)intValue));
        Calendar now = Calendar.getInstance();
        now.add(Calendar.YEAR, 10);
        now.getTime().getTime();
        claims.put("exp", now.getTime().getTime());
      }

      //this.getJwtClaimsSetVerifier().verify(claims);
      return claims;
    } catch (Exception var6) {
      throw new InvalidTokenException("Cannot convert access token to JSON", var6);
    }
  }
}

