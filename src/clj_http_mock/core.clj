(ns clj-http-mock.core
  (:require [clj-http.core]
            [robert.hooke]
            [medley.core :refer [map-keys]]
            [ring.util.codec :refer [form-decode]])
  (:import [org.apache.http HttpEntity]
           [java.net URL]))

(def ^:dynamic *mock-routes* [])
(def ^:dynamic *in-isolation* false)

(def any (constantly true))

(def any-route
  {:method       any
   :scheme       any
   :host         any
   :port         any
   :path         any
   :query-params any})

(letfn [(get-port [^URL u]
          (let [p (.getPort u)]
            (if (> p 0)
              p
              (.getDefaultPort u))))
        (get-query-params [^URL u]
          (when-let [query-string (.getQuery u)]
            (map-keys keyword (form-decode u))))]
  (defn parse-url
    [s]
    (let [^URL u (URL. s)]
      {:scheme       (keyword (.getProtocol u))
       :host         (.getHost u)
       :port         (get-port u)
       :path         (.getPath u)
       :query-params (get-query-params u)})))

(defn route
  ([m]
   (merge {:method :get :scheme :http :port nil :path "/" :query-parms nil} m))

  ([method url]
   (assoc (parse-url url) :method method))

  ([method url query-params]
   (assoc (parse-url url) :method method :query-params query-params)))

(defmacro with-mock-routes
  [routes & body]
  `(binding [*mock-routes* ~routes]
     ~@body))

(defmacro with-mock-routes-in-isolation
  [routes & body]
  `(binding [*in-isolation* true]
     (with-mock-routes ~routes ~@body)))

(defn set-mock-routes!
  [routes]
  (alter-var-root #'*mock-routes* (constantly routes)))

(defn set-in-isolation!
  [true-or-false]
  (alter-var-root #'*in-isolation* (constantly true-or-false)))

(defn matches?
  [wanted val]
  (cond
   (nil? wanted) (nil? val)
   (fn? wanted)  (wanted val)
   (set? wanted) (contains? wanted val)
   (map? wanted) (and (map? val)
                      (= (keys wanted) (keys val))
                      (every? (fn [k] (matches? (wanted k) (val k))) (keys wanted)))
   (instance? java.util.regex.Pattern wanted) (and val (re-find wanted val))
   :else (= wanted val)))

(defn request-method-matches?
  [route-spec {:keys [request-method] :as request}]
  (or (= (:method route-spec) :any)
      (matches? (:method route-spec) request-method)))

(defn scheme-matches?
  [route-spec {:keys [scheme]}]
  (matches? (:scheme route-spec) scheme))

(defn server-name-matches?
  [route-spec {:keys [server-name]}]
  (matches? (:host route-spec) server-name))

(defn server-port-matches?
  [{:keys [scheme port] :as route-spec} {:keys [server-port]}]
  (or (matches? port server-port)
      (and (= scheme :http)  (contains? #{nil  80} port) (contains? #{nil  80} server-port))
      (and (= scheme :https) (contains? #{nil 443} port) (contains? #{nil 443} server-port))))

(defn uri-matches?
  [{:keys [path] :as route-spec} {:keys [uri]}]
  (or (matches? path uri)
      (and (#{"/" ""} path)
           (#{"/" ""} uri))))

(defn query-string-matches?
  [route-spec {:keys [query-string]}]
  (let [query (when (not-empty query-string) (map-keys keyword (form-decode query-string)))]
    (matches? (:query-params route-spec) query)))

(defn route-matches?
  [route-spec request]
  (every? (fn [pred] (pred route-spec request))
          [request-method-matches?
           scheme-matches?
           server-name-matches?
           server-port-matches?
           uri-matches?
           query-string-matches?]))

(defn- mock-handler-for
  [request]
  (loop [candidates (partition 2 *mock-routes*)]
    (when-let [[route-spec handler] (first candidates)]
      (if (route-matches? route-spec request)
        handler
        (recur (rest candidates))))))

(defn- utf8-bytes
    "Returns the UTF-8 bytes corresponding to the given string."
    [^String s]
    (.getBytes s "UTF-8"))

(defn- unwrap-body
  [request]
  (update-in request [:body]
             (fn [body]
               (if (instance? HttpEntity body)
                 (.getContent body)
                 body))))

(defn try-intercept
  [origfn request]
  (if-let [handler (mock-handler-for request)]
    (let [response (handler (unwrap-body request))]
      (update-in response [:body] utf8-bytes))
    (if *in-isolation*
      (throw
       (Exception. (str "No matching mock route found to handle request: "
                        (pr-str (select-keys request [:scheme :request-method :server-name
                                                      :server-port :uri :query-string])))))
      (origfn request))))

(robert.hooke/add-hook #'clj-http.core/request
                       #'try-intercept)
