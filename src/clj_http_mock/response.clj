(ns clj-http-mock.response
  (:require [clojure.java.io :as io]))

(defn resource-response
  [resource-name]
  (constantly {:status 200 :headers {} :body (slurp (io/resource resource-name))}))

(defn ok-response
  [body]
  (constantly {:status 200 :headers {} :body body}))

(defn redirect-response
  [location & {:keys [status] :or {status 302}}]
  (constantly {:status status :headers {"location" location} :body nil}))

(defn not-found-response
  []
  (constantly {:status 404 :headers {} :body ""}))
