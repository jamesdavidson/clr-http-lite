# `clr-http-lite`

A Clojure HTTP library for ClojureCLR similar to
[clj-http](http://github.com/dakrone/clj-http), but more lightweight.

## Installation
<!--
`clj-http-lite` is available as a Maven artifact
from [Clojars](http://clojars.org/clj-http-lite):

```clojure
[clj-http-lite "0.2.1"]
```
-->
## Differences from clj-http

- Instead of Apache HTTP client, clj-http-lite uses System.Net.WebRequest
- Does support cookies
- No automatic JSON decoding for response bodies
- No proxy-ing DELETEs with body
- No multipart form uploads
- No persistent connection support
- No support for insecure HTTPS connection (yet)
- namespace rename clj-http.* -> clr-http.lite.*

## Usage

The main HTTP client functionality is provided by the
`clr-http.lite.client` namespace:

```clojure
(require '[clr-http.lite.client :as client])
```

The client supports simple `get`, `head`, `put`, `post`, and `delete`
requests. Responses are returned as Ring-style response maps:

```clojure
(client/get "http://google.com")
=> {:status 200
    :headers {"date" "Sun, 01 Aug 2010 07:03:49 GMT"
              "cache-control" "private, max-age=0"
              "content-type" "text/html; charset=ISO-8859-1"
              ...}
    :body "<!doctype html>..."}
```

More example requests:

```clojure
(client/get "http://site.com/resources/id")

(client/get "http://site.com/resources/3" {:accept :json})

;; Various options:
(client/post "http://site.com/api"
  {:basic-auth ["user" "pass"]
   :body "{\"json\": \"input\"}"
   :headers {"X-Api-Version" "2"}
   :content-type :json
   :socket-timeout 1000
   :conn-timeout 1000
   :accept :json})

;; Need to contact a server with an untrusted SSL cert?
(client/get "https://alioth.debian.org" {:insecure? true})

;; If you don't want to follow-redirects automatically:
(client/get "http://site.come/redirects-somewhere" {:follow-redirects false})

;; Send form params as a urlencoded body
(client/post "http//site.com" {:form-params {:foo "bar"}})

;; Multipart form uploads/posts
;; a map or vector works as the multipart object. Use a vector of
;; vectors if you need to preserve order, a map otherwise.
(client/post "http//example.org" {:multipart [["title" "My Awesome Picture"]
                                              ["Content/type" "image/jpeg"]
                                              ["file" (clojure.java.io/file "pic.jpg")]]})
;; Multipart values can be one of the following:
;; String, InputStream, File, or a byte-array

;; Basic authentication
(client/get "http://site.com/protected" {:basic-auth ["user" "pass"]})
(client/get "http://site.com/protected" {:basic-auth "user:pass"})

;; Query parameters
(client/get "http://site.com/search" {:query-params {"q" "foo, bar"}})
```

The client will also follow redirects on the appropriate `30*` status
codes.

The client transparently accepts and decompresses the `gzip` and
`deflate` content encodings.

### Input coercion

```clojure
;; body as a byte-array
(client/post "http://site.com/resources" {:body my-byte-array})

;; body as a string
(client/post "http://site.com/resources" {:body "string"})

;; :body-encoding is optional and defaults to "UTF-8"
(client/post "http://site.com/resources"
             {:body "string" :body-encoding "UTF-8"})

;; body as a file
(client/post "http://site.com/resources"
             {:body (clojure.java.io/file "/tmp/foo") :body-encoding
             "UTF-8"})

;; :length is NOT optional for passing an InputStream in
(client/post "http://site.com/resources"
             {:body (clojure.java.io/input-stream "/tmp/foo")
              :length 1000})
```

### Output coercion

```clojure
;; The default output is a string body
(client/get "http://site.com/foo.txt")

;; Coerce as a byte-array
(client/get "http://site.com/favicon.ico" {:as :byte-array})

;; Coerce as something other than UTF-8 string
(client/get "http://site.com/string.txt" {:as "UTF-16"})

;; Try to automatically coerce the output based on the content-type
;; header (this is currently a BETA feature!)
(client/get "http://site.com/foo.bar" {:as :auto})

;; Return the body as a stream
(client/get "http://site.com/bigrequest.html" {:as :stream})
;; Note that the connection to the server will NOT be closed until the
;; stream has been read
```

A more general `request` function is also available, which is useful
as a primitive for building higher-level interfaces:

```clojure
(defn api-action [method path & [opts]]
  (client/request
    (merge {:method method :url (str "http://site.com/" path)} opts)))
```

### Exceptions

The client will throw exceptions on, well, exceptional status
codes.

```clojure
(client/get "http://site.com/broken")
=> Stone Object thrown by throw+: {:status 404, :headers {"server" "nginx/1.0.4",
                                                          "x-runtime" "12ms",
                                                          "content-encoding" "gzip",
                                                          "content-type" "text/html; charset=utf-8",
                                                          "date" "Mon, 17 Oct 2011 23:15 :36 GMT",
                                                          "cache-control" "no-cache",
                                                          "status" "404 Not Found",
                                                          "transfer-encoding" "chunked",
                                                          "connection" "close"},
                                   :body "...body here..."}
   clr-http.lite.client/wrap-exceptions/fn--227 (client.clj:37)

;; You can also ignore exceptions and handle them yourself:
(client/get "http://site.com/broken" {:throw-exceptions false})
;; Or ignore an unknown host (methods return 'nil' if this is set to
;; true and the host does not exist:
(client/get "http://aoeuntahuf89o.com" {:ignore-unknown-host? true})
````
(spacing added by me to be human readable)

## Design

The design of `clr-http` is inspired by the
[Ring](http://github.com/mmcgrana/ring) protocol for Clojure HTTP
server applications.

The client in `clr-http.lite.core` makes HTTP requests according to a given
Ring request map and returns Ring response maps corresponding to the
resulting HTTP response. The function `clr-http.lite.client/request` uses
Ring-style middleware to layer functionality over the core HTTP
request/response implementation. Methods like `clr-http.lite.client/get`
are sugar over this `clr-http.lite.client/request` function.

## Development

To run the tests:

    $ lein clr test

## License

Released under the MIT License:
<http://www.opensource.org/licenses/mit-license.php>
