(ns clj-http-mock.core-test
  (:require [clojure.test :refer :all]
            [clj-http-mock.core :as mock]
            [clj-http.client :as http]))

(mock/set-in-isolation! true)

(def mock-response (constantly {:status 200 :body "Mocked"}))

(defmacro mocked?
  [expr]
  `(is (= "Mocked" (:body ~expr))))

(defmacro not-mocked?
  [& expr]
  `(is (~'thrown-with-msg? Exception #"No matching mock route found to handle request"
                           ~expr)))

(deftest url-no-path
  (mock/with-mock-routes
    [(mock/route :get "http://foo.com") mock-response]
    (mocked? (http/get "http://foo.com"))
    (mocked? (http/get "http://foo.com/"))
    (not-mocked? (http/get "http://bar.com"))
    (not-mocked? (http/get "http://foo.com/some/other/path"))))

(deftest url-with-path
  (mock/with-mock-routes
    [(mock/route :get "http://foo.com/bar") mock-response
     (mock/route :get "http://foo.com/baz") mock-response]
    (doseq [u ["http://foo.com/bar" "http://foo.com/baz"]]
      (mocked? (http/get u)))
    (doseq [u ["http://foo.com" "http://foo.com/" "http://foo.com/quux"]]
      (not-mocked? (http/get u)))))

(deftest url-with-port
  (mock/with-mock-routes
    [(mock/route :get "http://foo.com:81/bar") mock-response]
    (mocked? (http/get "http://foo.com:81/bar"))
    (not-mocked? (http/get "http://foo.com/bar"))
    (not-mocked? (http/get "http://foo.com:8080/bar"))))

(deftest default-ports
  (mock/with-mock-routes
    [(mock/route {:scheme :http :host "foo.com" :path "/bar" :query-params nil}) mock-response
     (mock/route {:scheme :https :host "bar.com" :path "/foo" :query-params nil}) mock-response]
    (mocked? (http/get "http://foo.com/bar"))
    (mocked? (http/get "http://foo.com:80/bar"))
    (mocked? (http/get "https://bar.com/foo"))
    (mocked? (http/get "https://bar.com:443/foo"))
    (not-mocked? (http/get "https://foo.com/bar"))
    (not-mocked? (http/get "https://foo.com:443/bar"))
    (not-mocked? (http/get "https://foo.com:80/bar"))
    (not-mocked? (http/get "http://bar.com/foo"))
    (not-mocked? (http/get "http://bar.com:80/foo"))
    (not-mocked? (http/get "http://bar.com:443/foo"))))
