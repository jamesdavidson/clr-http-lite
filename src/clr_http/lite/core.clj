(ns clr-http.lite.core
  "Core HTTP request/response implementation."
  (:require [clojure.clr.io :as io]
            [clr-http.lite.util :as util]
            )
  (:import
   System.Net.WebRequest
   #_(java.io ByteArrayOutputStream InputStream IOException)
   #_(java.net URI URL HttpURLConnection)))

(defn safe-conj [a b]
  (if (vector? a)
    (conj a b)
    [a b]))

(defn parse-headers
  "Takes a URLConnection and returns a map of names to values.

  If a name appears more than once (like `set-cookie`) then the value
  will be a vector containing the values in the order they appeared
  in the headers."
  [conn]
  (let [headers (.Headers conn)]
    (apply merge-with
           safe-conj
           (for [header (.Headers conn)]
             {header (.Get headers header)}))))

(defn- coerce-body-entity
  "Coerce the http-entity from an HttpResponse to either a byte-array, or a
  stream that closes itself and the connection manager when closed."
  [{:keys [as]} conn]
  (let [ins (.GetResponseStream conn)]
    (if (or (= :stream as) (nil? ins))
      ins
      (util/to-byte-array ins))))

(defn request
  "Executes the HTTP request corresponding to the given Ring request map and
  returns the Ring response map corresponding to the resulting HTTP response.
  Note that where Ring uses InputStreams for the request and response bodies,
  the clj-http uses ByteArrays for the bodies."
  [{:keys [request-method scheme server-name server-port uri query-string
           headers content-type character-encoding body socket-timeout
           conn-timeout multipart debug insecure? save-request? follow-redirects] :as req}]
  (let [http-url (str (name scheme) "://" server-name
                      (when server-port (str ":" server-port))
                      uri
                      (when query-string (str "?" query-string)))
        request (WebRequest/Create http-url)
        Headers (.Headers request)
        ]
    (when (and content-type character-encoding)
      (.ContentType request (str content-type
                                 "; charset="
                                 character-encoding)))
    (when (and content-type (not character-encoding))
      (.ContentType request content-type))
    (doseq [[h v] headers]
      (.Add Headers h v))
    (when (false? follow-redirects)
      (.AllowAutoRedirect request false))
    ;(.Method request (.ToUpper (name request-method)))

    (when socket-timeout
      (.ReadWriteTimeout request socket-timeout))
    (when body
      (with-open [out (.GetRequestStream request)]
        (io/copy body out)))
    (let [
          response (.GetResponse request)
          ]
      (merge {:headers (parse-headers response)
              :status (.StatusCode response)
              :body (when-not (= request-method :head)
                      (coerce-body-entity req response))}
             (when save-request?
               {:request (assoc (dissoc req :save-request?)
                           :http-url http-url)})))))

(defn t []
  (-> {:scheme "http"
       :server-name "www.google.com"
       :request-method "GET"
       }
      request
      (update-in [:body] util/utf8-string)
      pr-str))
