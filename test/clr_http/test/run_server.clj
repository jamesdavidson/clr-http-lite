(ns clr-http.test.run-server)

(require '[ring.adapter.jetty :as ring])

(defn handler [req]
  ;;(pp/pprint req)
  ;;(println) (println)
  (condp = [(:request-method req) (:uri req)]
    [:get "/get"]
    {:status 200 :body "get"}
    [:head "/head"]
    {:status 200}
    [:get "/content-type"]
    {:status 200 :body (:content-type req)}
    [:get "/header"]
    {:status 200 :body (get-in req [:headers "x-my-header"])}
    [:post "/post"]
    {:status 200 :body (slurp (:body req))}
    [:get "/redirect"] {:status 302 :headers {"Location" "/get"} }
    [:get "/error"]
    {:status 500 :body "o noes"}
    [:get "/timeout"]
    (do
      (Thread/sleep 10)
      {:status 200 :body "timeout"})
    [:delete "/delete-with-body"]
    {:status 200 :body "delete-with-body"}
    [:post "/multipart"]
    {:status 200 :body (:body req)}))


(defn -main [& args]
  (defonce server
    (do
      (future
        (ring/run-jetty handler {:port 18080}))
      (Thread/sleep 1000))))
