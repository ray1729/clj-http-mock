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
             '[clj-http-mock :as mock])
             

    (mock/with-mock-routes
      [(mock/route :get "http://www.google.com") (constantly {:status 200 :body "Mocked"})]
      (http/get "http://www.google.com/"))
      
    ;; => {:status 200 :body "Mocked"}


## License

Copyright © 2015 Ray Miller <ray@1729.org.uk>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
