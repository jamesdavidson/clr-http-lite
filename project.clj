(defproject clj-http-lite "0.2.2-SNAPSHOT"
  :description "Http client for ClojureCLR based on clj-http-lite."
  :url "https://github.com/whamtet/clr-http-lite/"
  :dependencies [
                  [org.clojure/clojure "1.6.0"]
                  [ring/ring-jetty-adapter "1.3.2"]
                  [ring/ring-devel "1.3.2"]
                 ]
  :plugins [
            [lein-clr "0.2.0"]
            ]
  :clr {
        :cmd-templates  {:clj-exe   [[CLJCLR14_40 %1]]}
        :main-cmd [:clj-exe "Clojure.Main.exe"]
        :compile-cmd [:clj-exe "Clojure.Compile.exe"]
        }
  )
