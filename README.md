# clj-http-mock

A Clojure library for mocking [clj-http](https://github.com/dakrone/clj-http) requests.

This library is heavily influenced by [clj-http-fake](https://github.com/myfreeweb/clj-http-fake)
and borrows one or two functions from it. The main differences are:

1. Mock routes are defined in a vector, not a map, and are searched in
order.
1. We use a different algorithm for matching routes, so avoid
the combinatorial explosion of permutiations and cross-product.
1. The routes themselves are defined as maps, and allow for very flexible matching.

## Installation

Available from Clojars:

    [clj-http-mock "0.1.0"]
    
## Usage

    (require '[clj-http.client :as http]
             '[clj-http-mock.core :as mock])
             
    (mock/with-mock-routes
      [(mock/route :get "http://www.google.com/")      
      (constantly {:status 200 :body "Mocked"})]
      (http/get "http://www.google.com/"))
    ;;=> {:status 200 :body "q=foo"}
    
To match any query parameters:

    (mock/with-mock-routes
      [(mock/route :get "http;//www.google.com/" mock/any)
       (fn [{:keys [query-string]}]
         {:status 200 :body query-string})]
      (http/get "http://www.google.com?q=mock"))
    ;;=> {:status 200 :body "q=mock"}

## Defining mock routes     
             
Mock routes are defined as a vector. Requests are matched against the
routes in the order they are defined, and the first match wins. If no
mock route matches, the request is passed on to the original
`clj-http.core/request` function unless `*in-isolation*` is true, in
which case an exception is thrown. You can set in-isolation globally by
calling `(mock/set-in-isolation! true)` or by using the helper macro
`mock/with-mock-routes-in-isolation` to set up your mock routes.

A mock route is simply a Clojure map with the keys `:method`, `:scheme`,
`:host`, `:port`, `:path`, and `:query-params`. The values in the map
may be constants, regular expressons, sets, functions, maps, or nil, and
are matched against the corresponding field in the request as follows:

1. nil matches `(nil? val)`
2. a function matches if `(f val)` returns a true value
3. a set matches if it contains `val`
4. a map matches if `val` is a map with the same keys and every value
matches recursively according to these rules
5. a regular-expression matches if `val` is a matching string
6. anything else must match according to Clojure's `=`

(I'm open to supporting `bag=` for collections if that anyone needs
that.)

There is special handling of `:port` to allow it to be unset in the mock
route or the request if it is the default port for the scheme. There is
also special handling of `:path`, which treats `""` and `/` as
equivalent.

For example, the following mock route:

      {:method :get 
       :scheme :http 
       :host "www.google.com"
       :port 80
       :path "/"
       :query-params {:q #".*"}}

would match all requests to `http://www.google.com/?q=.*`. The
`mock/route` function exists to make it easier to define routes. Called
with a single map argument, it will merge in some defaults, so the above
is equivalent to:

    (mock/route {:host "www.google.com" :query-params {:q #".*"}})
    
Called with an HTTP method (:get, :put, :post, etc.) and URL, it will
parse the URL to generate the route map for you. So we could also write:

    (mock/route :get "http://www.google.com/")
    
to match GET requests with no query string, or:

    (mock/route :get "http://www.google.com/" {:q #".*})
    
which will produce the map we started with. If you don't need special
matching of the query parameters (values are constants that much match
literally), you can define your route like so:

    (mock/route :get "http://www.google.com/?q=foo&sort=relevance")
    
Note that, because we parse the query string and match against the
resulting map, this route will match both
`http://www.google.com/?q=foo*sort=relevance` and
`http://www.google.com/?sort=relevance&q=foo`.

The helper macros `mock/with-mock-routes` and
`mock/with-mock-routes-in-isolation` expect their first argument to be a
vector with an even number of elements (pairs of mock route and
handler); subsequent expressions are evaluated with the appropriate
bindings in place.

## Generating responses

The `clj-http-mock.response` namespace contains a few functions for
generating responses:

* HTTP status 200, constant body

    (ok-response "Body text")

* HTTP status 200, body read from `clojure.java.io/resource`

    (resource-response "my-mocks/page.html")
    
* HTTP status 302, Location header set to `url`

    (redirect-response url)
    
* HTTP status 307, Location header set to `url`

    (redirect-response url :status 307)
    
* HTTP status 404, empty body

    (not-found-response)

## License

Copyright © 2015 Ray Miller <ray@1729.org.uk>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
