package org.craftercms.movies.omdb

// include a third-party library for easily calling the API
@Grab(value='io.github.http-builder-ng:http-builder-ng-core:1.0.4', initClass=false)
import groovyx.net.http.HttpBuilder

class OmdbService {

  // the base URL for all API calls
  String baseUrl

  // the API key needed for the calls
  String apiKey

  // The http client
  HttpBuilder http

  // creates an instance of the http client with the configured base URL
  def init() {
    http = HttpBuilder.configure {
      request.uri = baseUrl
    }
  }

  // performs a search call, returns the entries as maps
  def search(String title) {
    return [
      http.get() {
        // include the needed parameters
        request.uri.query  = [ apiKey: apiKey, t: title ]
      }
    ].flatten() // return a list even if the API only returns a single entry
  }

}
