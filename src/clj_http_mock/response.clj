(ns clj-http-mock.response
  (:require [clojure.java.io :as io]
            [clj-http.headers]))

(defn coerce-headers
  [headers]
  (into (clj-http.headers/header-map) headers))

(defn resource-response
  [resource-name & {:keys [headers coerce-body] :or {headers {} coerce-body identity}}]
  (constantly {:status  200
               :headers (coerce-headers headers)
               :body    (coerce-body (slurp (io/resource resource-name)))}))

(defn ok-response
  [body & {:keys [headers] :or {headers {}}}]
  (constantly {:status  200
               :headers (coerce-headers headers)
               :body    body}))

(defn redirect-response
  [location & {:keys [status headers] :or {status 302 headers {}}}]
  (constantly {:status  status
               :headers (coerce-headers (merge headers {"location" location}))
               :body    nil}))

(defn not-found-response
  [& {:keys [headers] :or {headers {}}}]
  (constantly {:status  404
               :headers (coerce-headers headers)
               :body    ""}))
