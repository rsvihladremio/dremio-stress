/**
 * Copyright 2022 Dremio
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.support.diagnostics.stress;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

/** HttpApiCall is the wrapper for HttpUrlConnection logic */
public class HttpApiCall implements ApiCall {

  public HttpApiCall(final boolean ignoreSSL) {
    if (ignoreSSL) {
      HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
      try {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(
            null,
            new X509TrustManager[] {
              new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                  return new X509Certificate[0];
                }
              }
            },
            new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public HttpApiResponse submitGet(URL url, Map<String, String> headers) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoInput(true);
    connection.setRequestMethod("GET");
    for (Map.Entry<String, String> kvp : headers.entrySet()) {
      connection.setRequestProperty(kvp.getKey(), kvp.getValue());
    }

    if (connection.getResponseCode() > 199 && connection.getResponseCode() < 400) {
      StringBuilder content = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        String strCurrentLine;
        while ((strCurrentLine = reader.readLine()) != null) {
          content.append(strCurrentLine);
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> value =
            mapper.readValue(content.toString(), new TypeReference<Map<String, Object>>() {});
        HttpApiResponse response = new HttpApiResponse();
        response.setResponseCode(connection.getResponseCode());
        response.setMessage(connection.getResponseMessage());
        response.setResponse(value);
        return response;
      }
    }
    StringBuilder error = new StringBuilder();
    InputStream errorCode = connection.getErrorStream();
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(errorCode, StandardCharsets.UTF_8))) {
      String strCurrentLine;
      while ((strCurrentLine = br.readLine()) != null) {
        error.append(strCurrentLine);
      }
      HttpApiResponse response = new HttpApiResponse();
      response.setResponseCode(connection.getResponseCode());
      response.setMessage(connection.getResponseMessage() + " ----- " + error);
      return response;
    }
  }

  @Override
  public HttpApiResponse submitPost(
      final URL url, final Map<String, String> headers, final String body) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoInput(true);
    connection.setRequestMethod("POST");
    for (Map.Entry<String, String> kvp : headers.entrySet()) {
      connection.setRequestProperty(kvp.getKey(), kvp.getValue());
    }
    if (body != null) {
      connection.setDoOutput(true);
      try (OutputStream stream = connection.getOutputStream()) {
        try (OutputStreamWriter streamWriter =
            new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
          streamWriter.write(body);
          streamWriter.flush();
        }
        stream.flush();
      }
    }

    if (connection.getResponseCode() > 199 && connection.getResponseCode() < 400) {
      StringBuilder content = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        String strCurrentLine;
        while ((strCurrentLine = reader.readLine()) != null) {
          content.append(strCurrentLine);
        }
      }
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> value =
          mapper.readValue(content.toString(), new TypeReference<>() {
          });
      HttpApiResponse response = new HttpApiResponse();
      response.setResponseCode(connection.getResponseCode());
      response.setMessage(connection.getResponseMessage());
      response.setResponse(value);
      return response;
    }
    StringBuilder error = new StringBuilder();
    InputStream errorCode = connection.getErrorStream();
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(errorCode, StandardCharsets.UTF_8))) {
      String strCurrentLine;
      while ((strCurrentLine = br.readLine()) != null) {
        error.append(strCurrentLine);
      }
      HttpApiResponse response = new HttpApiResponse();
      response.setResponseCode(connection.getResponseCode());
      response.setMessage(connection.getResponseMessage() + " ----- " + error);
      return response;
    }
  }
}
