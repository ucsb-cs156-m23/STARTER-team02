package edu.ucsb.cs156.example.controllers;

import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;

@Profile("development")
@RestController
@Slf4j
public class FrontendProxyController {
  @GetMapping({"/", "/{path:^(?!api|oauth2|swagger-ui|h2-console).*}/**"})
  // @GetMapping("/")

  public ResponseEntity<?> proxy(ProxyExchange<byte[]> proxy) {
    String path = proxy.path("/");
    log.info("path={}",path);
    try {
      log.info("about to call proxy.uri");
      return proxy.uri("http://localhost:3000/" + path).get();
    } catch (ResourceAccessException e) {
      log.info("caught ResourceAccessException: {}", e.getMessage());
      if (e.getCause() instanceof ConnectException) {
        String instructions = """
                <p>Failed to connect to the frontend server...</p>
                <p>On Dokku, be sure that <code>PRODUCTION</code> is defined.</p>
                <p>On localhost, open a second terminal window, cd into <code>frontend</code> and type: <code>npm install; npm start</code></p>
                <p>Or, you may click to access: </p>
                <ul>
                  <li><a href='/oauth2/authorization/google'>login with Google</a></li>
                  <li><a href='/logout'>logout</a></li>
                  <li><a href='/swagger-ui/index.html'>/swagger-ui/index.html</a></li>
                  <li><a href='/h2-console'>/h2-console</a></li>
                </ul>""";

        return ResponseEntity.ok(instructions);
      }
      throw e;
    } catch (Exception e) {
      log.info("caught Exception: {}", e.getMessage());
      throw e;
    }
  }
}
