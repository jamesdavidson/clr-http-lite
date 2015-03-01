(ns clr-http.test.core
  (:use [clojure.test]
        #_[clojure.clr.io :only [file]])
  (:require [clojure.pprint :as pp]
            [clr-http.lite.core :as core]
            [clr-http.lite.util :as util]))

;TESTING STRATEGY::
;RUN SERVER IN SEPARATE CLOJUREJVM PROCESS THEN RUN CLIENT TESTS
(def base-req
  {:scheme :http
   :server-name "localhost"
   :server-port 18080})

(defn request [req]
  (core/request (merge base-req req)))

(defn slurp-body [req]
  (slurp (:body req)))

(deftest ^{:integration true} makes-get-request
  (let [resp (request {:request-method :get :uri "/get"})]
    (is (= 200 (:status resp)))
    (is (= "get" (slurp-body resp)))))


(deftest ^{:integration true} makes-head-request

  (let [resp (request {:request-method :head :uri "/head"})]
    (is (= 200 (:status resp)))
    (is (nil? (:body resp)))))


(deftest ^{:integration true} sets-content-type-with-charset

  (let [resp (request {:request-method :get :uri "/content-type"
                       :content-type "text/plain" :character-encoding "UTF-8"})]
    (is (= "text/plain; charset=UTF-8" (slurp-body resp)))))

(deftest ^{:integration true} sets-content-type-without-charset

  (let [resp (request {:request-method :get :uri "/content-type"
                       :content-type "text/plain"})]
    (is (= "text/plain" (slurp-body resp)))))


(deftest ^{:integration true} sets-arbitrary-headers

  (let [resp (request {:request-method :get :uri "/header"
                       :headers {"X-My-Header" "header-val"}})]
    (is (= "header-val" (slurp-body resp)))))


(deftest ^{:integration true} sends-and-returns-byte-array-body

  (let [resp (request {:request-method :post :uri "/post"
                       :body (util/utf8-bytes "contents")})]
    (is (= 200 (:status resp)))
    (is (= "contents" (slurp-body resp)))))


(deftest ^{:integration true} returns-arbitrary-headers
  (let [resp (request {:request-method :get :uri "/get"})]
    (is (string? (get-in resp [:headers "Date"])))))


(deftest ^{:integration true} returns-status-on-exceptional-responses
  (let [resp (request {:request-method :get :uri "/error"})]
    (is (= 500 (:status resp)))))

(deftest ^{:integration true} returns-status-on-redirect

  (let [resp (request {:request-method :get :uri "/redirect" :follow-redirects false})]
    (is (= 302 (:status resp)))))

(deftest ^{:integration true} auto-follows-on-redirect
  (let [resp (request {:request-method :get :uri "/redirect"})]
    (is (= 200 (:status resp)))
    (is (= "get" (slurp-body resp)))))

#_(deftest ^{:integration true} sets-socket-timeout
  (try
    (request {:request-method :get :uri "/timeout" :socket-timeout 1})
    (throw (Exception. "Shouldn't get here."))
    (catch Exception e
      (is (or (= java.net.SocketTimeoutException (class e))
              (= java.net.SocketTimeoutException (class (.getCause e))))))))

;; HUC can't do this
;; (deftest ^{:integration true} delete-with-body
;;
;;   (let [resp (request {:request-method :delete :uri "/delete-with-body"
;;                        :body (.getBytes "foo bar")})]
;;     (is (= 200 (:status resp)))))

#_(deftest ^{:integration true} self-signed-ssl-get
  (let [t (doto (Thread. #(ring/run-jetty handler
                                          {:port 8081 :ssl-port 18082 :ssl? true
                                           :keystore "test-resources/keystore"
                                           :key-password "keykey"})) .start)]
    (Thread/sleep 1000)
    (try
      (is (thrown? javax.net.ssl.SSLException
                   (request {:request-method :get :uri "/get"
                             :server-port 18082 :scheme :https})))
      #_(let [resp (request {:request-method :get :uri "/get" :server-port 18082
                             :scheme :https :insecure? true})]
          (is (= 200 (:status resp)))
          (is (= "get" (slurp-body resp))))
      (finally
       (.stop t)))))

;; (deftest ^{:integration true} multipart-form-uploads
;;
;;   (let [bytes (util/utf8-bytes "byte-test")
;;         stream (ByteArrayInputStream. bytes)
;;         resp (request {:request-method :post :uri "/multipart"
;;                        :multipart [["a" "testFINDMEtest"]
;;                                    ["b" bytes]
;;                                    ["c" stream]
;;                                    ["d" (file "test-resources/keystore")]]})
;;         resp-body (apply str (map #(try (char %) (catch Exception _ ""))
;;                                   (:body resp)))]
;;     (is (= 200 (:status resp)))
;;     (is (re-find #"testFINDMEtest" resp-body))
;;     (is (re-find #"byte-test" resp-body))
;;     (is (re-find #"name=\"c\"" resp-body))
;;     (is (re-find #"name=\"d\"" resp-body))))

(deftest ^{:integration true} t-save-request-obj

  (let [resp (request {:request-method :post :uri "/post"
                       :body (util/utf8-bytes "foo bar")
                       :save-request? true})]
    (is (= 200 (:status resp)))
    (is (= {:scheme :http
            :http-url "http://localhost:18080/post"
            :request-method :post
            :uri "/post"
            :server-name "localhost"
            :server-port 18080}
           (dissoc (:request resp) :body)))))

;; (deftest parse-headers
;;   (are [headers expected]
;;        (let [iterator (BasicHeaderIterator.
;;                        (into-array BasicHeader
;;                                    (map (fn [[name value]]
;;                                           (BasicHeader. name value))
;;                                         headers))
;;                        nil)]
;;          (is (= (core/parse-headers iterator)
;;                 expected)))

;;        []
;;        {}

;;        [["Set-Cookie" "one"]]
;;        {"set-cookie" "one"}

;;        [["Set-Cookie" "one"]
;;         ["set-COOKIE" "two"]]
;;        {"set-cookie" ["one" "two"]}

;;        [["Set-Cookie" "one"]
;;         ["serVer"     "some-server"]
;;         ["set-cookie" "two"]]
;;        {"set-cookie" ["one" "two"]
;;         "server"     "some-server"}))

(deftest ^{:integration true} t-streaming-response

  (let [stream (:body (request {:request-method :get :uri "/get" :as :stream}))
        body (slurp stream)]
    (is (= "get" body))))
